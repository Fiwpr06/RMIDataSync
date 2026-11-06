package com.fiwpr06.rmidatasync.server.core;

import com.fiwpr06.rmidatasync.server.database.DatabaseConfig;
import com.fiwpr06.rmidatasync.server.database.RecordDao;
import com.fiwpr06.rmidatasync.server.database.RmiDatabaseManager;
import com.fiwpr06.rmidatasync.server.database.SyncEventDao;
import com.fiwpr06.rmidatasync.server.database.UserDao;
import com.fiwpr06.rmidatasync.shared.model.NodeInfo;
import com.fiwpr06.rmidatasync.shared.model.RecordItem;
import com.fiwpr06.rmidatasync.shared.model.SyncAction;
import com.fiwpr06.rmidatasync.shared.model.User;
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
 * Quan ly khoi tao RMI Registry, Binding dich vu, Heartbeat, Pairing Handshake va Replication giua 2 Server
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

    public interface PairingRequestHandler {
        /**
         * Xu ly yeu cau ghep doi tu Server doi tac.
         * @return true neu chap nhan (Accept), false neu tu choi (Reject)
         */
        boolean handlePairingRequest(String originNodeId, String originHost, int originPort, String originDbName);
    }

    public static class PairingResult {
        private final boolean success;
        private final String message;

        public PairingResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    private final String nodeId;
    private final int localPort;
    private volatile String localHost;
    private volatile String peerHost;
    private volatile int peerPort;
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
    private volatile PairingRequestHandler pairingRequestHandler;

    public RmiServerManager(String nodeId, int localPort, String peerHost, int peerPort, String dbName) {
        this.nodeId = nodeId;
        this.localPort = localPort;
        this.peerHost = peerHost != null ? peerHost.trim() : "127.0.0.1";
        this.peerPort = peerPort;
        this.dbName = dbName;
        this.localHost = NetworkUtil.getLocalIpAddress();
    }

    public void addListener(ServerDashboardListener listener) {
        listeners.add(listener);
    }

    public void setPairingRequestHandler(PairingRequestHandler handler) {
        this.pairingRequestHandler = handler;
    }

    public synchronized boolean start() {
        if (running) return true;

        // 1. Khoi tao CSDL MySQL cho Node
        DatabaseConfig config = new DatabaseConfig();
        config.setDatabaseName(dbName);
        this.dbManager = new RmiDatabaseManager(config);
        if (!dbManager.initialize()) {
            LOGGER.severe("[RMI_MGR] Khoi tao MySQL that bai cho " + dbName);
            return false;
        }

        this.recordDao = new RecordDao(dbManager);
        this.userDao = new UserDao(dbManager);
        this.syncEventDao = new SyncEventDao(dbManager);

        // Don dep cac phien online cu khi khoi dong
        this.userDao.clearAllSessions();

        try {
            // 2. Thiet lap hostname cho RMI de ho tro may that & may ao tren mang LAN
            this.localHost = NetworkUtil.getLocalIpAddress();
            NetworkUtil.setupRmiHostname(localHost);

            // 3. Khoi tao Registry
            try {
                localRegistry = LocateRegistry.createRegistry(localPort);
            } catch (Exception e) {
                localRegistry = LocateRegistry.getRegistry(localPort);
            }

            // 4. Bind services
            dataSyncService = new DataSyncServiceImpl(nodeId, recordDao, syncEventDao, userDao, this, localPort);
            clientDataService = new ClientDataServiceImpl(nodeId, recordDao, userDao, syncEventDao, this, localPort);

            localRegistry.rebind("DataSyncService", dataSyncService);
            localRegistry.rebind("ClientDataService", clientDataService);

            running = true;
            asyncTaskPool = Executors.newCachedThreadPool();

            // 5. Khoi chay Heartbeat kiem tra Peer Server dinh ky 3s
            startHeartbeat();

            LOGGER.info(String.format("[RMI_MGR] %s da khoi dong tren %s:%d, MySQL DB: %s",
                    nodeId, localHost, localPort, dbName));
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

    /**
     * Cho phep doi dia chi cong bo (RMI Hostname) khi may chu co nhieu card mang (Wi-Fi, Ethernet).
     */
    public synchronized void rebindServices(String newLocalHost) {
        if (!running || newLocalHost == null || newLocalHost.trim().isEmpty()) return;
        this.localHost = newLocalHost.trim();
        NetworkUtil.setupRmiHostname(this.localHost);

        try {
            if (dataSyncService != null) {
                UnicastRemoteObject.unexportObject(dataSyncService, true);
            }
            if (clientDataService != null) {
                UnicastRemoteObject.unexportObject(clientDataService, true);
            }

            dataSyncService = new DataSyncServiceImpl(nodeId, recordDao, syncEventDao, userDao, this, localPort);
            clientDataService = new ClientDataServiceImpl(nodeId, recordDao, userDao, syncEventDao, this, localPort);

            localRegistry.rebind("DataSyncService", dataSyncService);
            localRegistry.rebind("ClientDataService", clientDataService);

            LOGGER.info("[RMI_MGR] Da rebind services voi IP moi: " + this.localHost);
            NodeInfo info = getLocalNodeInfo();
            for (ServerDashboardListener l : listeners) {
                l.onServerStarted(info);
            }
        } catch (Exception e) {
            LOGGER.warning("[RMI_MGR_ERR] Rebind services that bai: " + e.getMessage());
        }
    }

    /**
     * Gui yeu cau ghep doi (Pairing Handshake) toi may chu doi tac.
     * Neu doi tac dong y (Accept), ca 2 may se tu dong luu dia chi va bat dau dong bo bu.
     */
    public PairingResult connectAndPairWithPeer(String targetHost, int targetPort) {
        if (!running) {
            return new PairingResult(false, "Máy chủ cục bộ chưa khởi động!");
        }

        try {
            LOGGER.info(String.format("[PAIRING] Dang gui yeu cau ghep doi toi %s:%d tu %s (%s:%d)...",
                    targetHost, targetPort, nodeId, localHost, localPort));

            Registry peerReg = LocateRegistry.getRegistry(targetHost, targetPort);
            IDataSyncService peerSync = (IDataSyncService) peerReg.lookup("DataSyncService");

            boolean accepted = peerSync.requestPairing(nodeId, localHost, localPort, dbName);
            if (accepted) {
                this.peerHost = targetHost;
                this.peerPort = targetPort;
                this.peerConnected = true;

                LOGGER.info(String.format("[PAIRING] May chu doi tac %s:%d da CHAP NHAN ket noi!", targetHost, targetPort));
                for (ServerDashboardListener l : listeners) {
                    l.onPeerStatusChanged(true, targetHost, targetPort);
                }

                // Kich hoat dong bo bu ngay lap tuc
                reconcileWithPeer();

                return new PairingResult(true, "Máy chủ đối tác " + targetHost + ":" + targetPort + " đã chấp nhận kết nối! Hệ thống đang đồng bộ dữ liệu hai chiều.");
            } else {
                LOGGER.warning(String.format("[PAIRING] May chu doi tac %s:%d da TU CHOI ket noi.", targetHost, targetPort));
                this.peerConnected = false;
                for (ServerDashboardListener l : listeners) {
                    l.onPeerStatusChanged(false, targetHost, targetPort);
                }
                return new PairingResult(false, "Máy chủ đối tác tại " + targetHost + ":" + targetPort + " đã từ chối yêu cầu ghép đôi.");
            }
        } catch (Exception e) {
            LOGGER.warning("[PAIRING_ERR] Khong the ket noi toi doi tac: " + e.getMessage());
            this.peerConnected = false;
            for (ServerDashboardListener l : listeners) {
                l.onPeerStatusChanged(false, targetHost, targetPort);
            }
            return new PairingResult(false, "Không thể kết nối tới máy chủ đối tác tại " + targetHost + ":" + targetPort
                    + " (" + e.getClass().getSimpleName() + "). Vui lòng kiểm tra địa chỉ IP, cổng và Tường lửa (Firewall)!");
        }
    }

    /**
     * Xu ly yeu cau ghep doi duoc goi tu may chu khac toi node nay.
     */
    public boolean handleIncomingPairing(String originNodeId, String originHost, int originPort, String originDbName) {
        boolean accepted = true;
        if (pairingRequestHandler != null) {
            accepted = pairingRequestHandler.handlePairingRequest(originNodeId, originHost, originPort, originDbName);
        }

        if (accepted) {
            this.peerHost = originHost;
            this.peerPort = originPort;
            this.peerConnected = true;

            LOGGER.info(String.format("[PAIRING_ACCEPTED] Da chap nhan ghep doi tu %s (%s:%d). Thiet lap doi tac thanh cong.",
                    originNodeId, originHost, originPort));

            for (ServerDashboardListener l : listeners) {
                l.onPeerStatusChanged(true, originHost, originPort);
            }

            // Dong bo bu hai chieu ngay khi chap nhan
            if (asyncTaskPool != null) {
                asyncTaskPool.submit(this::reconcileWithPeer);
            }
            return true;
        } else {
            LOGGER.info(String.format("[PAIRING_REJECTED] Da tu choi yeu cau ghep doi tu %s (%s:%d).",
                    originNodeId, originHost, originPort));
            return false;
        }
    }

    public synchronized void updatePeerConfig(String newHost, int newPort) {
        this.peerHost = newHost != null ? newHost.trim() : "127.0.0.1";
        this.peerPort = newPort;
        LOGGER.info("[RMI_MGR] Cap nhat cau hinh Peer Server: " + peerHost + ":" + peerPort);
        if (asyncTaskPool != null) {
            asyncTaskPool.submit(() -> {
                boolean alive = checkPeerAlive();
                peerConnected = alive;
                for (ServerDashboardListener l : listeners) {
                    l.onPeerStatusChanged(peerConnected, peerHost, peerPort);
                }
                if (peerConnected) {
                    reconcileWithPeer();
                }
            });
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

    public boolean checkPeerAlive() {
        try {
            Registry peerReg = LocateRegistry.getRegistry(peerHost, peerPort);
            IDataSyncService peerSync = (IDataSyncService) peerReg.lookup("DataSyncService");
            return peerSync.ping();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isUserLoggedInOnPeer(String username) {
        if (!peerConnected) return false;
        try {
            Registry peerReg = LocateRegistry.getRegistry(peerHost, peerPort);
            IDataSyncService peerSync = (IDataSyncService) peerReg.lookup("DataSyncService");
            return peerSync.isUserLoggedInOnPeer(username);
        } catch (Exception e) {
            return false;
        }
    }

    public void notifyUserSessionChanged(String username, boolean isOnline, String node) {
        if (!peerConnected) return;
        asyncTaskPool.submit(() -> {
            try {
                Registry peerReg = LocateRegistry.getRegistry(peerHost, peerPort);
                IDataSyncService peerSync = (IDataSyncService) peerReg.lookup("DataSyncService");
                peerSync.notifyUserSessionChanged(username, isOnline, node);
            } catch (Exception ignored) {}
        });
    }

    public void replicateUserToPeer(User user) {
        if (!running) return;
        asyncTaskPool.submit(() -> {
            try {
                Registry peerReg = LocateRegistry.getRegistry(peerHost, peerPort);
                IDataSyncService peerSync = (IDataSyncService) peerReg.lookup("DataSyncService");
                boolean ok = peerSync.replicateUser(user);
                if (ok) {
                    syncEventDao.logEvent(SyncAction.REPLICATE, "USER:" + user.getUsername(), user.getFullName(), nodeId, peerHost + ":" + peerPort, "SUCCESS");
                    LOGGER.info(String.format("[REPLICATE_USER] Da dong bo tai khoan '%s' sang Peer Server thanh cong.", user.getUsername()));
                    notifyDataChanged();
                }
            } catch (Exception e) {
                LOGGER.info("[REPLICATE_USER] Peer chua san sang (" + e.getMessage() + "). Tai khoan se duoc tu dong dong bo bu khi Peer ket noi.");
            }
        });
    }

    /**
     * Reconcile: Dong bo bu cac ban ghi va tai khoan nguoi dung khi Peer Server online
     */
    public void reconcileWithPeer() {
        if (asyncTaskPool == null) return;
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

                // 3. Dong bo danh sach tai khoan nguoi dung hai chieu (Bi-directional User Reconciliation)
                LOGGER.info("[RECONCILE] Bat dau dong bo danh sach tai khoan nguoi dung...");
                try {
                    // 3a. Lay tat ca User tu Peer ve Node nay
                    List<User> peerUsers = peerSync.requestFullUserSync();
                    if (peerUsers != null) {
                        for (User pu : peerUsers) {
                            userDao.saveOrUpdate(pu);
                        }
                    }
                    // 3b. Day tat ca User tu Node nay sang Peer
                    List<User> localUsers = userDao.getAllUsers();
                    for (User lu : localUsers) {
                        peerSync.replicateUser(lu);
                    }
                    LOGGER.info(String.format("[RECONCILE] Hoan tat dong bo tai khoan: %d local, %d peer",
                            localUsers.size(), peerUsers != null ? peerUsers.size() : 0));
                } catch (Exception ue) {
                    LOGGER.warning("[RECONCILE_USER_ERR] Loi dong bo tai khoan: " + ue.getMessage());
                }

                LOGGER.info("[RECONCILE] Hoan tat toan bo tien trinh dong bo bu giua 2 Server.");
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
        return new NodeInfo(nodeId, localHost, localPort, running);
    }

    public String getNodeId() { return nodeId; }
    public int getLocalPort() { return localPort; }
    public String getLocalHost() { return localHost; }
    public String getPeerHost() { return peerHost; }
    public int getPeerPort() { return peerPort; }
    public boolean isPeerConnected() { return peerConnected; }
    public RecordDao getRecordDao() { return recordDao; }
    public UserDao getUserDao() { return userDao; }
    public SyncEventDao getSyncEventDao() { return syncEventDao; }
}
