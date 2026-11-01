# ⚡ RMIDataSync — Distributed Multi-Master Record Synchronization (Lab 5)

Hệ thống đồng bộ hóa dữ liệu phân tán hai chiều (Multi-Master Active-Active Replication) qua **Java RMI** và cơ sở dữ liệu **MySQL**, đáp ứng 100% yêu cầu đồ án Lab 5:
> **Lab 5:** RMI Đồng bộ hóa dữ liệu trên 2 Server. Cách chấm: Server chạy trên 2 máy thực và các Client chạy trên các máy ảo (VMware / VirtualBox).

---

## 🌟 1. Điểm nổi bật & Tính năng chính

1. **Kiến trúc Multi-Master Active-Active (Hai chiều không xung đột):**
   - Cả 2 Server Node (Node A & Node B) đều có thể nhận ghi đọc trực tiếp từ Client.
   - Khi có thay đổi (Create / Update / Delete) tại một Node, Node đó ngay lập tức đẩy biến động qua RMI (`IDataSyncService.replicateRecord()`) sang Node còn lại.
2. **Reconciliation & Heartbeat tự động:**
   - Mỗi Node định kỳ phát Heartbeat kiểm tra sức khỏe của Node đối tác.
   - Khi phát hiện đối tác vừa kết nối lại sau sự cố mạng (Offline -> Online), hệ thống tự động chạy **Reconciliation (Full Sync)** để bù toàn bộ dữ liệu bị trễ theo mốc thời gian `lastSyncTimestamp`.
3. **Optimistic Locking & Last-Write-Wins (LWW):**
   - Mỗi bản ghi có trường `version` và `last_modified`. Nếu nhận được bản ghi có version thấp hơn, server tự động bỏ qua để chống ghi đè dữ liệu cũ.
4. **Hỗ trợ chạy xuyên máy thực & máy ảo (VM):**
   - Tự động cấu hình `java.rmi.server.hostname` bằng IP LAN thực tế của máy chủ thông qua `NetworkUtil.setupRmiHostname()`, khắc phục triệt để lỗi RMI bind `127.0.0.1` khiến máy ảo hoặc máy khác không gọi được stub.
5. **Cơ sở dữ liệu MySQL độc lập:**
   - Node A lưu trữ tại database `rmisync_db_a`.
   - Node B lưu trữ tại database `rmisync_db_b`.
   - Không hash mật khẩu (theo yêu cầu đơn giản hóa để chấm bài).
6. **Giao diện người dùng hiện đại (JavaFX Cyberpunk Dark):**
   - **Server Node Dashboard:** Hiển thị IP/Port lắng nghe, trạng thái Live của Peer Server, bảng lịch sử các sự kiện đồng bộ RMI theo thời gian thực.
   - **Client App:** Nút chuyển đổi nhanh giữa Server A (Port 1099) và Server B (Port 1100), thanh kết nối IP/Port tùy biến cho máy ảo, tìm kiếm tức thì, form nhập liệu trực quan.

---

## 🛠️ 2. Cấu trúc thư mục chuẩn `default-tree`

```text
RMIDataSync/
├── .mvn/wrapper/               # Maven Wrapper tự động
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java
│   │   │   └── com/fiwpr06/rmidatasync/
│   │   │       ├── client/     # Client App, Launcher & Controller
│   │   │       ├── server/     # Server Nodes A/B Launchers & Dashboard Controller
│   │   │       │   ├── core/     # RmiServerManager, DataSyncServiceImpl, ClientDataServiceImpl
│   │   │       │   └── database/ # MySQL Database Manager, RecordDao, UserDao, SyncEventDao
│   │   │       └── shared/     # Model, RMI Interfaces, DateTime & Network Utilities
│   │   └── resources/
│   │       ├── com/fiwpr06/rmidatasync/
│   │       │   ├── css/        # server.css, client.css
│   │       │   └── fxml/       # server_view.fxml, client_view.fxml
│   │       └── db.properties   # Cấu hình kết nối MySQL (host, port, user, password)
│   └── test/
│       └── java/com/fiwpr06/rmidatasync/
│           └── RmiReplicationTest.java # Kiểm thử tự động đồng bộ 2 chiều và Reconcile
├── pom.xml
├── START-SERVER-A.bat / .vbs    # Khởi chạy Node A (Port 1099, rmisync_db_a)
├── START-SERVER-B.bat / .vbs    # Khởi chạy Node B (Port 1100, rmisync_db_b)
├── START-CLIENT.bat / .vbs      # Khởi chạy Client Desktop App
└── README.md
```

---

## 🚀 3. Hướng dẫn triển khai & Khởi chạy

### Bước 1: Chuẩn bị CSDL MySQL
Đảm bảo dịch vụ MySQL đang chạy (mặc định port 3306, user `root`, pass rỗng hoặc chỉnh lại trong `src/main/resources/db.properties`).
Hệ thống sẽ **tự động tạo database** `rmisync_db_a`, `rmisync_db_b` và các bảng dữ liệu khi khởi động, không cần import SQL thủ công!

Tài khoản mặc định:
- Username: `admin` | Password: `123`
- Username: `user1` | Password: `123`

### Bước 2: Chạy trên 1 máy tính (Localhost Test)
- **Chạy Server Node A:** Nhấp đúp vào `START-SERVER-A.bat` (hoặc `mvnw javafx:run -Pserver-a`).
- **Chạy Server Node B:** Nhấp đúp vào `START-SERVER-B.bat` (hoặc `mvnw javafx:run -Pserver-b`).
- **Chạy Client:** Nhấp đúp vào `START-CLIENT.bat` (hoặc `mvnw javafx:run -Pclient`).

### Bước 3: Triển khai 2 máy thực & máy ảo (VMware / VirtualBox)
1. **Máy thực 1 (Node A):**
   - Đặt IP LAN (ví dụ `192.168.1.10`).
   - Mở port 1099 trên Windows Firewall.
   - Chạy `START-SERVER-A.bat`.
2. **Máy thực 2 (Node B):**
   - Đặt IP LAN (ví dụ `192.168.1.20`).
   - Sửa tham số Peer IP thành `192.168.1.10`.
   - Mở port 1100 trên Windows Firewall.
   - Chạy `START-SERVER-B.bat`.
3. **Máy ảo Client (VM Bridged / Host-Only Network):**
   - Chạy `START-CLIENT.bat`.
   - Trên ô Host, nhập IP của Máy thực 1 (`192.168.1.10`) hoặc Máy thực 2 (`192.168.1.20`).
   - Nhấn **Connect** và đăng nhập `admin` / `123`. Mọi thao tác thêm/sửa/xóa đều được phản chiếu tức thì qua cả 2 máy thực!

---

## 🧪 4. Kiểm thử tự động (Unit & Integration Tests)
Chạy lệnh kiểm thử toàn diện:
```bash
.\mvnw test
```
- Kiểm tra kết nối RMI 2 chiều giữa 2 Registry độc lập.
- Kiểm tra tính toàn vẹn của tiến trình tạo mới tại Node A -> tự động nhân bản sang Node B.
- Kiểm tra cập nhật tại Node B -> tự động nhân bản ngược về Node A.
