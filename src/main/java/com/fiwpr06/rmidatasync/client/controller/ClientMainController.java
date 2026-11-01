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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Optional;

public class ClientMainController {

    // Connection Bar
    @FXML private TextField txtHost;
    @FXML private TextField txtPort;
    @FXML private Button btnConnect;
    @FXML private Button btnDisconnect;
    @FXML private Label lblNodeStatus;

    // Auth Bar
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblLoggedInUser;

    // Records Table
    @FXML private TextField txtSearch;
    @FXML private TableView<RecordItem> tblRecords;
    @FXML private TableColumn<RecordItem, String> colId;
    @FXML private TableColumn<RecordItem, String> colTitle;
    @FXML private TableColumn<RecordItem, String> colCategory;
    @FXML private TableColumn<RecordItem, Integer> colVersion;
    @FXML private TableColumn<RecordItem, String> colOrigin;
    @FXML private TableColumn<RecordItem, String> colLastModified;

    // Record Editor Panel
    @FXML private TextField txtRecordId;
    @FXML private TextField txtRecordTitle;
    @FXML private TextField txtRecordCategory;
    @FXML private Label lblRecordOrigin;
    @FXML private TextArea txtRecordContent;

    // Bottom Status Bar
    @FXML private Label lblBottomStatus;

    private RmiClientManager clientManager;

    private final ObservableList<RecordItem> masterData = FXCollections.observableArrayList();
    private FilteredList<RecordItem> filteredData;

    @FXML
    public void initialize() {
        this.clientManager = new RmiClientManager();

        setupTable();
        setupSearchFilter();
        setupSelectionListener();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colVersion.setCellValueFactory(new PropertyValueFactory<>("version"));
        colOrigin.setCellValueFactory(new PropertyValueFactory<>("originNode"));
        colLastModified.setCellValueFactory(data -> new SimpleStringProperty(DateTimeUtil.formatEpochMillis(data.getValue().getLastModified())));

        filteredData = new FilteredList<>(masterData, p -> true);
        tblRecords.setItems(filteredData);
    }

    private void setupSearchFilter() {
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(record -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return true;
                }
                String lower = newVal.toLowerCase();
                boolean matchTitle = record.getTitle() != null && record.getTitle().toLowerCase().contains(lower);
                boolean matchCategory = record.getCategory() != null && record.getCategory().toLowerCase().contains(lower);
                boolean matchContent = record.getContent() != null && record.getContent().toLowerCase().contains(lower);
                return matchTitle || matchCategory || matchContent;
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
        lblRecordOrigin.setText(item.getOriginNode() + " (v" + item.getVersion() + ")");
        txtRecordContent.setText(item.getContent());
    }

    // ================= KET NOI & CHUYEN SERVER =================

    @FXML
    public void handleConnect() {
        String host = txtHost.getText().trim();
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException e) {
            AlertHelper.showWarning("Input Error", "Port phai la so nguyen!");
            return;
        }

        connectToServer(host, port);
    }

    private void connectToServer(String host, int port) {
        lblBottomStatus.setText("Connecting to RMI Server " + host + ":" + port + "...");
        new Thread(() -> {
            boolean ok = clientManager.connect(host, port);
            Platform.runLater(() -> {
                if (ok) {
                    btnConnect.setDisable(true);
                    btnDisconnect.setDisable(false);
                    NodeInfo node = clientManager.getNodeInfo();
                    String nodeName = (node != null) ? node.getNodeId() : "Server";
                    lblNodeStatus.setText("Connected: " + nodeName + " (" + host + ":" + port + ")");
                    lblNodeStatus.setStyle("-fx-text-fill: #2EE68A; -fx-font-weight: bold;");
                    lblBottomStatus.setText("Connected to " + nodeName);
                    // Tu dong dang nhap mac dinh neu co
                    handleLogin();
                    handleRefresh();
                } else {
                    btnConnect.setDisable(false);
                    lblNodeStatus.setText("Connection Failed");
                    lblNodeStatus.setStyle("-fx-text-fill: #FF3B56; -fx-font-weight: bold;");
                    lblBottomStatus.setText("Connection failed.");
                    AlertHelper.showError("Connection Failed", "Khong the ket noi toi RMI Server tai " + host + ":" + port);
                }
            });
        }).start();
    }

    @FXML
    public void switchToNodeA() {
        txtHost.setText("127.0.0.1");
        txtPort.setText("1099");
        if (clientManager.isConnected()) {
            clientManager.disconnect();
        }
        connectToServer("127.0.0.1", 1099);
    }

    @FXML
    public void switchToNodeB() {
        txtHost.setText("127.0.0.1");
        txtPort.setText("1100");
        if (clientManager.isConnected()) {
            clientManager.disconnect();
        }
        connectToServer("127.0.0.1", 1100);
    }

    @FXML
    public void handleDisconnect() {
        clientManager.disconnect();
        btnConnect.setDisable(false);
        btnDisconnect.setDisable(true);
        lblNodeStatus.setText("Not Connected");
        lblNodeStatus.setStyle("-fx-text-fill: #FF3B56; -fx-font-weight: bold;");
        lblLoggedInUser.setText("Guest (Read Only)");
        masterData.clear();
        handleClearForm();
        lblBottomStatus.setText("Disconnected from server.");
    }

    // ================= XAC THUC =================

    @FXML
    public void handleLogin() {
        if (!clientManager.isConnected()) {
            AlertHelper.showWarning("Chua ket noi", "Vui long ket noi toi Server RMI truoc khi dang nhap.");
            return;
        }

        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();
        if (user.isEmpty() || pass.isEmpty()) return;

        new Thread(() -> {
            boolean success = clientManager.login(user, pass);
            Platform.runLater(() -> {
                if (success) {
                    lblLoggedInUser.setText("👤 User: " + user);
                    lblLoggedInUser.setStyle("-fx-text-fill: #2EE68A; -fx-font-weight: bold;");
                    lblBottomStatus.setText("Logged in as " + user);
                } else {
                    AlertHelper.showError("Auth Error", "Sai ten dang nhap hoac mat khau tren MySQL database.");
                }
            });
        }).start();
    }

    @FXML
    public void handleRegister() {
        if (!clientManager.isConnected()) {
            AlertHelper.showWarning("Chua ket noi", "Vui long ket noi toi Server RMI truoc khi dang ky.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Register New Account");
        dialog.setHeaderText("Create account in Server MySQL Database");
        dialog.setContentText("Enter username,password,fullname (separated by comma):");
        Optional<String> res = dialog.showAndWait();
        res.ifPresent(input -> {
            String[] parts = input.split(",");
            if (parts.length >= 2) {
                String u = parts[0].trim();
                String p = parts[1].trim();
                String fn = parts.length >= 3 ? parts[2].trim() : u;
                new Thread(() -> {
                    boolean ok = clientManager.register(u, p, fn);
                    Platform.runLater(() -> {
                        if (ok) {
                            AlertHelper.showInfo("Success", "Dang ky tai khoan '" + u + "' thanh cong!");
                            txtUsername.setText(u);
                            txtPassword.setText(p);
                        } else {
                            AlertHelper.showError("Error", "Dang ky that bai.");
                        }
                    });
                }).start();
            }
        });
    }

    // ================= CRUD RECORDS =================

    @FXML
    public void handleRefresh() {
        if (!clientManager.isConnected()) return;
        new Thread(() -> {
            List<RecordItem> items = clientManager.getAllRecords();
            Platform.runLater(() -> {
                masterData.setAll(items);
                lblBottomStatus.setText("Loaded " + items.size() + " records from server.");
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
        lblRecordOrigin.setText("-");
        txtRecordContent.clear();
        tblRecords.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleSaveRecord() {
        if (!clientManager.isConnected()) {
            AlertHelper.showWarning("Chua ket noi", "Vui long ket noi toi Server RMI truoc khi luu du lieu.");
            return;
        }

        String title = txtRecordTitle.getText().trim();
        if (title.isEmpty()) {
            AlertHelper.showWarning("Input Error", "Tieu de ban ghi khong duoc de trong!");
            return;
        }

        String id = txtRecordId.getText().trim();
        String cat = txtRecordCategory.getText().trim();
        String content = txtRecordContent.getText();

        RecordItem item = new RecordItem();
        item.setId(id.isEmpty() ? null : id);
        item.setTitle(title);
        item.setCategory(cat.isEmpty() ? "General" : cat);
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
                    AlertHelper.showInfo("Thanh cong", "Ban ghi da duoc luu tren Server va tu dong kich hoat dong bo RMI!");
                    handleRefresh();
                } else {
                    AlertHelper.showError("Loi", "Khong the luu ban ghi.");
                }
            });
        }).start();
    }

    @FXML
    public void handleDeleteRecord() {
        if (!clientManager.isConnected()) return;
        RecordItem selected = tblRecords.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Thong bao", "Vui long chon mot ban ghi can xoa.");
            return;
        }

        if (AlertHelper.showConfirmation("Xac nhan", "Ban co chac muon xoa ban ghi '" + selected.getTitle() + "'?\nHanh dong nay se dong bo xoa tren ca 2 Server!")) {
            new Thread(() -> {
                boolean ok = clientManager.deleteRecord(selected.getId());
                Platform.runLater(() -> {
                    if (ok) {
                        handleClearForm();
                        handleRefresh();
                        AlertHelper.showInfo("Thanh cong", "Da xoa ban ghi.");
                    } else {
                        AlertHelper.showError("Loi", "Xoa that bai.");
                    }
                });
            }).start();
        }
    }
}
