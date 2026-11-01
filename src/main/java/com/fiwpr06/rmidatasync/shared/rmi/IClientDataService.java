package com.fiwpr06.rmidatasync.shared.rmi;

import com.fiwpr06.rmidatasync.shared.model.NodeInfo;
import com.fiwpr06.rmidatasync.shared.model.RecordItem;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Giao dien RMI cho phep cac Client (may ao/may tram) thao tac CRUD du lieu tren Server
 */
public interface IClientDataService extends Remote {

    /**
     * Dang nhap tra ve boolean co ban
     */
    boolean login(String username, String password) throws RemoteException;

    /**
     * Dang nhap chi tiet phuc vu kiem soat phien phan tan
     * Tra ve: "SUCCESS", "INVALID_CREDENTIALS", hoac "ALREADY_LOGGED_IN:<NODE_NAME>"
     */
    String loginDetailed(String username, String password) throws RemoteException;

    /**
     * Dang xuat giai phong phien phan tan
     */
    void logout(String username) throws RemoteException;

    /**
     * Dang ky tai khoan moi vao MySQL
     */
    boolean register(String username, String password, String fullName) throws RemoteException;

    /**
     * Lay danh sach tat ca ban ghi san pham
     */
    List<RecordItem> getAllRecords() throws RemoteException;

    /**
     * Tao moi ban ghi san pham
     */
    boolean createRecord(RecordItem record) throws RemoteException;

    /**
     * Cap nhat ban ghi san pham
     */
    boolean updateRecord(RecordItem record) throws RemoteException;

    /**
     * Xoa ban ghi san pham
     */
    boolean deleteRecord(String recordId) throws RemoteException;

    /**
     * Can thiep nhanh so luong ton kho (Nhap kho +10 / Xuat kho -1)
     */
    boolean adjustStock(String recordId, int delta, String updatedBy) throws RemoteException;

    /**
     * Lay danh sach tat ca nguoi dung (phuc vu quan tri)
     */
    List<com.fiwpr06.rmidatasync.shared.model.User> getAllUsers() throws RemoteException;

    /**
     * Lay thong tin Node hien tai
     */
    NodeInfo getNodeInfo() throws RemoteException;
}
