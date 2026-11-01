package com.fiwpr06.rmidatasync.shared.rmi;

import com.fiwpr06.rmidatasync.shared.model.RecordItem;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Giao dien RMI dong bo du lieu giua 2 Server (Server-to-Server Peer Replication)
 */
public interface IDataSyncService extends Remote {

    /**
     * Nhan ban sao du lieu tu Server doi tac khi co thao tac Them/Sua
     * @param record Ban ghi can dong bo
     * @param originNodeId ID cua server gui den
     */
    boolean replicateRecord(RecordItem record, String originNodeId) throws RemoteException;

    /**
     * Nhan yeu cau xoa ban ghi tu Server doi tac
     * @param recordId ID ban ghi can xoa
     * @param originNodeId ID cua server gui yeu cau
     */
    boolean deleteRecord(String recordId, String originNodeId) throws RemoteException;

    /**
     * Lay danh sach tat ca ban ghi co timestamp sau moc lastSyncTimestamp (phuc vu Reconciliation khi server online lai)
     */
    List<RecordItem> requestFullSync(long lastSyncTimestamp) throws RemoteException;

    /**
     * Kiem tra song con (Heartbeat) giua 2 Server
     */
    boolean ping() throws RemoteException;
}
