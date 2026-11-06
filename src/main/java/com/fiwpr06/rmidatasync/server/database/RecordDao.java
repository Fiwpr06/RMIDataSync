package com.fiwpr06.rmidatasync.server.database;

import com.fiwpr06.rmidatasync.shared.model.RecordItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho bang `records` (Quan ly Kho Hang & San pham Phan tan)
 */
public class RecordDao {
    private final RmiDatabaseManager dbManager;

    public RecordDao(RmiDatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public List<RecordItem> getAll() {
        List<RecordItem> list = new ArrayList<>();
        String sql = "SELECT * FROM records WHERE is_deleted = 0 ORDER BY last_modified DESC";
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
        String sql = "INSERT INTO records (id, title, category, price, quantity, status, content, version, last_modified, updated_by, origin_node, is_synced, is_deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE title = VALUES(title), category = VALUES(category), price = VALUES(price), " +
                "quantity = VALUES(quantity), status = VALUES(status), content = VALUES(content), " +
                "version = VALUES(version), last_modified = VALUES(last_modified), updated_by = VALUES(updated_by), " +
                "origin_node = VALUES(origin_node), is_synced = VALUES(is_synced), is_deleted = VALUES(is_deleted);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getId());
            ps.setString(2, item.getTitle());
            ps.setString(3, item.getCategory() != null ? item.getCategory() : "Khác");
            ps.setDouble(4, item.getPrice());
            ps.setInt(5, item.getQuantity());
            ps.setString(6, item.getStatus() != null ? item.getStatus() : "Còn hàng");
            ps.setString(7, item.getContent() != null ? item.getContent() : "");
            ps.setInt(8, item.getVersion());
            ps.setLong(9, item.getLastModified());
            ps.setString(10, item.getUpdatedBy() != null ? item.getUpdatedBy() : "System");
            ps.setString(11, item.getOriginNode());
            ps.setBoolean(12, item.isSynced());
            ps.setBoolean(13, item.isDeleted());
            int rows = ps.executeUpdate();
            return rows >= 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(String id) {
        String sql = "UPDATE records SET is_deleted = 1, is_synced = 0, last_modified = ?, version = version + 1 WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, id);
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
        double price = 0;
        int qty = 0;
        String status = "Còn hàng";
        boolean isDeleted = false;
        try { price = rs.getDouble("price"); } catch (Exception ignored) {}
        try { qty = rs.getInt("quantity"); } catch (Exception ignored) {}
        try { status = rs.getString("status"); } catch (Exception ignored) {}
        try { isDeleted = rs.getBoolean("is_deleted"); } catch (Exception ignored) {}

        return new RecordItem(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("category"),
                price,
                qty,
                status != null ? status : "Còn hàng",
                rs.getString("content"),
                rs.getInt("version"),
                rs.getLong("last_modified"),
                rs.getString("updated_by"),
                rs.getString("origin_node"),
                rs.getBoolean("is_synced"),
                isDeleted
        );
    }
}
