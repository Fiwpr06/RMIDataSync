package com.fiwpr06.rmidatasync.server.controller;

import com.fiwpr06.rmidatasync.server.core.RmiServerManager;
import com.fiwpr06.rmidatasync.shared.model.NodeInfo;
import com.fiwpr06.rmidatasync.shared.model.RecordItem;
import com.fiwpr06.rmidatasync.shared.model.SyncEvent;
import com.fiwpr06.rmidatasync.shared.model.User;
import com.fiwpr06.rmidatasync.shared.util.AlertHelper;
import com.fiwpr06.rmidatasync.shared.util.DateTimeUtil;
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

public class ServerDashboardController implements RmiServerManager.ServerDashboardListener {

    @FXML private Label lblNodeTitle;
    @FXML private Label lblLocalInfo;
    @FXML private Label lblPeerInfo;
    @FXML private Label lblPeerBadge;
    @FXML private Label lblCopyFeedback;
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

        lblNodeTitle.setText("RMI NODE DASHBOARD - " + manager.getNodeId());
        lblLocalInfo.setText(manager.getLocalNodeInfo().getHost() + ":" + manager.getLocalPort());
        lblPeerInfo.setText(manager.getPeerHost() + ":" + manager.getPeerPort());

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
    public void handleCopyLocalAddress() {
        String addr = lblLocalInfo.getText();
        if (addr != null && !addr.isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(addr);
            Clipboard.getSystemClipboard().setContent(content);
            showFeedback("✓ Đã sao chép địa chỉ máy chủ: " + addr);
        }
    }

    @FXML
    public void handleSetPeerPresetA() {
        if (txtPeerHost != null) txtPeerHost.setText("127.0.0.1");
        if (txtPeerPort != null) txtPeerPort.setText("1099");
        showFeedback("Đã chọn cấu hình đối tác: Cổng 1099");
    }

    @FXML
    public void handleSetPeerPresetB() {
        if (txtPeerHost != null) txtPeerHost.setText("127.0.0.1");
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

        serverManager.updatePeerConfig(host, port);
        lblPeerInfo.setText(host + ":" + port);
        showFeedback("✓ Đã kết nối lại đối tác: " + host + ":" + port);
        logConsole("[CONFIG] Đã cập nhật máy đối tác: " + host + ":" + port + ". Đang kết nối...");
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
            logConsole("[SYSTEM] Node " + localNode.getNodeId() + " da khoi dong tren port " + localNode.getPort());
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
                lblPeerBadge.setStyle("-fx-text-fill: #DC2626; -fx-background-color: #FEE2E2; -fx-padding: 5px 12px; -fx-background-radius: 12px; -fx-font-weight: bold;");
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
