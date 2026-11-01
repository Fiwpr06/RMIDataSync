package com.fiwpr06.rmidatasync.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quan ly CSDL MySQL cho tung Node Server trong he thong RMI Data Sync
 */
public class RmiDatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(RmiDatabaseManager.class.getName());

    private final DatabaseConfig config;
    private boolean initialized = false;

    public RmiDatabaseManager(String databaseName) {
        this.config = new DatabaseConfig(databaseName);
    }

    public synchronized boolean initialize() {
        if (initialized) return true;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 1. Tao Database neu chua co
            try (Connection serverConn = DriverManager.getConnection(config.getServerJdbcUrl(), config.getUser(), config.getPassword());
                 Statement stmt = serverConn.createStatement()) {
                String createDbSql = "CREATE DATABASE IF NOT EXISTS `" + config.getDatabaseName() + "` " +
                        "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;";
                stmt.executeUpdate(createDbSql);
                LOGGER.info("[DB] Database '" + config.getDatabaseName() + "' da san sang.");
            }

            // 2. Tao cac bang du lieu
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                // Bang records (Ban ghi du lieu can dong bo)
                String createRecordsTable = "CREATE TABLE IF NOT EXISTS `records` (" +
                        "  `id` VARCHAR(64) PRIMARY KEY," +
                        "  `title` VARCHAR(255) NOT NULL," +
                        "  `category` VARCHAR(100)," +
                        "  `content` TEXT," +
                        "  `version` INT DEFAULT 1," +
                        "  `last_modified` BIGINT NOT NULL," +
                        "  `updated_by` VARCHAR(50)," +
                        "  `origin_node` VARCHAR(20)," +
                        "  `is_synced` TINYINT DEFAULT 1" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
                stmt.executeUpdate(createRecordsTable);

                // Bang users
                String createUsersTable = "CREATE TABLE IF NOT EXISTS `users` (" +
                        "  `username` VARCHAR(50) PRIMARY KEY," +
                        "  `password` VARCHAR(100) NOT NULL," +
                        "  `full_name` VARCHAR(100)," +
                        "  `role` VARCHAR(20)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
                stmt.executeUpdate(createUsersTable);

                // Bang sync_events
                String createEventsTable = "CREATE TABLE IF NOT EXISTS `sync_events` (" +
                        "  `id` INT AUTO_INCREMENT PRIMARY KEY," +
                        "  `event_type` VARCHAR(20) NOT NULL," +
                        "  `record_id` VARCHAR(64) NOT NULL," +
                        "  `record_title` VARCHAR(255)," +
                        "  `source_node` VARCHAR(20)," +
                        "  `target_node` VARCHAR(20)," +
                        "  `timestamp` VARCHAR(30) NOT NULL," +
                        "  `status` VARCHAR(20) NOT NULL" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
                stmt.executeUpdate(createEventsTable);

                // 3. Seed users
                String seedUsers = "INSERT IGNORE INTO `users` (`username`, `password`, `full_name`, `role`) VALUES " +
                        "('admin', 'admin', 'Administrator', 'ADMIN'), " +
                        "('user', '123456', 'Standard User', 'USER');";
                stmt.executeUpdate(seedUsers);

                LOGGER.info("[DB] Khoi tao cac bang thanh cong cho " + config.getDatabaseName());
            }

            initialized = true;
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[DB_ERR] Khoi tao MySQL that bai cho " + config.getDatabaseName() + ": " + e.getMessage(), e);
            return false;
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.getJdbcUrl(), config.getUser(), config.getPassword());
    }

    public DatabaseConfig getConfig() {
        return config;
    }
}
