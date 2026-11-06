package com.fiwpr06.rmidatasync.server.controller;

import com.fiwpr06.rmidatasync.server.core.RmiServerManager;
import com.fiwpr06.rmidatasync.shared.model.NodeInfo;
import com.fiwpr06.rmidatasync.shared.model.RecordItem;
import com.fiwpr06.rmidatasync.shared.model.SyncEvent;
import com.fiwpr06.rmidatasync.shared.model.User;
import com.fiwpr06.rmidatasync.shared.util.AlertHelper;
import com.fiwpr06.rmidatasync.shared.util.DateTimeUtil;
import com.fiwpr06.rmidatasync.shared.util.NetworkUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ServerDashboardController implements RmiServerManager.ServerDashboardListener {

    @FXML private Label lblNodeTitle;
    @FXML private ComboBox<String> cmbLocalIp;
    @FXML private Label lblLocalPort;
    @FXML private Label lblPeerInfo;
    @FXML private Label lblPeerBadge;
    @FXML private Label lblCopyFeedback;
    @FXML private CheckBox chkAutoAccept;
    @FXML private Button btnForceSync;

    // Peer Configuration inputs
    @FXML private TextField txtPeerHost;
    @FXML private TextField txtPeerPort;
    @FXML private Button btnUpdatePeer;

    @FXML private Label lblTotalRecords;
    @FXML private Label lblSyncedRecords;
    @FXML private Label lblPendingRecords;

    // Tab 1: Records
    @FXML private TableView<RecordItem> tblRecords;
    @FXML private TableColumn<RecordItem, String> colRecId;
    @FXML private TableColumn<RecordItem, String> colRecTitle;
    @FXML private TableColumn<RecordItem, String> colRecCategory;
    @FXML private TableColumn<RecordItem, String> colRecPrice;
    @FXML private TableColumn<RecordItem, Integer> colRecQuantity;
    @FXML private TableColumn<RecordItem, String> colRecStatus;
    @FXML private TableColumn<RecordItem, Integer> colRecVersion;
    @FXML private TableColumn<RecordItem, String> colRecOrigin;
    @FXML private TableColumn<RecordItem, String> colRecSynced;
    @FXML private TableColumn<RecordItem, String> colRecTime;

    // Tab 2: Events
    @FXML private TableView<SyncEvent> tblEvents;
    @FXML private TableColumn<SyncEvent, Integer> colEvId;
    @FXML private TableColumn<SyncEvent, String> colEvType;
    @FXML private TableColumn<SyncEvent, String> colEvRecId;
    @FXML private TableColumn<SyncEvent, String> colEvTitle;
    @FXML private TableColumn<SyncEvent, String> colEvSource;
    @FXML private TableColumn<SyncEvent, String> colEvTarget;
    @FXML private TableColumn<SyncEvent, String> colEvStatus;
    @FXML private TableColumn<SyncEvent, String> colEvTime;

    // Tab 3: Users
    @FXML private TableView<User> tblUsers;
    @FXML private TableColumn<User, String> colUserUsername;
    @FXML private TableColumn<User, String> colUserFullName;
    @FXML private TableColumn<User, String> colUserRole;
    @FXML private TableColumn<User, String> colUserStatus;
    @FXML private TableColumn<User, String> colUserNode;

    // Tab 4: Console
    @FXML private TextArea txtConsole;

    private RmiServerManager serverManager;
    private Timeline feedbackTimeline;
    private final DecimalFormat currencyFormat = new DecimalFormat("#,### VNĐ");

    private final ObservableList<RecordItem> recordsList = FXCollections.observableArrayList();
    private final ObservableList<SyncEvent> eventsList = FXCollections.observableArrayList();
    private final ObservableList<User> usersList = FXCollections.observableArrayList();

    public void initManager(RmiServerManager manager) {
        this.serverManager = manager;
        this.serverManager.addListener(this);
        this.serverManager.setPairingRequestHandler(this::handleIncomingPairing);

        lblNodeTitle.setText("RMI NODE DASHBOARD - " + manager.getNodeId());
        lblLocalPort.setText(":" + manager.getLocalPort());
        lblPeerInfo.setText(manager.getPeerHost() + ":" + manager.getPeerPort());

        // Nap danh sach IP mang de nguoi dung tien chon va cong bo
        List<String> availableIps = NetworkUtil.getAllLocalIpv4Addresses();
        cmbLocalIp.setItems(FXCollections.observableArrayList(availableIps));
        if (availableIps.contains(manager.getLocalHost())) {
            cmbLocalIp.setValue(manager.getLocalHost());
        } else if (!availableIps.isEmpty()) {
            cmbLocalIp.setValue(availableIps.get(0));
        }

        if (txtPeerHost != null) txtPeerHost.setText(manager.getPeerHost());
        if (txtPeerPort != null) txtPeerPort.setText(String.valueOf(manager.getPeerPort()));

        setupTables();
        refreshRecords();
        refreshEvents();
        refreshUsers();
        updateStats();

        onPeerStatusChanged(manager.isPeerConnected(), manager.getPeerHost(), manager.getPeerPort());
    }

    private void setupTables() {
        // Records
        colRecId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRecTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colRecCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        if (colRecPrice != null) {
            colRecPrice.setCellValueFactory(data -> new SimpleStringProperty(currencyFormat.format(data.getValue().getPrice())));
        }
        if (colRecQuantity != null) {
            colRecQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        }
        if (colRecStatus != null) {
            colRecStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        }
        colRecVersion.setCellValueFactory(new PropertyValueFactory<>("version"));
        colRecOrigin.setCellValueFactory(new PropertyValueFactory<>("originNode"));
        colRecSynced.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isSynced() ? "ĐÃ ĐỒNG BỘ" : "CHỜ ĐỒNG BỘ"));
        colRecTime.setCellValueFactory(data -> new SimpleStringProperty(DateTimeUtil.formatEpochMillis(data.getValue().getLastModified())));
        tblRecords.setItems(recordsList);

        // Events
        colEvId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEvType.setCellValueFactory(new PropertyValueFactory<>("eventType"));
        colEvRecId.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        colEvTitle.setCellValueFactory(new PropertyValueFactory<>("recordTitle"));
        colEvSource.setCellValueFactory(new PropertyValueFactory<>("sourceNode"));
        colEvTarget.setCellValueFactory(new PropertyValueFactory<>("targetNode"));
        colEvStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEvTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        tblEvents.setItems(eventsList);

        // Users
        if (tblUsers != null) {
            colUserUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
            colUserFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
            colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
            colUserStatus.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().isOnline() ? "● TRỰC TUYẾN" : "○ NGOẠI TUYẾN"));
            colUserNode.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().isOnline() ? data.getValue().getCurrentNode() : "-"));
            tblUsers.setItems(usersList);
        }
    }

    @FXML
    public void handleSelectLocalIp() {
        if (serverManager == null || cmbLocalIp == null) return;
        String selectedIp = cmbLocalIp.getValue();
        if (selectedIp != null && !selectedIp.trim().isEmpty() && !selectedIp.equals(serverManager.getLocalHost())) {
            serverManager.rebindServices(selectedIp);
            showFeedback("✓ Đã chuyển IP công bố: " + selectedIp);
            logConsole("[CONFIG] Đã chuyển IP công bố RMI sang: " + selectedIp + ":" + serverManager.getLocalPort());
        }
    }

    @FXML
    public void handleCopyLocalAddress() {
        String ip = (cmbLocalIp != null && cmbLocalIp.getValue() != null) ? cmbLocalIp.getValue() : serverManager.getLocalHost();
        int port = (serverManager != null) ? serverManager.getLocalPort() : 1099;
        String addr = ip + ":" + port;

        ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        Clipboard.getSystemClipboard().setContent(content);
        showFeedback("✓ Đã sao chép: " + addr);
    }

    @FXML
    public void handleSetPeerPresetA() {
        if (txtPeerHost != null && (txtPeerHost.getText().trim().isEmpty() || txtPeerHost.getText().trim().equals("127.0.0.1"))) {
            txtPeerHost.setText("127.0.0.1");
        }
        if (txtPeerPort != null) txtPeerPort.setText("1099");
        showFeedback("Đã chọn cấu hình đối tác: Cổng 1099");
    }

    @FXML
    public void handleSetPeerPresetB() {
        if (txtPeerHost != null && (txtPeerHost.getText().trim().isEmpty() || txtPeerHost.getText().trim().equals("127.0.0.1"))) {
            txtPeerHost.setText("127.0.0.1");
        }
        if (txtPeerPort != null) txtPeerPort.setText("1100");
        showFeedback("Đã chọn cấu hình đối tác: Cổng 1100");
    }

    @FXML
    public void handleUpdatePeer() {
        if (txtPeerHost == null || txtPeerPort == null) return;
        String host = txtPeerHost.getText().trim();
        int port;
        try {
            port = Integer.parseInt(txtPeerPort.getText().trim());
        } catch (NumberFormatException e) {
            AlertHelper.showWarning("Lỗi nhập liệu", "Cổng kết nối phải là số nguyên!");
            return;
        }

        if (btnUpdatePeer != null) btnUpdatePeer.setDisable(true);
        showFeedback("Đang gửi yêu cầu ghép đôi tới " + host + ":" + port + "...");
        logConsole("[CONFIG] Đang gửi yêu cầu ghép đôi tới máy đối tác: " + host + ":" + port + "...");

        new Thread(() -> {
            RmiServerManager.PairingResult result = serverManager.connectAndPairWithPeer(host, port);
            Platform.runLater(() -> {
                if (btnUpdatePeer != null) btnUpdatePeer.setDisable(false);
                if (result.isSuccess()) {
                    lblPeerInfo.setText(host + ":" + port);
                    showFeedback("✓ " + result.getMessage());
                    logConsole("[PAIRING] " + result.getMessage());
                    AlertHelper.showInfo("Ghép đôi thành công", result.getMessage());
                } else {
                    showFeedback("Ghép đôi thất bại: " + result.getMessage());
                    logConsole("[PAIRING_ERR] " + result.getMessage());
                    AlertHelper.showWarning("Ghép đôi không thành công", result.getMessage());
                }
            });
        }).start();
    }

    /**
     * Xu ly yeu cau ghep doi tu Server doi tac gui den node nay qua mang LAN.
     * Hien thi hop thoai thong bao xac nhan Accept/Reject hoac tu dong chap nhan.
     */
    private boolean handleIncomingPairing(String originNodeId, String originHost, int originPort, String originDbName) {
        // Truong hop 1: Nguoi dung bat tuy chon "Tu dong chap nhan"
        if (chkAutoAccept != null && chkAutoAccept.isSelected()) {
            Platform.runLater(() -> {
                if (txtPeerHost != null) txtPeerHost.setText(originHost);
                if (txtPeerPort != null) txtPeerPort.setText(String.valueOf(originPort));
                if (lblPeerInfo != null) lblPeerInfo.setText(originHost + ":" + originPort);
                showFeedback("✓ Tự động chấp nhận kết nối từ " + originNodeId);
                logConsole("[PAIRING] Tự động chấp nhận ghép đôi từ " + originNodeId + " (" + originHost + ":" + originPort + ")");
            });
            return true;
        }

        // Truong hop 2: Hien thi hop thoai xac nhan tuong tac cho nguoi dung (Interactive Accept/Reject Dialog)
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Yêu cầu kết nối & ghép đôi máy chủ");
                alert.setHeaderText("Máy chủ đối tác yêu cầu kết nối!");
                alert.setContentText(String.format(
                        "Máy chủ: %s\nĐịa chỉ: %s:%d\nCSDL đối tác: %s\n\nBạn có đồng ý CHẤP NHẬN (Accept) kết nối và đồng bộ dữ liệu hai chiều với máy chủ này không?",
                        originNodeId, originHost, originPort, originDbName
                ));

                ButtonType btnAccept = new ButtonType("Chấp nhận (Accept)", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnReject = new ButtonType("Từ chối (Reject)", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(btnAccept, btnReject);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == btnAccept) {
                    if (txtPeerHost != null) txtPeerHost.setText(originHost);
                    if (txtPeerPort != null) txtPeerPort.setText(String.valueOf(originPort));
                    if (lblPeerInfo != null) lblPeerInfo.setText(originHost + ":" + originPort);
                    showFeedback("✓ Đã chấp nhận kết nối từ " + originNodeId);
                    logConsole("[PAIRING] Đã chấp nhận kết nối ghép đôi từ " + originNodeId + " (" + originHost + ":" + originPort + ")");
                    future.complete(true);
                } else {
                    showFeedback("Đã từ chối kết nối từ " + originNodeId);
                    logConsole("[PAIRING] Đã từ chối kết nối từ " + originNodeId);
                    future.complete(false);
                }
            } catch (Exception e) {
                future.complete(false);
            }
        });

        try {
            // Cho phan hoi tu giao dien toi da 30 giay
            return future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            logConsole("[PAIRING] Yêu cầu kết nối từ " + originNodeId + " hết thời gian chờ (Timeout).");
            return false;
        }
    }

    @FXML
    public void handleForceReconcile() {
        if (serverManager != null) {
            new Thread(() -> {
                boolean alive = serverManager.checkPeerAlive();
                if (alive) {
                    serverManager.reconcileWithPeer();
                    Platform.runLater(() -> {
                        showFeedback("✓ Đang tiến hành đồng bộ bù dữ liệu & tài khoản...");
                        logConsole("[ACTION] Yêu cầu đồng bộ bù dữ liệu và tài khoản với đối tác thành công!");
                    });
                } else {
                    Platform.runLater(() -> {
                        showFeedback("Máy đối tác chưa trực tuyến!");
                        logConsole("[ACTION] Không thể kết nối đối tác tại " + serverManager.getPeerHost() + ":" + serverManager.getPeerPort());
                    });
                }
            }).start();
        }
    }

    @FXML
    public void clearConsole() {
        if (txtConsole != null) {
            txtConsole.clear();
            showFeedback("✓ Đã dọn dẹp nhật ký");
        }
    }

    private void showFeedback(String message) {
        if (lblCopyFeedback != null) {
            lblCopyFeedback.setText(message);
            if (feedbackTimeline != null) {
                feedbackTimeline.stop();
            }
            feedbackTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> lblCopyFeedback.setText("")));
            feedbackTimeline.play();
        }
    }

    @FXML
    public void handleRefreshAll() {
        refreshRecords();
        refreshEvents();
        refreshUsers();
        updateStats();
    }

    @FXML
    public void handleRefreshUsers() {
        refreshUsers();
        showFeedback("✓ Đã làm mới danh sách tài khoản.");
    }

    private void refreshUsers() {
        if (serverManager == null || serverManager.getUserDao() == null) return;
        new Thread(() -> {
            List<User> users = serverManager.getUserDao().getAllUsers();
            Platform.runLater(() -> usersList.setAll(users));
        }).start();
    }

    private void refreshRecords() {
        if (serverManager == null) return;
        new Thread(() -> {
            List<RecordItem> items = serverManager.getRecordDao().getAll();
            Platform.runLater(() -> recordsList.setAll(items));
        }).start();
    }

    private void refreshEvents() {
        if (serverManager == null) return;
        new Thread(() -> {
            List<SyncEvent> events = serverManager.getSyncEventDao().getRecentEvents(100);
            Platform.runLater(() -> eventsList.setAll(events));
        }).start();
    }

    private void updateStats() {
        if (serverManager == null) return;
        new Thread(() -> {
            List<RecordItem> all = serverManager.getRecordDao().getAll();
            int total = all.size();
            long synced = all.stream().filter(RecordItem::isSynced).count();
            long pending = total - synced;

            Platform.runLater(() -> {
                lblTotalRecords.setText(String.valueOf(total));
                lblSyncedRecords.setText(String.valueOf(synced));
                lblPendingRecords.setText(String.valueOf(pending));
            });
        }).start();
    }

    // ================= CALLBACKS TU RMISERVERMANAGER =================

    @Override
    public void onServerStarted(NodeInfo localNode) {
        Platform.runLater(() -> {
            logConsole("[SYSTEM] Node " + localNode.getNodeId() + " da khoi dong tren " + localNode.getHost() + ":" + localNode.getPort());
        });
    }

    @Override
    public void onServerStopped() {
        Platform.runLater(() -> {
            logConsole("[SYSTEM] Server node da dung.");
        });
    }

    @Override
    public void onPeerStatusChanged(boolean online, String peerHost, int peerPort) {
        Platform.runLater(() -> {
            if (online) {
                lblPeerBadge.setText("● Đã kết nối đối tác (" + peerHost + ":" + peerPort + ")");
                lblPeerBadge.setStyle("-fx-text-fill: #15803D; -fx-background-color: #DCFCE7; -fx-padding: 5px 12px; -fx-background-radius: 12px; -fx-font-weight: bold;");
                logConsole("[HEARTBEAT] Máy chủ đối tác (" + peerHost + ":" + peerPort + ") đã kết nối.");
            } else {
                lblPeerBadge.setText("● Đang chờ máy đối tác");
                lblPeerBadge.setStyle("-fx-text-fill: #DC2626; -fx-background-radius: 12px; -fx-background-color: #FEE2E2; -fx-padding: 5px 12px; -fx-font-weight: bold;");
                logConsole("[HEARTBEAT] Máy chủ đối tác (" + peerHost + ":" + peerPort + ") mất kết nối.");
            }
        });
    }

    @Override
    public void onDataChanged() {
        Platform.runLater(this::handleRefreshAll);
    }

    @Override
    public void onLog(String message) {
        Platform.runLater(() -> logConsole(message));
    }

    private void logConsole(String msg) {
        if (txtConsole != null) {
            String time = DateTimeUtil.now();
            txtConsole.appendText("[" + time + "] " + msg + "\n");
        }
    }
}
