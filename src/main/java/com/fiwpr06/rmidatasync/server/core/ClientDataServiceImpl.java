package com.fiwpr06.rmidatasync.server.core;

import com.fiwpr06.rmidatasync.server.database.RecordDao;
import com.fiwpr06.rmidatasync.server.database.SyncEventDao;
import com.fiwpr06.rmidatasync.server.database.UserDao;
import com.fiwpr06.rmidatasync.shared.model.*;
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
        LOGGER.info("[CLIENT_RMI] Yeu cau dang nhap: " + username);
        User user = userDao.authenticate(username, password);
        return user != null;
    }

    @Override
    public boolean register(String username, String password, String fullName) throws RemoteException {
        LOGGER.info("[CLIENT_RMI] Yeu cau dang ky user moi: " + username);
        User newUser = new User(username, password, fullName, "USER");
        return userDao.register(newUser);
    }

    @Override
    public List<RecordItem> getAllRecords() throws RemoteException {
        return recordDao.getAll();
    }

    @Override
    public boolean createRecord(RecordItem record) throws RemoteException {
        if (record.getId() == null || record.getId().trim().isEmpty()) {
            record.setId("rec-" + UUID.randomUUID().toString().substring(0, 8));
        }
        record.setVersion(1);
        record.setLastModified(System.currentTimeMillis());
        record.setOriginNode(localNodeId);
        record.setSynced(false);

        LOGGER.info(String.format("[CLIENT_RMI] Tao moi ban ghi '%s' (ID: %s)", record.getTitle(), record.getId()));
        boolean saved = recordDao.upsert(record);
        if (saved) {
            syncEventDao.logEvent(SyncAction.CREATE, record.getId(), record.getTitle(), localNodeId, localNodeId, "CREATED");
            serverManager.notifyDataChanged();
            // Kich hoat replicate tuc thi sang Peer Server
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

        LOGGER.info(String.format("[CLIENT_RMI] Cap nhat ban ghi '%s' len version %d", record.getTitle(), record.getVersion()));
        boolean updated = recordDao.upsert(record);
        if (updated) {
            syncEventDao.logEvent(SyncAction.UPDATE, record.getId(), record.getTitle(), localNodeId, localNodeId, "UPDATED");
            serverManager.notifyDataChanged();
            // Kich hoat replicate tuc thi sang Peer Server
            serverManager.replicateToPeer(record, SyncAction.UPDATE);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteRecord(String recordId) throws RemoteException {
        RecordItem existing = recordDao.getById(recordId);
        String title = existing != null ? existing.getTitle() : "Unknown";

        LOGGER.info("[CLIENT_RMI] Xoa ban ghi ID: " + recordId);
        boolean deleted = recordDao.delete(recordId);
        if (deleted) {
            syncEventDao.logEvent(SyncAction.DELETE, recordId, title, localNodeId, localNodeId, "DELETED");
            serverManager.notifyDataChanged();
            // Kich hoat delete tren Peer Server
            serverManager.deleteOnPeer(recordId);
            return true;
        }
        return false;
    }

    @Override
    public NodeInfo getNodeInfo() throws RemoteException {
        return serverManager.getLocalNodeInfo();
    }
}
