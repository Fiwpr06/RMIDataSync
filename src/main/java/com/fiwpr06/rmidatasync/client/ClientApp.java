package com.fiwpr06.rmidatasync.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Application khoi chay RMI Data Sync Client
 * Khoi dong voi man hinh Dang nhap / Dang ky va kiem soat phien phan tan
 */
public class ClientApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/rmidatasync/fxml/auth_view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/fiwpr06/rmidatasync/css/client.css").toExternalForm());

        stage.setTitle("⚡ RMIDataSync - Authentication & Server Selection");
        stage.setScene(scene);
        stage.setMinWidth(740);
        stage.setMinHeight(580);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
