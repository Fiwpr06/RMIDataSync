package com.fiwpr06.rmidatasync.server.controller;

import com.fiwpr06.rmidatasync.server.core.RmiServerManager;
import com.fiwpr06.rmidatasync.shared.model.NodeInfo;
import com.fiwpr06.rmidatasync.shared.model.RecordItem;
import com.fiwpr06.rmidatasync.shared.model.SyncEvent;
import com.fiwpr06.rmidatasync.shared.util.AlertHelper;
import com.fiwpr06.rmidatasync.shared.util.DateTimeUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.text.DecimalFormat;
import java.util.List;

public class ServerDashboardController implements RmiServerManager.ServerDashboardListener {

    @FXML private Label lblNodeTitle;
    @FXML private Label lblLocalInfo;
    @FXML private Label lblPeerInfo;
    @FXML private Label lblPeerBadge;
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

    // Tab 3: Console
    @FXML private TextArea txtConsole;

    private RmiServerManager serverManager;
    private final DecimalFormat currencyFormat = new DecimalFormat("#,### VNĐ");

    private final ObservableList<RecordItem> recordsList = FXCollections.observableArrayList();
    private final ObservableList<SyncEvent> eventsList = FXCollections.observableArrayList();

    public void initManager(RmiServerManager manager) {
        this.serverManager = manager;
        this.serverManager.addListener(this);

        lblNodeTitle.setText("⚡ RMI NODE DASHBOARD (" + manager.getNodeId() + ")");
        lblLocalInfo.setText(manager.getLocalNodeInfo().getHost() + ":" + manager.getLocalPort());
        lblPeerInfo.setText(manager.getPeerHost() + ":" + manager.getPeerPort());

        if (txtPeerHost != null) txtPeerHost.setText(manager.getPeerHost());
        if (txtPeerPort != null) txtPeerPort.setText(String.valueOf(manager.getPeerPort()));

        setupTables();
        refreshRecords();
        refreshEvents();
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
        logConsole("[CONFIG] Đã cập nhật máy đối tác: " + host + ":" + port + ". Đang kết nối...");
    }

    @FXML
    public void handleForceReconcile() {
        if (serverManager != null) {
            serverManager.reconcileWithPeer();
            logConsole("[RECONCILE] Người dùng kích hoạt cưỡng chế đồng bộ bù dữ liệu (Force Reconcile)...");
        }
    }

    @FXML
    public void handleRefreshAll() {
        refreshRecords();
        refreshEvents();
        updateStats();
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
                lblPeerBadge.setText("🟢 PEER ONLINE (" + peerHost + ":" + peerPort + ") - ĐANG ĐỒNG BỘ 2 CHIỀU");
                lblPeerBadge.setStyle("-fx-text-fill: #059669; -fx-background-color: #DCFCE7; -fx-padding: 4px 10px; -fx-background-radius: 12px; -fx-font-weight: bold;");
                btnForceSync.setDisable(false);
                logConsole("[HEARTBEAT] Máy chủ đối tác (" + peerHost + ":" + peerPort + ") ĐÃ KẾT NỐI (ONLINE).");
            } else {
                lblPeerBadge.setText("🔴 PEER OFFLINE - ĐANG CHỜ MÁY ĐỐI TÁC");
                lblPeerBadge.setStyle("-fx-text-fill: #DC2626; -fx-background-color: #FEE2E2; -fx-padding: 4px 10px; -fx-background-radius: 12px; -fx-font-weight: bold;");
                btnForceSync.setDisable(true);
                logConsole("[HEARTBEAT] Máy chủ đối tác (" + peerHost + ":" + peerPort + ") MẤT KẾT NỐI (OFFLINE).");
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
