package com.fiwpr06.rmidatasync.client.controller;

import com.fiwpr06.rmidatasync.client.network.RmiClientManager;
import com.fiwpr06.rmidatasync.shared.model.NodeInfo;
import com.fiwpr06.rmidatasync.shared.util.AlertHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller dieu khien man hinh Dang ky / Dang nhap cho RMIDataSync
 * Ho tro chon may chu Node A / Node B linh hoat va thuc thi kiem soat phien phan tan
 */
public class AuthViewController {

    @FXML private TextField txtHost;
    @FXML private TextField txtPort;

    @FXML private TextField txtLoginUser;
    @FXML private PasswordField txtLoginPass;
    @FXML private Button btnLogin;

    @FXML private TextField txtRegUser;
    @FXML private PasswordField txtRegPass;
    @FXML private TextField txtRegFullName;
    @FXML private Button btnRegister;

    @FXML private Label lblStatus;

    private RmiClientManager clientManager;

    @FXML
    public void initialize() {
        this.clientManager = new RmiClientManager();
    }

    @FXML
    public void selectNodeA() {
        txtHost.setText("127.0.0.1");
        txtPort.setText("1099");
        setStatus("Đã chọn máy chủ Node A (Cổng 1099).", false);
    }

    @FXML
    public void selectNodeB() {
        txtHost.setText("127.0.0.1");
        txtPort.setText("1100");
        setStatus("Đã chọn máy chủ Node B (Cổng 1100).", false);
    }

    @FXML
    public void handleLogin() {
        String host = txtHost.getText().trim();
        String portStr = txtPort.getText().trim();
        String user = txtLoginUser.getText().trim();
        String pass = txtLoginPass.getText().trim();

        if (host.isEmpty()) {
            setStatus("Địa chỉ máy chủ không được để trống!", true);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            setStatus("Cổng kết nối không hợp lệ (1 - 65535)!", true);
            return;
        }

        if (user.isEmpty() || pass.isEmpty()) {
            setStatus("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!", true);
            return;
        }

        btnLogin.setDisable(true);
        setStatus("Đang kết nối tới Server RMI " + host + ":" + port + "...", false);

        new Thread(() -> {
            boolean connected = clientManager.connect(host, port);
            if (!connected) {
                Platform.runLater(() -> {
                    btnLogin.setDisable(false);
                    setStatus("Không thể kết nối tới Server RMI tại " + host + ":" + port, true);
                    AlertHelper.showError("Lỗi kết nối", "Không thể kết nối tới Server RMI tại " + host + ":" + port + ".\nVui lòng kiểm tra Server đã bật chưa.");
                });
                return;
            }

            String status = clientManager.loginDetailed(user, pass);
            Platform.runLater(() -> {
                btnLogin.setDisable(false);
                if ("SUCCESS".equalsIgnoreCase(status)) {
                    setStatus("Đăng nhập thành công!", false);
                    openMainDashboard();
                } else if ("INVALID_CREDENTIALS".equalsIgnoreCase(status)) {
                    setStatus("Sai tên đăng nhập hoặc mật khẩu trên MySQL!", true);
                    AlertHelper.showError("Xác thực thất bại", "Sai tên đăng nhập hoặc mật khẩu trên MySQL Database.");
                } else if (status != null && status.startsWith("ALREADY_LOGGED_IN:")) {
                    String activeNode = status.substring("ALREADY_LOGGED_IN:".length());
                    setStatus("Tài khoản đang hoạt động tại " + activeNode + "!", true);
                    AlertHelper.showWarning("Cảnh báo Phân tán",
                            "Tài khoản '" + user + "' hiện đang hoạt động trực tuyến tại " + activeNode + "!\n\n" +
                            "Hệ thống quy định: Mỗi tài khoản chỉ được phép đăng nhập trên 1 máy chủ tại một thời điểm.\n" +
                            "Vui lòng đăng xuất ở máy chủ kia trước khi đăng nhập máy chủ này.");
                } else {
                    setStatus("Lỗi đăng nhập: " + status, true);
                }
            });
        }).start();
    }

    @FXML
    public void handleRegister() {
        String host = txtHost.getText().trim();
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (Exception e) {
            setStatus("Cổng kết nối không hợp lệ!", true);
            return;
        }

        String user = txtRegUser.getText().trim();
        String pass = txtRegPass.getText().trim();
        String fullName = txtRegFullName.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            setStatus("Tên đăng nhập và mật khẩu đăng ký không được để trống!", true);
            return;
        }

        btnRegister.setDisable(true);
        setStatus("Đang gửi yêu cầu đăng ký tới Server...", false);

        new Thread(() -> {
            boolean connected = clientManager.connect(host, port);
            if (!connected) {
                Platform.runLater(() -> {
                    btnRegister.setDisable(false);
                    setStatus("Không thể kết nối tới Server RMI!", true);
                });
                return;
            }

            boolean ok = clientManager.register(user, pass, fullName.isEmpty() ? user : fullName);
            Platform.runLater(() -> {
                btnRegister.setDisable(false);
                if (ok) {
                    AlertHelper.showInfo("Thành công", "Đăng ký tài khoản '" + user + "' thành công và đã đồng bộ sang 2 Server!");
                    txtLoginUser.setText(user);
                    txtLoginPass.setText(pass);
                    setStatus("Đăng ký thành công! Vui lòng chuyển sang Tab Đăng nhập.", false);
                } else {
                    AlertHelper.showError("Thất bại", "Đăng ký thất bại. Tên đăng nhập có thể đã tồn tại.");
                    setStatus("Đăng ký thất bại.", true);
                }
            });
        }).start();
    }

    private void openMainDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/rmidatasync/fxml/client_view.fxml"));
            Parent root = loader.load();

            ClientMainController controller = loader.getController();
            controller.initSession(clientManager);

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            Scene scene = btnLogin.getScene();
            scene.setRoot(root);

            NodeInfo node = clientManager.getNodeInfo();
            String nodeName = (node != null) ? node.getNodeId() : "Server";
            stage.setTitle("⚡ RMIDataSync - " + clientManager.getLoggedInUser() + "@" + nodeName + " (" + clientManager.getCurrentHost() + ":" + clientManager.getCurrentPort() + ")");
            stage.setMinWidth(1100);
            stage.setMinHeight(680);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Lỗi giao diện", "Không thể tải giao diện chính: " + e.getMessage());
        }
    }

    private void setStatus(String message, boolean isError) {
        lblStatus.setText(message);
        if (isError) {
            lblStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
        } else {
            lblStatus.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
        }
    }
}
