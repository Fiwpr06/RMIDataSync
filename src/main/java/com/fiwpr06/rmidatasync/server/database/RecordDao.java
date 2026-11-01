package com.fiwpr06.rmidatasync.server.database;

import com.fiwpr06.rmidatasync.shared.model.RecordItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho bang `records`
 */
public class RecordDao {
    private final RmiDatabaseManager dbManager;

    public RecordDao(RmiDatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<RecordItem> getAll() {
        List<RecordItem> list = new ArrayList<>();
        String sql = "SELECT * FROM records ORDER BY last_modified DESC";
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

    public RecordItem getById(String id) {
        String sql = "SELECT * FROM records WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
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

    public boolean upsert(RecordItem item) {
        String sql = "INSERT INTO records (id, title, category, content, version, last_modified, updated_by, origin_node, is_synced) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE title = VALUES(title), category = VALUES(category), content = VALUES(content), " +
                "version = VALUES(version), last_modified = VALUES(last_modified), updated_by = VALUES(updated_by), " +
                "origin_node = VALUES(origin_node), is_synced = VALUES(is_synced);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getId());
            ps.setString(2, item.getTitle());
            ps.setString(3, item.getCategory() != null ? item.getCategory() : "General");
            ps.setString(4, item.getContent() != null ? item.getContent() : "");
            ps.setInt(5, item.getVersion());
            ps.setLong(6, item.getLastModified());
            ps.setString(7, item.getUpdatedBy() != null ? item.getUpdatedBy() : "System");
            ps.setString(8, item.getOriginNode());
            ps.setBoolean(9, item.isSynced());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(String id) {
        String sql = "DELETE FROM records WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<RecordItem> getUnsynced() {
        List<RecordItem> list = new ArrayList<>();
        String sql = "SELECT * FROM records WHERE is_synced = 0";
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

    public boolean markSynced(String id) {
        String sql = "UPDATE records SET is_synced = 1 WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<RecordItem> getModifiedAfter(long timestamp) {
        List<RecordItem> list = new ArrayList<>();
        String sql = "SELECT * FROM records WHERE last_modified > ? ORDER BY last_modified ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, timestamp);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private RecordItem mapRow(ResultSet rs) throws SQLException {
        return new RecordItem(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("content"),
                rs.getInt("version"),
                rs.getLong("last_modified"),
                rs.getString("updated_by"),
                rs.getString("origin_node"),
                rs.getBoolean("is_synced")
        );
    }
}
