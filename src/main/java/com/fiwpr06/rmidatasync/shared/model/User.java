package com.fiwpr06.rmidatasync.shared.model;

import java.io.Serializable;

/**
 * Thuc the Nguoi dung trong he thong RMI Data Sync (Khong ma hoa mat khau de don gian demo)
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password; // Plain-text theo yeu cau de bai
    private String fullName;
    private String role;

    public User() {}

    public User(String username, String password, String fullName, String role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
