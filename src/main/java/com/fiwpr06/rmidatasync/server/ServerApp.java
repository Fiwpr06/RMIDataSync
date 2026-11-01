package com.fiwpr06.rmidatasync.server;

import com.fiwpr06.rmidatasync.server.controller.ServerDashboardController;
import com.fiwpr06.rmidatasync.server.core.RmiServerManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

/**
 * JavaFX Application khoi chay Server Node Dashboard
 */
public class ServerApp extends Application {
    private static String startupNode = "NODE_A";
    private RmiServerManager serverManager;

    public static void setStartupNode(String node) {
        startupNode = node;
    }

    @Override
    public void start(Stage stage) throws Exception {
        List<String> args = getParameters().getRaw();
        String node = (args != null && !args.isEmpty()) ? args.get(0) : startupNode;

        int localPort;
        int peerPort;
        String dbName;

        if ("NODE_B".equalsIgnoreCase(node)) {
            localPort = 1100;
            peerPort = 1099;
            dbName = "rmisync_db_b";
            node = "NODE_B";
        } else {
            localPort = 1099;
            peerPort = 1100;
            dbName = "rmisync_db_a";
            node = "NODE_A";
        }

        serverManager = new RmiServerManager(node, localPort, "127.0.0.1", peerPort, dbName);
        serverManager.start();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/rmidatasync/fxml/server_view.fxml"));
        Parent root = loader.load();
        ServerDashboardController controller = loader.getController();
        controller.initManager(serverManager);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/fiwpr06/rmidatasync/css/server.css").toExternalForm());

        stage.setTitle("⚡ RMI Server Node Dashboard (" + node + " - Port " + localPort + ") - Lab 5");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(580);

        stage.setOnCloseRequest(e -> {
            if (serverManager != null) {
                serverManager.stop();
            }
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
