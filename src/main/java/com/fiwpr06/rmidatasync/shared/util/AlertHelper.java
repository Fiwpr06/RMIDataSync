package com.fiwpr06.rmidatasync.shared.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Hop thoai thong bao chuan Cyberpunk Dark cho RMIDataSync
 */
public final class AlertHelper {
    private AlertHelper() {}

    private static void styleDialog(DialogPane pane) {
        if (pane == null) return;
        pane.setStyle(
                "-fx-background-color: #111620; " +
                "-fx-border-color: #1A2233; " +
                "-fx-border-width: 1px; " +
                "-fx-font-family: 'Segoe UI', Arial, sans-serif; " +
                "-fx-font-size: 13px;"
        );
        pane.lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: #E2E8F0;"));
    }

    public static void showInfo(String title, String content) {
        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            styleDialog(alert.getDialogPane());
            alert.showAndWait();
        });
    }

    public static void showWarning(String title, String content) {
        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            styleDialog(alert.getDialogPane());
            alert.showAndWait();
        });
    }

    public static void showError(String title, String content) {
        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            styleDialog(alert.getDialogPane());
            alert.showAndWait();
        });
    }

    public static boolean showConfirmation(String title, String content) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            styleDialog(alert.getDialogPane());
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        } else {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                styleDialog(alert.getDialogPane());
                Optional<ButtonType> result = alert.showAndWait();
                future.complete(result.isPresent() && result.get() == ButtonType.OK);
            });
            try {
                return future.get();
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static void runOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
