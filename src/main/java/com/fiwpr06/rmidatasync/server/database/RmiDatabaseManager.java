package com.fiwpr06.rmidatasync.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quan ly khoi tao va ket noi Co so du lieu MySQL cho Server Node trong RMIDataSync
 */
public class RmiDatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(RmiDatabaseManager.class.getName());

    private final DatabaseConfig config;
    private boolean initialized = false;

    public RmiDatabaseManager(DatabaseConfig config) {
        this.config = config;
    }

    public synchronized boolean initialize() {
        if (initialized) return true;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 1. Tao Database rieng cho Node (rmisync_db_a hoac rmisync_db_b)
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

                // Bang records (Quan ly Kho Hang & San pham Phan tan)
                String createRecordsTable = "CREATE TABLE IF NOT EXISTS `records` (" +
                        "  `id` VARCHAR(64) PRIMARY KEY," +
                        "  `title` VARCHAR(255) NOT NULL," +
                        "  `category` VARCHAR(100)," +
                        "  `price` DOUBLE DEFAULT 0," +
                        "  `quantity` INT DEFAULT 0," +
                        "  `status` VARCHAR(50) DEFAULT 'Còn hàng'," +
                        "  `content` TEXT," +
                        "  `version` INT DEFAULT 1," +
                        "  `last_modified` BIGINT NOT NULL," +
                        "  `updated_by` VARCHAR(50)," +
                        "  `origin_node` VARCHAR(20)," +
                        "  `is_synced` TINYINT DEFAULT 1" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
                stmt.executeUpdate(createRecordsTable);

                // Dam bao co cac cot price, quantity, status neu bang cu da ton tai
                try { stmt.executeUpdate("ALTER TABLE `records` ADD COLUMN `price` DOUBLE DEFAULT 0;"); } catch (Exception ignored) {}
                try { stmt.executeUpdate("ALTER TABLE `records` ADD COLUMN `quantity` INT DEFAULT 0;"); } catch (Exception ignored) {}
                try { stmt.executeUpdate("ALTER TABLE `records` ADD COLUMN `status` VARCHAR(50) DEFAULT 'Còn hàng';"); } catch (Exception ignored) {}

                // Bang users (Bao gom kiem soat phien online phan tan)
                String createUsersTable = "CREATE TABLE IF NOT EXISTS `users` (" +
                        "  `username` VARCHAR(50) PRIMARY KEY," +
                        "  `password` VARCHAR(100) NOT NULL," +
                        "  `full_name` VARCHAR(100)," +
                        "  `role` VARCHAR(20)," +
                        "  `is_online` TINYINT DEFAULT 0," +
                        "  `current_node` VARCHAR(20) DEFAULT NULL," +
                        "  `last_login_time` BIGINT DEFAULT 0" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
                stmt.executeUpdate(createUsersTable);

                try { stmt.executeUpdate("ALTER TABLE `users` ADD COLUMN `is_online` TINYINT DEFAULT 0;"); } catch (Exception ignored) {}
                try { stmt.executeUpdate("ALTER TABLE `users` ADD COLUMN `current_node` VARCHAR(20) DEFAULT NULL;"); } catch (Exception ignored) {}
                try { stmt.executeUpdate("ALTER TABLE `users` ADD COLUMN `last_login_time` BIGINT DEFAULT 0;"); } catch (Exception ignored) {}

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
                String seedUsers = "INSERT IGNORE INTO `users` (`username`, `password`, `full_name`, `role`, `is_online`, `current_node`, `last_login_time`) VALUES " +
                        "('admin', '123', 'Administrator', 'ADMIN', 0, NULL, 0), " +
                        "('user1', '123', 'Nguyễn Văn A', 'USER', 0, NULL, 0), " +
                        "('user2', '123', 'Trần Thị B', 'USER', 0, NULL, 0);";
                stmt.executeUpdate(seedUsers);

                // 4. Seed sample inventory records neu bang dang trong
                String countSql = "SELECT COUNT(*) FROM records";
                try (var rs = stmt.executeQuery(countSql)) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        long now = System.currentTimeMillis();
                        String seedRecords = "INSERT INTO `records` (`id`, `title`, `category`, `price`, `quantity`, `status`, `content`, `version`, `last_modified`, `updated_by`, `origin_node`, `is_synced`) VALUES " +
                                "('SP-101', 'Laptop ASUS ROG Strix G16', 'Máy tính xách tay', 28500000, 25, 'Còn hàng', 'Intel Core i7-13650HX, 16GB RAM, RTX 4060', 1, " + now + ", 'admin', 'SYSTEM', 1), " +
                                "('SP-102', 'Màn hình Dell UltraSharp U2724D', 'Màn hình', 11500000, 18, 'Còn hàng', '27 inch 2K QHD IPS Black 120Hz', 1, " + (now + 10) + ", 'admin', 'SYSTEM', 1), " +
                                "('SP-103', 'Bàn phím cơ FL-Esports OG98', 'Phụ kiện', 2450000, 40, 'Còn hàng', 'Gasket Mount, 3 mode, switch TTC', 1, " + (now + 20) + ", 'admin', 'SYSTEM', 1), " +
                                "('SP-104', 'Router Wi-Fi 6 ASUS RT-AX53U', 'Thiết bị mạng', 1290000, 12, 'Còn hàng', 'Dual Band Gigabit Wi-Fi 6 (802.11ax)', 1, " + (now + 30) + ", 'admin', 'SYSTEM', 1);";
                        stmt.executeUpdate(seedRecords);
                    }
                }
            }

            initialized = true;
            LOGGER.info("[DB] Khoi tao cac bang thanh cong cho " + config.getDatabaseName());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[DB_ERR] Loi khoi tao MySQL database: " + e.getMessage(), e);
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
