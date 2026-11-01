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

    boolean login(String username, String password) throws RemoteException;

    boolean register(String username, String password, String fullName) throws RemoteException;

    List<RecordItem> getAllRecords() throws RemoteException;

    boolean createRecord(RecordItem record) throws RemoteException;

    boolean updateRecord(RecordItem record) throws RemoteException;

    boolean deleteRecord(String recordId) throws RemoteException;

    NodeInfo getNodeInfo() throws RemoteException;
}
