package com.fiwpr06.rmidatasync.server.core;

import com.fiwpr06.rmidatasync.server.database.RecordDao;
import com.fiwpr06.rmidatasync.server.database.SyncEventDao;
import com.fiwpr06.rmidatasync.server.database.UserDao;
import com.fiwpr06.rmidatasync.shared.model.NodeInfo;
import com.fiwpr06.rmidatasync.shared.model.RecordItem;
import com.fiwpr06.rmidatasync.shared.model.SyncAction;
import com.fiwpr06.rmidatasync.shared.model.User;
import com.fiwpr06.rmidatasync.shared.rmi.IClientDataService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Xu ly yeu cau CRUD va xac thuc tu Client qua RMI
 */
public class ClientDataServiceImpl extends UnicastRemoteObject implements IClientDataService {
    private static final Logger LOGGER = Logger.getLogger(ClientDataServiceImpl.class.getName());

    private final String localNodeId;
    private final RecordDao recordDao;
    private final UserDao userDao;
    private final SyncEventDao syncEventDao;
    private final RmiServerManager serverManager;

    public ClientDataServiceImpl(String localNodeId, RecordDao recordDao, UserDao userDao, SyncEventDao syncEventDao, RmiServerManager serverManager) throws RemoteException {
        super();
        this.localNodeId = localNodeId;
        this.recordDao = recordDao;
        this.userDao = userDao;
        this.syncEventDao = syncEventDao;
        this.serverManager = serverManager;
    }

    @Override
    public boolean login(String username, String password) throws RemoteException {
        String status = loginDetailed(username, password);
        return "SUCCESS".equalsIgnoreCase(status);
    }

    @Override
    public String loginDetailed(String username, String password) throws RemoteException {
        LOGGER.info("[CLIENT_RMI] Yeu cau dang nhap: " + username);
        User user = userDao.authenticate(username, password);
        if (user == null) {
            return "INVALID_CREDENTIALS";
        }

        // 1. Kiem tra xem user co dang active tren chinh node nay khong
        if (user.isOnline() && localNodeId.equalsIgnoreCase(user.getCurrentNode())) {
            LOGGER.warning("[SESSION_DENIED] User '" + username + "' da dang nhap tren " + localNodeId);
            return "ALREADY_LOGGED_IN:" + localNodeId;
        }

        // 2. Kiem tra xem user co dang active tren Peer Node khong
        if (serverManager.isUserLoggedInOnPeer(username)) {
            String peerNode = "NODE_A".equalsIgnoreCase(localNodeId) ? "NODE_B" : "NODE_A";
            LOGGER.warning("[SESSION_DENIED] User '" + username + "' da dang nhap tren Peer " + peerNode);
            return "ALREADY_LOGGED_IN:" + peerNode;
        }

        // 3. Cho phep dang nhap -> Cap nhat trang thai online va thong bao cho peer
        userDao.setSessionState(username, true, localNodeId);
        serverManager.notifyUserSessionChanged(username, true, localNodeId);
        LOGGER.info(String.format("[SESSION_OK] User '%s' dang nhap thanh cong tren %s", username, localNodeId));
        return "SUCCESS";
    }

    @Override
    public void logout(String username) throws RemoteException {
        LOGGER.info("[CLIENT_RMI] Yeu cau dang xuat: " + username);
        userDao.setSessionState(username, false, null);
        serverManager.notifyUserSessionChanged(username, false, null);
    }

    @Override
    public boolean register(String username, String password, String fullName) throws RemoteException {
        LOGGER.info("[CLIENT_RMI] Yeu cau dang ky user moi: " + username);
        User newUser = new User(username, password, fullName, "USER", false, null, 0);
        boolean ok = userDao.register(newUser);
        if (ok) {
            serverManager.replicateUserToPeer(newUser);
        }
        return ok;
    }

    @Override
    public List<RecordItem> getAllRecords() throws RemoteException {
        return recordDao.getAll();
    }

    @Override
    public boolean createRecord(RecordItem record) throws RemoteException {
        if (record.getId() == null || record.getId().trim().isEmpty()) {
            record.setId("SP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        record.setVersion(1);
        record.setLastModified(System.currentTimeMillis());
        record.setOriginNode(localNodeId);
        record.setSynced(false);

        LOGGER.info(String.format("[CLIENT_RMI] Tao moi san pham '%s' (ID: %s)", record.getTitle(), record.getId()));
        boolean saved = recordDao.upsert(record);
        if (saved) {
            syncEventDao.logEvent(SyncAction.CREATE, record.getId(), record.getTitle(), localNodeId, localNodeId, "CREATED");
            serverManager.notifyDataChanged();
            serverManager.replicateToPeer(record, SyncAction.CREATE);
            return true;
        }
        return false;
    }

    @Override
    public boolean updateRecord(RecordItem record) throws RemoteException {
        RecordItem existing = recordDao.getById(record.getId());
        if (existing == null) {
            return false;
        }

        record.setVersion(existing.getVersion() + 1);
        record.setLastModified(System.currentTimeMillis());
        record.setOriginNode(localNodeId);
        record.setSynced(false);

        LOGGER.info(String.format("[CLIENT_RMI] Cap nhat san pham '%s' len version %d", record.getTitle(), record.getVersion()));
        boolean updated = recordDao.upsert(record);
        if (updated) {
            syncEventDao.logEvent(SyncAction.UPDATE, record.getId(), record.getTitle(), localNodeId, localNodeId, "UPDATED");
            serverManager.notifyDataChanged();
            serverManager.replicateToPeer(record, SyncAction.UPDATE);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteRecord(String recordId) throws RemoteException {
        RecordItem existing = recordDao.getById(recordId);
        String title = existing != null ? existing.getTitle() : "Unknown";

        LOGGER.info("[CLIENT_RMI] Xoa san pham ID: " + recordId);
        boolean deleted = recordDao.delete(recordId);
        if (deleted) {
            syncEventDao.logEvent(SyncAction.DELETE, recordId, title, localNodeId, localNodeId, "DELETED");
            serverManager.notifyDataChanged();
            serverManager.deleteOnPeer(recordId);
            return true;
        }
        return false;
    }

    @Override
    public boolean adjustStock(String recordId, int delta, String updatedBy) throws RemoteException {
        RecordItem existing = recordDao.getById(recordId);
        if (existing == null) return false;

        int newQty = Math.max(0, existing.getQuantity() + delta);
        existing.setQuantity(newQty);
        existing.setStatus(newQty > 0 ? "Còn hàng" : "Hết hàng");
        if (updatedBy != null) {
            existing.setUpdatedBy(updatedBy);
        }
        return updateRecord(existing);
    }

    @Override
    public NodeInfo getNodeInfo() throws RemoteException {
        return serverManager.getLocalNodeInfo();
    }
}
