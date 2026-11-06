package com.fiwpr06.rmidatasync.client.network;

import com.fiwpr06.rmidatasync.shared.model.NodeInfo;
import com.fiwpr06.rmidatasync.shared.model.RecordItem;
import com.fiwpr06.rmidatasync.shared.rmi.IClientDataService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Quan ly ket noi va goi ham RMI tu Client toi Server Node (Node A hoac Node B)
 */
public class RmiClientManager {
    private static final Logger LOGGER = Logger.getLogger(RmiClientManager.class.getName());

    private IClientDataService clientService;
    private String currentHost = "127.0.0.1";
    private int currentPort = 1099;
    private boolean connected = false;
    private String loggedInUser = null;
    private String loggedInPassword = null;

    public synchronized boolean connect(String host, int port) {
        try {
            this.currentHost = host;
            this.currentPort = port;

            // Set RMI timeout to avoid long hangs on unreachable hosts
            System.setProperty("sun.rmi.transport.tcp.responseTimeout", "5000");
            System.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "5000");

            Registry registry = LocateRegistry.getRegistry(host, port);
            this.clientService = (IClientDataService) registry.lookup("ClientDataService");

            // Force a real network call to verify the connection is live
            this.clientService.getNodeInfo();

            this.connected = true;
            LOGGER.info("[CLIENT_NET] Ket noi thanh cong toi Server RMI tai " + host + ":" + port);
            return true;
        } catch (Exception e) {
            LOGGER.severe("[CLIENT_NET_ERR] Ket noi RMI that bai tai " + host + ":" + port + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            this.connected = false;
            this.clientService = null;
            return false;
        }
    }

    public synchronized boolean login(String username, String password) {
        String res = loginDetailed(username, password);
        return "SUCCESS".equalsIgnoreCase(res);
    }

    public synchronized String loginDetailed(String username, String password) {
        if (!connected || clientService == null) return "NOT_CONNECTED";
        try {
            String status = clientService.loginDetailed(username, password);
            if ("SUCCESS".equalsIgnoreCase(status)) {
                this.loggedInUser = username;
                this.loggedInPassword = password;
            }
            return status;
        } catch (Exception e) {
            LOGGER.warning("[CLIENT_NET_ERR] Loi goi loginDetailed(): " + e.getMessage());
            return "ERROR:" + e.getMessage();
        }
    }

    public synchronized void logout() {
        if (connected && clientService != null && loggedInUser != null) {
            try {
                clientService.logout(loggedInUser);
            } catch (Exception ignored) {}
            this.loggedInUser = null;
            this.loggedInPassword = null;
        }
    }

    public synchronized boolean register(String username, String password, String fullName) {
        if (!connected || clientService == null) return false;
        try {
            return clientService.register(username, password, fullName);
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized boolean updatePassword(String newPassword) {
        if (!connected || clientService == null || loggedInUser == null) return false;
        try {
            boolean ok = clientService.updatePassword(loggedInUser, newPassword);
            if (ok) {
                this.loggedInPassword = newPassword;
            }
            return ok;
        } catch (Exception e) {
            LOGGER.severe("[CLIENT_NET_ERR] Loi updatePassword: " + e.getMessage());
            return false;
        }
    }

    public synchronized List<RecordItem> getAllRecords() {
        if (!connected || clientService == null) return new ArrayList<>();
        try {
            return clientService.getAllRecords();
        } catch (Exception e) {
            LOGGER.warning("[CLIENT_NET_ERR] Loi lay danh sach records: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public synchronized boolean createRecord(RecordItem record) {
        if (!connected || clientService == null) return false;
        try {
            if (loggedInUser != null) {
                record.setUpdatedBy(loggedInUser);
            }
            return clientService.createRecord(record);
        } catch (Exception e) {
            LOGGER.warning("[CLIENT_NET_ERR] Loi tao record: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean updateRecord(RecordItem record) {
        if (!connected || clientService == null) return false;
        try {
            if (loggedInUser != null) {
                record.setUpdatedBy(loggedInUser);
            }
            return clientService.updateRecord(record);
        } catch (Exception e) {
            LOGGER.warning("[CLIENT_NET_ERR] Loi sua record: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean deleteRecord(String recordId) {
        if (!connected || clientService == null) return false;
        try {
            return clientService.deleteRecord(recordId);
        } catch (Exception e) {
            LOGGER.warning("[CLIENT_NET_ERR] Loi xoa record: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean adjustStock(String recordId, int delta) {
        if (!connected || clientService == null) return false;
        try {
            return clientService.adjustStock(recordId, delta, loggedInUser != null ? loggedInUser : "User");
        } catch (Exception e) {
            LOGGER.warning("[CLIENT_NET_ERR] Loi chinh ton kho: " + e.getMessage());
            return false;
        }
    }

    public synchronized NodeInfo getNodeInfo() {
        if (!connected || clientService == null) return null;
        try {
            return clientService.getNodeInfo();
        } catch (Exception e) {
            return null;
        }
    }

    public synchronized void disconnect() {
        logout();
        connected = false;
        clientService = null;
        loggedInUser = null;
    }

    public boolean isConnected() { return connected; }
    public String getCurrentHost() { return currentHost; }
    public int getCurrentPort() { return currentPort; }
    public String getLoggedInUser() { return loggedInUser; }
    public String getLoggedInPassword() { return loggedInPassword; }
}
