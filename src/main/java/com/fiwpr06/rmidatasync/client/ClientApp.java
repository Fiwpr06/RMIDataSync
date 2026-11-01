package com.fiwpr06.rmidatasync.client;

import com.fiwpr06.rmidatasync.client.controller.ClientMainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Application khoi chay RMI Data Sync Client
 */
public class ClientApp extends Application {
    private ClientMainController controller;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/rmidatasync/fxml/client_view.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/fiwpr06/rmidatasync/css/client.css").toExternalForm());

        stage.setTitle("⚡ Distributed Data Sync Client (RMI Multi-Master) - Lab 5");
        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(620);

        stage.setOnCloseRequest(e -> {
            if (controller != null) {
                controller.handleDisconnect();
            }
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
