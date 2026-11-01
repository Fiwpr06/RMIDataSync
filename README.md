# ⚡ RMIDataSync — Distributed Multi-Master Inventory Synchronization System

Hệ thống quản lý kho hàng và đồng bộ hóa dữ liệu phân tán hai chiều (**Multi-Master Active-Active Replication**) thông qua **Java RMI** và cơ sở dữ liệu **MySQL**, hỗ trợ kiểm soát phiên phân tán (**Single-Session Concurrency Control**) và giao diện sáng **Light Modern High-Legibility** theo chuẩn Design-Kit.

---

## 🌟 1. Điểm nổi bật & Tính năng chính

1. **Kiểm soát Phiên Phân tán (Single-Session Concurrency Control):**
   - **Quy tắc:** Mỗi tài khoản chỉ được phép tồn tại/đăng nhập trên **1 máy chủ** tại một thời điểm, không thể đăng nhập 2 máy chủ cùng lúc.
   - Khi người dùng đăng nhập tại Node A, Node A ghi nhận trạng thái và thông báo qua RMI cho Node B.
   - Nếu ai đó cố tình dùng cùng tài khoản này đăng nhập vào Node B, hệ thống sẽ **lập tức từ chối và cảnh báo**:
     > ⚠️ *"Tài khoản 'admin' hiện đang trực tuyến tại NODE_A. Vui lòng đăng xuất trước khi đăng nhập máy chủ này!"*
   - Khi người dùng đăng xuất (Logout) hoặc thoát ứng dụng, phiên làm việc được giải phóng tức thời, cho phép tài khoản đăng nhập vào máy chủ khác bình thường.

2. **Nghiệp vụ Thực tế: Quản lý Kho Hàng & Sản phẩm Phân tán:**
   - Mỗi sản phẩm gồm: Mã SP, Tên sản phẩm, Danh mục, Đơn giá (VNĐ), Tồn kho, Trạng thái (Còn hàng/Hết hàng), Cấu hình chi tiết, Phiên bản (`v1`, `v2`, `v3`), Node gốc.
   - **Can thiệp dữ liệu tương tác cao:**
     - `[+ Thêm sản phẩm]`: Thêm mới sản phẩm vào kho.
     - `[💾 Lưu sửa đổi]`: Sửa giá, đổi mô tả, cập nhật thông tin.
     - `[📦 Nhập kho (+10)]`: Tăng nhanh số lượng tồn kho lên 10, tăng version, phản chiếu tức thì sang Server đối tác.
     - `[📤 Xuất kho (-1)]`: Giảm số lượng tồn kho 1, cập nhật trạng thái nếu hết hàng, phản chiếu tức thì.
     - `[🗑️ Xóa sản phẩm]`: Xóa sản phẩm và đồng bộ lệnh xóa sang Server còn lại.
     - `[🔍 Tìm kiếm & Lọc]`: Tìm kiếm tức thì theo mã, tên, danh mục.

3. **Cấu hình & Kết nối 2 Server Rõ ràng (Peer Server Configuration):**
   - Trực tiếp trên giao diện Server Dashboard có ô nhập:
     - `Địa chỉ IP máy đối tác (Peer Host)`: Nhập IP máy thực thứ 2 (ví dụ: `192.168.1.50` hoặc `127.0.0.1`).
     - `Cổng đối tác (Peer Port)`: Mặc định `1100` (hoặc `1099`).
     - Nút **[🔗 Áp dụng & Kết nối lại]** và nút **[🔄 Đồng bộ bù (Reconcile)]**.
   - Đèn LED trạng thái hiển thị rõ:
     - `🟢 PEER ONLINE (192.168.1.50:1100) - ĐANG ĐỒNG BỘ 2 CHIỀU`
     - `🔴 PEER OFFLINE - ĐANG CHỜ MÁY ĐỐI TÁC HOẶC KIỂM TRA LẠI IP`

4. **Giao diện Sáng (Light Modern High-Legibility) chuẩn Design-Kit:**
   - Nền sáng dịu (`#F8FAFC`, `#FFFFFF`), chữ đen sắc nét (`#0F172A`), độ tương phản cao, không gây mỏi mắt.
   - Bảng biểu có độ rộng cột chuẩn xác, tự động co giãn (`TableView.CONSTRAINED_RESIZE_POLICY`), triệt tiêu hoàn toàn lỗi text bị cắt thành `...`.

5. **Reconciliation & Heartbeat Tự động:**
   - Tự động kiểm tra Heartbeat mỗi 3 giây.
   - Khi một Node mất mạng hoặc tắt đột ngột, các bản ghi phát sinh tại Node còn lại sẽ được lưu trữ với cờ `is_synced = 0`.
   - Ngay khi Node kết nối lại, hệ thống tự động bù toàn bộ dữ liệu bị trễ mà không làm mất thông tin.

---

## 🛠️ 2. Cấu trúc Cây Thư mục

```text
RMIDataSync/
├── .mvn/wrapper/               # Maven Wrapper tự động
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java
│   │   │   └── com/fiwpr06/rmidatasync/
│   │   │       ├── client/     # Client App, Launcher & Controllers (AuthViewController, ClientMainController)
│   │   │       ├── server/     # Server Nodes Launchers & Dashboard Controller
│   │   │       │   ├── core/     # RmiServerManager, DataSyncServiceImpl, ClientDataServiceImpl
│   │   │       │   └── database/ # MySQL Database Manager, RecordDao, UserDao, SyncEventDao
│   │   │       └── shared/     # Models (RecordItem, User), RMI Interfaces, Utilities
│   │   └── resources/
│   │       ├── com/fiwpr06/rmidatasync/
│   │       │   ├── css/        # server.css, client.css (Light Modern Theme)
│   │       │   └── fxml/       # auth_view.fxml, client_view.fxml, server_view.fxml
│   │       └── db.properties   # Cấu hình kết nối MySQL
│   └── test/
│       └── java/com/fiwpr06/rmidatasync/
│           └── RmiReplicationTest.java # Kiểm thử tự động đồng bộ 2 chiều, phân tán và single-session
├── pom.xml
├── START-SERVER-A.bat / .vbs    # Khởi chạy Node A (Port 1099, rmisync_db_a)
├── START-SERVER-B.bat / .vbs    # Khởi chạy Node B (Port 1100, rmisync_db_b)
├── START-CLIENT.bat / .vbs      # Khởi chạy Client Desktop App
└── README.md
```

---

## 🚀 3. Hướng dẫn Khởi chạy & Kiểm tra

### Bước 1: Khởi động 2 Server Node
- **Chạy Server Node A:** Nhấp đúp vào `START-SERVER-A.bat` (Cổng 1099, CSDL `rmisync_db_a`).
- **Chạy Server Node B:** Nhấp đúp vào `START-SERVER-B.bat` (Cổng 1100, CSDL `rmisync_db_b`).
- Đèn trạng thái trên 2 Dashboard sẽ chuyển sang màu xanh: `🟢 PEER ONLINE`.

### Bước 2: Khởi động Client & Thử nghiệm Kiểm soát Phiên Phân tán
1. **Chạy Client 1 (`START-CLIENT.bat`):**
   - Chọn **Node A (Port 1099)**.
   - Đăng nhập tài khoản `admin` / `123` -> Đăng nhập thành công!
2. **Chạy Client 2 (`START-CLIENT.bat` thứ 2):**
   - Chọn **Node B (Port 1100)**.
   - Cố tình đăng nhập cùng tài khoản `admin` / `123`.
   - **Kết quả:** Hệ thống lập tức chặn lại và cảnh báo tài khoản `admin` đang online tại `NODE_A`!
3. **Trên Client 1:** Bấm nút **[🚪 Đăng xuất]**.
4. **Trên Client 2:** Bấm lại **[Đăng nhập]** -> Đăng nhập thành công vào Node B!

### Bước 3: Thử nghiệm Đồng bộ Kho Hàng Tức thời
- Trên Client 1 (kết nối Node A): Chọn 1 sản phẩm, bấm **[📦 Nhập kho (+10)]**.
- Trên Client 2 (kết nối Node B): Bấm **[🔄 Làm mới]** -> Số lượng tồn kho và Version nhảy số tức thì theo thời gian thực!
