package com.fiwpr06.rmidatasync.server.controller;

import com.fiwpr06.rmidatasync.server.core.RmiServerManager;
import com.fiwpr06.rmidatasync.shared.model.NodeInfo;
import com.fiwpr06.rmidatasync.shared.model.RecordItem;
import com.fiwpr06.rmidatasync.shared.model.SyncEvent;
import com.fiwpr06.rmidatasync.shared.util.DateTimeUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class ServerDashboardController implements RmiServerManager.ServerDashboardListener {

    @FXML private Label lblNodeTitle;
    @FXML private Label lblLocalInfo;
    @FXML private Label lblPeerInfo;
    @FXML private Label lblPeerBadge;
    @FXML private Button btnForceSync;

    @FXML private Label lblTotalRecords;
    @FXML private Label lblSyncedRecords;
    @FXML private Label lblPendingRecords;

    // Tab 1: Records
    @FXML private TableView<RecordItem> tblRecords;
    @FXML private TableColumn<RecordItem, String> colRecId;
    @FXML private TableColumn<RecordItem, String> colRecTitle;
    @FXML private TableColumn<RecordItem, String> colRecCategory;
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

    private final ObservableList<RecordItem> recordsList = FXCollections.observableArrayList();
    private final ObservableList<SyncEvent> eventsList = FXCollections.observableArrayList();

    public void initManager(RmiServerManager manager) {
        this.serverManager = manager;
        this.serverManager.addListener(this);

        lblNodeTitle.setText("⚡ RMI NODE DASHBOARD (" + manager.getNodeId() + ")");
        lblLocalInfo.setText(manager.getLocalNodeInfo().getHost() + ":" + manager.getLocalPort());
        lblPeerInfo.setText(manager.getPeerHost() + ":" + manager.getPeerPort());

        setupTables();
        refreshRecords();
        refreshEvents();
        updateStats();
    }

    private void setupTables() {
        // Records
        colRecId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRecTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colRecCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colRecVersion.setCellValueFactory(new PropertyValueFactory<>("version"));
        colRecOrigin.setCellValueFactory(new PropertyValueFactory<>("originNode"));
        colRecSynced.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isSynced() ? "YES" : "PENDING"));
        colRecTime.setCellValueFactory(data -> new SimpleStringProperty(DateTimeUtil.formatEpochMillis(data.getValue().getLastModified())));
        tblRecords.setItems(recordsList);

        // Events
        colEvId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEvType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventType().name()));
        colEvRecId.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        colEvTitle.setCellValueFactory(new PropertyValueFactory<>("recordTitle"));
        colEvSource.setCellValueFactory(new PropertyValueFactory<>("sourceNode"));
        colEvTarget.setCellValueFactory(new PropertyValueFactory<>("targetNode"));
        colEvStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEvTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        tblEvents.setItems(eventsList);
    }

    @FXML
    public void refreshRecords() {
        if (serverManager != null && serverManager.getRecordDao() != null) {
            List<RecordItem> items = serverManager.getRecordDao().getAll();
            recordsList.setAll(items);
            updateStats();
        }
    }

    @FXML
    public void refreshEvents() {
        if (serverManager != null && serverManager.getSyncEventDao() != null) {
            List<SyncEvent> events = serverManager.getSyncEventDao().getRecentEvents(50);
            eventsList.setAll(events);
        }
    }

    private void updateStats() {
        int total = recordsList.size();
        long synced = recordsList.stream().filter(RecordItem::isSynced).count();
        long pending = total - synced;

        lblTotalRecords.setText(String.valueOf(total));
        lblSyncedRecords.setText(String.valueOf(synced));
        lblPendingRecords.setText(String.valueOf(pending));
    }

    @FXML
    public void handleForceReconcile() {
        if (serverManager != null) {
            logConsole("[USER_ACTION] Kich hoat dong bo bu toan dien (Force Reconcile)...");
            serverManager.reconcileWithPeer();
        }
    }

    private void logConsole(String line) {
        txtConsole.appendText(line + "\n");
    }

    // ================= IMPLEMENT ServerDashboardListener =================

    @Override
    public void onServerStarted(NodeInfo localNode) {
        Platform.runLater(() -> {
            logConsole("[STATUS] Node " + localNode.getNodeId() + " khoi chay thanh cong tren port " + localNode.getPort());
        });
    }

    @Override
    public void onServerStopped() {
        Platform.runLater(() -> {
            logConsole("[STATUS] Server Node da dung.");
        });
    }

    @Override
    public void onPeerStatusChanged(boolean online, String peerHost, int peerPort) {
        Platform.runLater(() -> {
            if (online) {
                lblPeerBadge.setText("PEER ONLINE (" + peerHost + ":" + peerPort + ")");
                lblPeerBadge.getStyleClass().clear();
                lblPeerBadge.getStyleClass().add("peer-badge-online");
                logConsole("[HEARTBEAT] Peer Server ONLINE: " + peerHost + ":" + peerPort);
            } else {
                lblPeerBadge.setText("PEER OFFLINE");
                lblPeerBadge.getStyleClass().clear();
                lblPeerBadge.getStyleClass().add("peer-badge-offline");
                logConsole("[HEARTBEAT] Peer Server OFFLINE: " + peerHost + ":" + peerPort);
            }
        });
    }

    @Override
    public void onDataChanged() {
        Platform.runLater(() -> {
            refreshRecords();
            refreshEvents();
            logConsole("[RMI_SYNC] Du lieu tren MySQL da duoc cap nhat.");
        });
    }

    @Override
    public void onLog(String message) {
        Platform.runLater(() -> logConsole(message));
    }
}
