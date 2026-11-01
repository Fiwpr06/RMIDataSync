package com.fiwpr06.rmidatasync.client.controller;

import com.fiwpr06.rmidatasync.client.network.RmiClientManager;
import com.fiwpr06.rmidatasync.shared.model.NodeInfo;
import com.fiwpr06.rmidatasync.shared.model.RecordItem;
import com.fiwpr06.rmidatasync.shared.util.AlertHelper;
import com.fiwpr06.rmidatasync.shared.util.DateTimeUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

public class ClientMainController {

    // Header Info Bar
    @FXML private Label lblCurrentUser;
    @FXML private Label lblCurrentServer;
    @FXML private Label lblNodeStatus;
    @FXML private Button btnLogout;

    // Fast Switch Buttons
    @FXML private Button btnSwitchNodeA;
    @FXML private Button btnSwitchNodeB;

    // Inventory Table
    @FXML private TextField txtSearch;
    @FXML private TableView<RecordItem> tblRecords;
    @FXML private TableColumn<RecordItem, String> colId;
    @FXML private TableColumn<RecordItem, String> colTitle;
    @FXML private TableColumn<RecordItem, String> colCategory;
    @FXML private TableColumn<RecordItem, String> colPrice;
    @FXML private TableColumn<RecordItem, Integer> colQuantity;
    @FXML private TableColumn<RecordItem, String> colStatus;
    @FXML private TableColumn<RecordItem, Integer> colVersion;
    @FXML private TableColumn<RecordItem, String> colOrigin;
    @FXML private TableColumn<RecordItem, String> colLastModified;

    // Inventory Editor Panel
    @FXML private TextField txtRecordId;
    @FXML private TextField txtRecordTitle;
    @FXML private TextField txtRecordCategory;
    @FXML private TextField txtRecordPrice;
    @FXML private TextField txtRecordQuantity;
    @FXML private ComboBox<String> cmbRecordStatus;
    @FXML private Label lblRecordOrigin;
    @FXML private TextArea txtRecordContent;

    // Bottom Status Bar
    @FXML private Label lblBottomStatus;

    private RmiClientManager clientManager;
    private final DecimalFormat currencyFormat = new DecimalFormat("#,### VNĐ");

    private final ObservableList<RecordItem> masterData = FXCollections.observableArrayList();
    private FilteredList<RecordItem> filteredData;

    @FXML
    public void initialize() {
        if (this.clientManager == null) {
            this.clientManager = new RmiClientManager();
        }

        if (cmbRecordStatus != null) {
            cmbRecordStatus.setItems(FXCollections.observableArrayList("Còn hàng", "Sắp hết hàng", "Hết hàng"));
            cmbRecordStatus.setValue("Còn hàng");
        }

        setupTable();
        setupSearchFilter();
        setupSelectionListener();
    }

    public void initSession(RmiClientManager manager) {
        this.clientManager = manager;
        updateHeaderInfo();
        handleRefresh();
    }

    private void updateHeaderInfo() {
        if (clientManager == null) return;
        String user = clientManager.getLoggedInUser();
        lblCurrentUser.setText("Người dùng: " + (user != null ? user : "Guest"));

        NodeInfo node = clientManager.getNodeInfo();
        String nodeName = (node != null) ? node.getNodeId() : "Server";
        lblCurrentServer.setText("Máy chủ: " + nodeName + " (" + clientManager.getCurrentHost() + ":" + clientManager.getCurrentPort() + ")");
        lblNodeStatus.setText("● Đã kết nối");
        lblNodeStatus.setStyle("-fx-text-fill: #15803D; -fx-font-weight: bold; -fx-background-color: #DCFCE7; -fx-padding: 3px 10px; -fx-background-radius: 12px;");
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(data -> new SimpleStringProperty(currencyFormat.format(data.getValue().getPrice())));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colVersion.setCellValueFactory(new PropertyValueFactory<>("version"));
        colOrigin.setCellValueFactory(new PropertyValueFactory<>("originNode"));
        colLastModified.setCellValueFactory(data -> new SimpleStringProperty(DateTimeUtil.formatEpochMillis(data.getValue().getLastModified())));

        filteredData = new FilteredList<>(masterData, p -> true);
        tblRecords.setItems(filteredData);

        tblRecords.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                RecordItem selected = tblRecords.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    populateEditor(selected);
                }
            }
        });
    }

    private void setupSearchFilter() {
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(record -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return true;
                }
                String lower = newVal.toLowerCase();
                boolean matchTitle = record.getTitle() != null && record.getTitle().toLowerCase().contains(lower);
                boolean matchId = record.getId() != null && record.getId().toLowerCase().contains(lower);
                boolean matchCategory = record.getCategory() != null && record.getCategory().toLowerCase().contains(lower);
                boolean matchContent = record.getContent() != null && record.getContent().toLowerCase().contains(lower);
                return matchTitle || matchId || matchCategory || matchContent;
            });
        });
    }

    private void setupSelectionListener() {
        tblRecords.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateEditor(newVal);
            }
        });
    }

    private void populateEditor(RecordItem item) {
        txtRecordId.setText(item.getId());
        txtRecordTitle.setText(item.getTitle());
        txtRecordCategory.setText(item.getCategory());
        txtRecordPrice.setText(String.valueOf((long) item.getPrice()));
        txtRecordQuantity.setText(String.valueOf(item.getQuantity()));
        if (cmbRecordStatus != null) {
            cmbRecordStatus.setValue(item.getStatus() != null ? item.getStatus() : "Còn hàng");
        }
        lblRecordOrigin.setText(item.getOriginNode() + " (v" + item.getVersion() + ")");
        txtRecordContent.setText(item.getContent());
    }

    // ================= CRUD SAN PHAM & CAN THIEP KHO =================

    @FXML
    public void handleRefresh() {
        if (clientManager == null || !clientManager.isConnected()) return;
        new Thread(() -> {
            List<RecordItem> items = clientManager.getAllRecords();
            Platform.runLater(() -> {
                masterData.setAll(items);
                lblBottomStatus.setText("Đã tải " + items.size() + " sản phẩm từ CSDL " + clientManager.getCurrentHost() + ":" + clientManager.getCurrentPort());
            });
        }).start();
    }

    @FXML
    public void handleNewRecord() {
        handleClearForm();
        txtRecordTitle.requestFocus();
    }

    @FXML
    public void handleClearForm() {
        txtRecordId.clear();
        txtRecordTitle.clear();
        txtRecordCategory.clear();
        txtRecordPrice.clear();
        txtRecordQuantity.clear();
        if (cmbRecordStatus != null) cmbRecordStatus.setValue("Còn hàng");
        lblRecordOrigin.setText("-");
        txtRecordContent.clear();
        tblRecords.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleSaveRecord() {
        if (!clientManager.isConnected()) {
            AlertHelper.showWarning("Chưa kết nối", "Vui lòng kết nối tới Server RMI trước khi lưu dữ liệu.");
            return;
        }

        String title = txtRecordTitle.getText().trim();
        if (title.isEmpty()) {
            AlertHelper.showWarning("Lỗi nhập liệu", "Tên sản phẩm không được để trống!");
            return;
        }

        String id = txtRecordId.getText().trim();
        String cat = txtRecordCategory.getText().trim();
        String content = txtRecordContent.getText();

        double price = 0;
        try {
            if (!txtRecordPrice.getText().trim().isEmpty()) {
                price = Double.parseDouble(txtRecordPrice.getText().trim().replaceAll("[^0-9.]", ""));
            }
        } catch (NumberFormatException e) {
            AlertHelper.showWarning("Lỗi nhập liệu", "Đơn giá phải là số hợp lệ!");
            return;
        }

        int qty = 0;
        try {
            if (!txtRecordQuantity.getText().trim().isEmpty()) {
                qty = Integer.parseInt(txtRecordQuantity.getText().trim());
            }
        } catch (NumberFormatException e) {
            AlertHelper.showWarning("Lỗi nhập liệu", "Số lượng tồn kho phải là số nguyên!");
            return;
        }

        String status = cmbRecordStatus != null ? cmbRecordStatus.getValue() : "Còn hàng";

        RecordItem item = new RecordItem();
        item.setId(id.isEmpty() ? null : id);
        item.setTitle(title);
        item.setCategory(cat.isEmpty() ? "Khác" : cat);
        item.setPrice(price);
        item.setQuantity(qty);
        item.setStatus(status);
        item.setContent(content);

        new Thread(() -> {
            boolean ok;
            if (id.isEmpty()) {
                ok = clientManager.createRecord(item);
            } else {
                ok = clientManager.updateRecord(item);
            }

            Platform.runLater(() -> {
                if (ok) {
                    AlertHelper.showInfo("Thành công", "Đã lưu sản phẩm vào kho và kích hoạt đồng bộ RMI tức thời sang Server đối tác!");
                    handleRefresh();
                } else {
                    AlertHelper.showError("Lỗi", "Không thể lưu sản phẩm.");
                }
            });
        }).start();
    }

    @FXML
    public void handleDeleteRecord() {
        if (!clientManager.isConnected()) return;
        RecordItem selected = tblRecords.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Thông báo", "Vui lòng chọn một sản phẩm cần xóa.");
            return;
        }

        if (AlertHelper.showConfirmation("Xác nhận xóa", "Bạn có chắc muốn xóa sản phẩm '" + selected.getTitle() + "'?\nHành động này sẽ xóa dữ liệu trên cả 2 Server!")) {
            new Thread(() -> {
                boolean ok = clientManager.deleteRecord(selected.getId());
                Platform.runLater(() -> {
                    if (ok) {
                        handleClearForm();
                        handleRefresh();
                        AlertHelper.showInfo("Thành công", "Đã xóa sản phẩm khỏi hệ thống phân tán.");
                    } else {
                        AlertHelper.showError("Lỗi", "Xóa sản phẩm thất bại.");
                    }
                });
            }).start();
        }
    }

    // ================= CAN THIEP NHANH SO LUONG TON KHO =================

    @FXML
    public void handleStockPlus10() {
        RecordItem selected = tblRecords.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Thông báo", "Vui lòng chọn 1 sản phẩm trong bảng để nhập thêm kho (+10).");
            return;
        }

        new Thread(() -> {
            boolean ok = clientManager.adjustStock(selected.getId(), 10);
            Platform.runLater(() -> {
                if (ok) {
                    lblBottomStatus.setText("Đã nhập thêm +10 vào sản phẩm " + selected.getTitle());
                    handleRefresh();
                }
            });
        }).start();
    }

    @FXML
    public void handleStockMinus1() {
        RecordItem selected = tblRecords.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Thông báo", "Vui lòng chọn 1 sản phẩm trong bảng để xuất kho (-1).");
            return;
        }

        if (selected.getQuantity() <= 0) {
            AlertHelper.showWarning("Cảnh báo", "Sản phẩm này đã hết hàng trong kho!");
            return;
        }

        new Thread(() -> {
            boolean ok = clientManager.adjustStock(selected.getId(), -1);
            Platform.runLater(() -> {
                if (ok) {
                    lblBottomStatus.setText("Đã xuất kho -1 sản phẩm " + selected.getTitle());
                    handleRefresh();
                }
            });
        }).start();
    }

    // ================= CHUYEN MAY CHU & DANG XUAT =================

    @FXML
    public void switchToNodeA() {
        switchServer("127.0.0.1", 1099, "NODE_A");
    }

    @FXML
    public void switchToNodeB() {
        switchServer("127.0.0.1", 1100, "NODE_B");
    }

    private void switchServer(String host, int port, String targetNode) {
        String user = clientManager.getLoggedInUser();
        String pass = clientManager.getLoggedInPassword();
        lblBottomStatus.setText("Đang chuyển kết nối sang " + targetNode + " (" + host + ":" + port + ")...");
        new Thread(() -> {
            // Giai phong phien tai server cu truoc khi sang server moi
            clientManager.logout();
            boolean ok = clientManager.connect(host, port);
            if (ok && user != null && pass != null) {
                // Dang nhap vao server moi voi mat khau thuc cua user
                clientManager.loginDetailed(user, pass);
            }
            Platform.runLater(() -> {
                if (ok) {
                    updateHeaderInfo();
                    handleRefresh();
                    AlertHelper.showInfo("Chuyển Server Thành công", "Đã chuyển sang kết nối với " + targetNode + ".\nDữ liệu hiển thị đồng bộ 100%!");
                } else {
                    AlertHelper.showError("Lỗi kết nối", "Không thể kết nối sang " + targetNode + " tại " + host + ":" + port);
                }
            });
        }).start();
    }

    @FXML
    public void handleLogout() {
        if (clientManager != null) {
            clientManager.disconnect();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/rmidatasync/fxml/auth_view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("RMIDataSync - Đăng nhập và chọn máy chủ");
            stage.setWidth(740);
            stage.setHeight(580);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleDisconnect() {
        if (clientManager != null) {
            clientManager.disconnect();
        }
    }
}
