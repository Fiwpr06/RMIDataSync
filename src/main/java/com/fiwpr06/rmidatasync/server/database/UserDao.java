package com.fiwpr06.rmidatasync.server.database;

import com.fiwpr06.rmidatasync.shared.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho bang `users` trong RMIDataSync
 * Ho tro kiem soat phien truc tuyen phan tan (Single active session per user)
 */
public class UserDao {
    private final RmiDatabaseManager dbManager;

    public UserDao(RmiDatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User getByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean register(User user) {
        String sql = "INSERT INTO users (username, password, full_name, role, is_online, current_node, last_login_time) " +
                "VALUES (?, ?, ?, ?, 0, NULL, 0)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole() != null ? user.getRole() : "USER");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setSessionState(String username, boolean isOnline, String nodeName) {
        String sql = "UPDATE users SET is_online = ?, current_node = ?, last_login_time = ? WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, isOnline);
            ps.setString(2, isOnline ? nodeName : null);
            ps.setLong(3, isOnline ? System.currentTimeMillis() : 0);
            ps.setString(4, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void clearAllSessions() {
        String sql = "UPDATE users SET is_online = 0, current_node = NULL";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username ASC";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        boolean isOnline = false;
        String currentNode = null;
        long lastLogin = 0;
        try { isOnline = rs.getBoolean("is_online"); } catch (Exception ignored) {}
        try { currentNode = rs.getString("current_node"); } catch (Exception ignored) {}
        try { lastLogin = rs.getLong("last_login_time"); } catch (Exception ignored) {}

        return new User(
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("full_name"),
                rs.getString("role"),
                isOnline,
                currentNode,
                lastLogin
        );
    }
}
