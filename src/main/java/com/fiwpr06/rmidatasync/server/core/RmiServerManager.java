package com.fiwpr06.rmidatasync.server.core;

import com.fiwpr06.rmidatasync.server.database.RecordDao;
import com.fiwpr06.rmidatasync.server.database.RmiDatabaseManager;
import com.fiwpr06.rmidatasync.server.database.SyncEventDao;
import com.fiwpr06.rmidatasync.server.database.UserDao;
import com.fiwpr06.rmidatasync.shared.model.NodeInfo;
import com.fiwpr06.rmidatasync.shared.model.RecordItem;
import com.fiwpr06.rmidatasync.shared.model.SyncAction;
import com.fiwpr06.rmidatasync.shared.rmi.IDataSyncService;
import com.fiwpr06.rmidatasync.shared.util.NetworkUtil;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Quan ly khoi tao RMI Registry, Binding dich vu, Heartbeat va Replication giua 2 Server
 */
public class RmiServerManager {
    private static final Logger LOGGER = Logger.getLogger(RmiServerManager.class.getName());

    public interface ServerDashboardListener {
        void onServerStarted(NodeInfo localNode);
        void onServerStopped();
        void onPeerStatusChanged(boolean online, String peerHost, int peerPort);
        void onDataChanged();
        void onLog(String message);
    }

    private final String nodeId;
    private final int localPort;
    private final String peerHost;
    private final int peerPort;
    private final String dbName;

    private RmiDatabaseManager dbManager;
    private RecordDao recordDao;
    private UserDao userDao;
    private SyncEventDao syncEventDao;

    private Registry localRegistry;
    private DataSyncServiceImpl dataSyncService;
    private ClientDataServiceImpl clientDataService;

    private volatile boolean running = false;
    private volatile boolean peerConnected = false;
    private ScheduledExecutorService heartbeatScheduler;
    private ExecutorService asyncTaskPool;

    private final List<ServerDashboardListener> listeners = Collections.synchronizedList(new ArrayList<>());

    public RmiServerManager(String nodeId, int localPort, String peerHost, int peerPort, String dbName) {
        this.nodeId = nodeId;
        this.localPort = localPort;
        this.peerHost = peerHost;
        this.peerPort = peerPort;
        this.dbName = dbName;
    }

    public void addListener(ServerDashboardListener listener) {
        listeners.add(listener);
    }

    public synchronized boolean start() {
        if (running) return true;

        // 1. Khoi tao CSDL MySQL cho Node
        this.dbManager = new RmiDatabaseManager(dbName);
        if (!dbManager.initialize()) {
            LOGGER.severe("[RMI_MGR] Khoi tao MySQL that bai cho " + dbName);
            return false;
        }

        this.recordDao = new RecordDao(dbManager);
        this.userDao = new UserDao(dbManager);
        this.syncEventDao = new SyncEventDao(dbManager);

        try {
            // 2. Thiet lap hostname cho RMI de ho tro may that & may ao
            String localIp = NetworkUtil.getLocalIpAddress();
            NetworkUtil.setupRmiHostname(localIp);

            // 3. Khoi tao Registry
            try {
                localRegistry = LocateRegistry.createRegistry(localPort);
            } catch (Exception e) {
                localRegistry = LocateRegistry.getRegistry(localPort);
            }

            // 4. Bind services
            dataSyncService = new DataSyncServiceImpl(nodeId, recordDao, syncEventDao, this);
            clientDataService = new ClientDataServiceImpl(nodeId, recordDao, userDao, syncEventDao, this);

            localRegistry.rebind("DataSyncService", dataSyncService);
            localRegistry.rebind("ClientDataService", clientDataService);

            running = true;
            asyncTaskPool = Executors.newCachedThreadPool();

            // 5. Khoi chay Heartbeat kiem tra Peer Server dinh ky 3s
            startHeartbeat();

            LOGGER.info(String.format("[RMI_MGR] %s da khoi dong tren port %d, MySQL DB: %s", nodeId, localPort, dbName));
            NodeInfo info = getLocalNodeInfo();
            for (ServerDashboardListener l : listeners) {
                l.onServerStarted(info);
            }
            return true;
        } catch (Exception e) {
            LOGGER.severe("[RMI_MGR_ERR] Khoi dong RMI Server that bai: " + e.getMessage());
            return false;
        }
    }

    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            boolean prevStatus = peerConnected;
            boolean currentStatus = checkPeerAlive();

            if (currentStatus != prevStatus) {
                peerConnected = currentStatus;
                LOGGER.info("[HEARTBEAT] Trang thai Peer Server (" + peerHost + ":" + peerPort + ") thay doi: " + (peerConnected ? "ONLINE" : "OFFLINE"));
                for (ServerDashboardListener l : listeners) {
                    l.onPeerStatusChanged(peerConnected, peerHost, peerPort);
                }

                // Neu Peer Server vua ket noi lai (Offline -> Online) -> Thuc hien Reconcile / Full Sync ngay!
                if (peerConnected) {
                    reconcileWithPeer();
                }
            }
        }, 1, 3, TimeUnit.SECONDS);
    }

    private boolean checkPeerAlive() {
        try {
            Registry peerReg = LocateRegistry.getRegistry(peerHost, peerPort);
            IDataSyncService peerSync = (IDataSyncService) peerReg.lookup("DataSyncService");
            return peerSync.ping();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Reconcile: Dong bo bu cac ban ghi chua dong bo khi Peer Server vua online lai
     */
    public void reconcileWithPeer() {
        asyncTaskPool.submit(() -> {
            try {
                LOGGER.info("[RECONCILE] Bat dau tien trinh dong bo bu du lieu giua 2 Server...");
                Registry peerReg = LocateRegistry.getRegistry(peerHost, peerPort);
                IDataSyncService peerSync = (IDataSyncService) peerReg.lookup("DataSyncService");

                // 1. Day tat ca ban ghi chua dong bo cua minh sang Peer
                List<RecordItem> unsynced = recordDao.getUnsynced();
                for (RecordItem item : unsynced) {
                    boolean ok = peerSync.replicateRecord(item, nodeId);
                    if (ok) {
                        recordDao.markSynced(item.getId());
                        syncEventDao.logEvent(SyncAction.RECONCILE, item.getId(), item.getTitle(), nodeId, peerHost, "PUSHED_OK");
                    }
                }

                // 2. Lay cac ban ghi moi tu Peer ve Node cua minh
                List<RecordItem> peerRecords = peerSync.requestFullSync(0);
                for (RecordItem peerItem : peerRecords) {
                    RecordItem local = recordDao.getById(peerItem.getId());
                    if (local == null || peerItem.getVersion() > local.getVersion()) {
                        peerItem.setSynced(true);
                        recordDao.upsert(peerItem);
                        syncEventDao.logEvent(SyncAction.RECONCILE, peerItem.getId(), peerItem.getTitle(), peerHost, nodeId, "PULLED_OK");
                    }
                }

                LOGGER.info("[RECONCILE] Hoan tat dong bo bu du lieu giua 2 Server.");
                notifyDataChanged();
            } catch (Exception e) {
                LOGGER.warning("[RECONCILE_ERR] Loi dong bo bu: " + e.getMessage());
            }
        });
    }

    /**
     * Dong bo tuc thi sang Peer Server (Instant Replication)
     */
    public void replicateToPeer(RecordItem record, SyncAction action) {
        if (!running) return;
        asyncTaskPool.submit(() -> {
            try {
                Registry peerReg = LocateRegistry.getRegistry(peerHost, peerPort);
                IDataSyncService peerSync = (IDataSyncService) peerReg.lookup("DataSyncService");
                boolean ok = peerSync.replicateRecord(record, nodeId);
                if (ok) {
                    recordDao.markSynced(record.getId());
                    syncEventDao.logEvent(action, record.getId(), record.getTitle(), nodeId, peerHost + ":" + peerPort, "SUCCESS");
                    LOGGER.info(String.format("[REPLICATE_OUT] Da dong bo '%s' sang Peer Server thanh cong.", record.getTitle()));
                } else {
                    syncEventDao.logEvent(action, record.getId(), record.getTitle(), nodeId, peerHost + ":" + peerPort, "FAILED");
                }
            } catch (Exception e) {
                // Peer Server dang offline -> giu cờ is_synced = 0 de Reconcile sau
                LOGGER.warning("[REPLICATE_OUT_ERR] Peer Server offline, ban ghi se duoc luu lai va dong bo sau: " + e.getMessage());
                syncEventDao.logEvent(action, record.getId(), record.getTitle(), nodeId, peerHost + ":" + peerPort, "QUEUED_OFFLINE");
            }
        });
    }

    public void deleteOnPeer(String recordId) {
        if (!running) return;
        asyncTaskPool.submit(() -> {
            try {
                Registry peerReg = LocateRegistry.getRegistry(peerHost, peerPort);
                IDataSyncService peerSync = (IDataSyncService) peerReg.lookup("DataSyncService");
                peerSync.deleteRecord(recordId, nodeId);
                LOGGER.info("[DELETE_OUT] Da dong bo lenh xoa ID " + recordId + " sang Peer Server.");
            } catch (Exception e) {
                LOGGER.warning("[DELETE_OUT_ERR] Khong the gui lenh xoa sang Peer Server (co the offline).");
            }
        });
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;

        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
        if (asyncTaskPool != null) {
            asyncTaskPool.shutdownNow();
        }

        try {
            if (localRegistry != null) {
                localRegistry.unbind("DataSyncService");
                localRegistry.unbind("ClientDataService");
            }
            if (dataSyncService != null) {
                UnicastRemoteObject.unexportObject(dataSyncService, true);
            }
            if (clientDataService != null) {
                UnicastRemoteObject.unexportObject(clientDataService, true);
            }
        } catch (Exception ignored) {}

        for (ServerDashboardListener l : listeners) {
            l.onServerStopped();
        }
        LOGGER.info("[RMI_MGR] " + nodeId + " da dung.");
    }

    public void notifyDataChanged() {
        for (ServerDashboardListener l : listeners) {
            l.onDataChanged();
        }
    }

    public NodeInfo getLocalNodeInfo() {
        return new NodeInfo(nodeId, NetworkUtil.getLocalIpAddress(), localPort, running);
    }

    public String getNodeId() { return nodeId; }
    public int getLocalPort() { return localPort; }
    public String getPeerHost() { return peerHost; }
    public int getPeerPort() { return peerPort; }
    public boolean isPeerConnected() { return peerConnected; }
    public RecordDao getRecordDao() { return recordDao; }
    public UserDao getUserDao() { return userDao; }
    public SyncEventDao getSyncEventDao() { return syncEventDao; }
}
