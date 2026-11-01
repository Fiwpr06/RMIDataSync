package com.fiwpr06.rmidatasync.shared.rmi;

import com.fiwpr06.rmidatasync.shared.model.RecordItem;
import com.fiwpr06.rmidatasync.shared.model.User;

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

    /**
     * Kiem tra xem User co dang truc tuyen tren Server doi tac khong (Kiem soat phien phan tan)
     */
    boolean isUserLoggedInOnPeer(String username) throws RemoteException;

    /**
     * Thong bao bien dong phien dang nhap/dang xuat cua User sang Server doi tac
     */
    void notifyUserSessionChanged(String username, boolean isOnline, String node) throws RemoteException;

    /**
     * Dong bo tai khoan nguoi dung vua dang ky sang Server doi tac
     */
    boolean replicateUser(User user) throws RemoteException;
}
