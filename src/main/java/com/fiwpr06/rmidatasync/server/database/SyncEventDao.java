package com.fiwpr06.rmidatasync.server.database;

import com.fiwpr06.rmidatasync.shared.model.SyncAction;
import com.fiwpr06.rmidatasync.shared.model.SyncEvent;
import com.fiwpr06.rmidatasync.shared.util.DateTimeUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho bang `sync_events`
 */
public class SyncEventDao {
    private final RmiDatabaseManager dbManager;

    public SyncEventDao(RmiDatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public boolean logEvent(SyncAction action, String recordId, String title, String source, String target, String status) {
        String sql = "INSERT INTO sync_events (event_type, record_id, record_title, source_node, target_node, timestamp, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action.name());
            ps.setString(2, recordId);
            ps.setString(3, title != null ? title : "-");
            ps.setString(4, source);
            ps.setString(5, target);
            ps.setString(6, DateTimeUtil.now());
            ps.setString(7, status);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<SyncEvent> getRecentEvents(int limit) {
        List<SyncEvent> list = new ArrayList<>();
        String sql = "SELECT * FROM sync_events ORDER BY id DESC LIMIT ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SyncEvent(
                            rs.getInt("id"),
                            SyncAction.valueOf(rs.getString("event_type")),
                            rs.getString("record_id"),
                            rs.getString("record_title"),
                            rs.getString("source_node"),
                            rs.getString("target_node"),
                            rs.getString("timestamp"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
