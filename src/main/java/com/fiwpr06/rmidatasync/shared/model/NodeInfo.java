package com.fiwpr06.rmidatasync.shared.model;

import java.io.Serializable;

/**
 * Thong tin Node Server trong he thong RMI phan tan
 */
public class NodeInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nodeId;
    private String host;
    private int port;
    private boolean online;

    public NodeInfo() {}

    public NodeInfo(String nodeId, String host, int port, boolean online) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
        this.online = online;
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    @Override
    public String toString() {
        return nodeId + " (" + host + ":" + port + ")";
    }
}
