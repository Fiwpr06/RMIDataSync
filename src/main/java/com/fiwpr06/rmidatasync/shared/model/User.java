package com.fiwpr06.rmidatasync.shared.model;

import java.io.Serializable;

/**
 * Thuc the Nguoi dung trong he thong RMI Data Sync (Khong ma hoa mat khau de don gian demo)
 * Co truong kiem soat phien online phan tan: moi tai khoan chi duoc online tren 1 server tai 1 thoi diem
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password; // Plain-text theo yeu cau de bai
    private String fullName;
    private String role;
    private boolean online;
    private String currentNode;
    private long lastLoginTime;

    public User() {}

    public User(String username, String password, String fullName, String role) {
        this(username, password, fullName, role, false, null, 0);
    }

    public User(String username, String password, String fullName, String role, boolean online, String currentNode, long lastLoginTime) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.online = online;
        this.currentNode = currentNode;
        this.lastLoginTime = lastLoginTime;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public String getCurrentNode() { return currentNode; }
    public void setCurrentNode(String currentNode) { this.currentNode = currentNode; }

    public long getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(long lastLoginTime) { this.lastLoginTime = lastLoginTime; }
}
