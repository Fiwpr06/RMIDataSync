package com.fiwpr06.rmidatasync.server.core;

import com.fiwpr06.rmidatasync.server.database.RecordDao;
import com.fiwpr06.rmidatasync.server.database.SyncEventDao;
import com.fiwpr06.rmidatasync.server.database.UserDao;
import com.fiwpr06.rmidatasync.shared.model.RecordItem;
import com.fiwpr06.rmidatasync.shared.model.SyncAction;
import com.fiwpr06.rmidatasync.shared.model.User;
import com.fiwpr06.rmidatasync.shared.rmi.IDataSyncService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.logging.Logger;

/**
 * Xu ly yeu cau dong bo RMI tu Server doi tac (Peer-to-Peer Replication)
 */
public class DataSyncServiceImpl extends UnicastRemoteObject implements IDataSyncService {
    private static final Logger LOGGER = Logger.getLogger(DataSyncServiceImpl.class.getName());

    private final String localNodeId;
    private final RecordDao recordDao;
    private final SyncEventDao syncEventDao;
    private final UserDao userDao;
    private final RmiServerManager serverManager;

    public DataSyncServiceImpl(String localNodeId, RecordDao recordDao, SyncEventDao syncEventDao, UserDao userDao, RmiServerManager serverManager) throws RemoteException {
        super();
        this.localNodeId = localNodeId;
        this.recordDao = recordDao;
        this.syncEventDao = syncEventDao;
        this.userDao = userDao;
        this.serverManager = serverManager;
    }

    @Override
    public boolean replicateRecord(RecordItem record, String originNodeId) throws RemoteException {
        LOGGER.info(String.format("[REPLICATION_IN] Nhan san pham '%s' (ID: %s, v%d) tu %s",
                record.getTitle(), record.getId(), record.getVersion(), originNodeId));

        record.setSynced(true);
        boolean saved = recordDao.upsert(record);
        if (saved) {
            syncEventDao.logEvent(SyncAction.REPLICATE, record.getId(), record.getTitle(), originNodeId, localNodeId, "SUCCESS");
            serverManager.notifyDataChanged();
            return true;
        } else {
            syncEventDao.logEvent(SyncAction.REPLICATE, record.getId(), record.getTitle(), originNodeId, localNodeId, "FAILED");
            return false;
        }
    }

    @Override
    public boolean deleteRecord(String recordId, String originNodeId) throws RemoteException {
        LOGGER.info(String.format("[REPLICATION_IN] Nhan yeu cau xoa san pham ID '%s' tu %s", recordId, originNodeId));

        boolean deleted = recordDao.delete(recordId);
        syncEventDao.logEvent(SyncAction.DELETE, recordId, "Deleted Record", originNodeId, localNodeId, deleted ? "SUCCESS" : "FAILED");
        if (deleted) {
            serverManager.notifyDataChanged();
        }
        return deleted;
    }

    @Override
    public List<RecordItem> requestFullSync(long lastSyncTimestamp) throws RemoteException {
        LOGGER.info("[RECONCILIATION] Peer Server yeu cau Full Sync tu moc: " + lastSyncTimestamp);
        return recordDao.getModifiedAfter(lastSyncTimestamp);
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    @Override
    public boolean isUserLoggedInOnPeer(String username) throws RemoteException {
        User user = userDao.getByUsername(username);
        boolean online = user != null && user.isOnline() && localNodeId.equalsIgnoreCase(user.getCurrentNode());
        LOGGER.info(String.format("[SESSION_CHECK] Peer hoi kiem tra User '%s': %s tren %s", username, online ? "ONLINE" : "OFFLINE", localNodeId));
        return online;
    }

    @Override
    public void notifyUserSessionChanged(String username, boolean isOnline, String node) throws RemoteException {
        LOGGER.info(String.format("[SESSION_NOTIFY] Nhan thong bao User '%s' %s tren %s", username, isOnline ? "ONLINE" : "OFFLINE", node));
        userDao.setSessionState(username, isOnline, node);
    }

    @Override
    public boolean replicateUser(User user) throws RemoteException {
        LOGGER.info(String.format("[REPLICATION_USER] Nhan tai khoan moi '%s' tu peer", user.getUsername()));
        return userDao.register(user);
    }
}
