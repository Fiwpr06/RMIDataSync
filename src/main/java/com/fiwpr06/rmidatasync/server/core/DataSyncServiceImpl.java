package com.fiwpr06.rmidatasync.server.core;

import com.fiwpr06.rmidatasync.server.database.RecordDao;
import com.fiwpr06.rmidatasync.server.database.SyncEventDao;
import com.fiwpr06.rmidatasync.shared.model.RecordItem;
import com.fiwpr06.rmidatasync.shared.model.SyncAction;
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
    private final RmiServerManager serverManager;

    public DataSyncServiceImpl(String localNodeId, RecordDao recordDao, SyncEventDao syncEventDao, RmiServerManager serverManager) throws RemoteException {
        super();
        this.localNodeId = localNodeId;
        this.recordDao = recordDao;
        this.syncEventDao = syncEventDao;
        this.serverManager = serverManager;
    }

    @Override
    public boolean replicateRecord(RecordItem record, String originNodeId) throws RemoteException {
        LOGGER.info(String.format("[REPLICATION_IN] Nhan ban ghi '%s' (ID: %s, v%d) tu %s",
                record.getTitle(), record.getId(), record.getVersion(), originNodeId));

        // Dam bao ban ghi ghi nhan da duoc dong bo
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
        LOGGER.info(String.format("[REPLICATION_IN] Nhan yeu cau xoa ban ghi ID '%s' tu %s", recordId, originNodeId));

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
}
