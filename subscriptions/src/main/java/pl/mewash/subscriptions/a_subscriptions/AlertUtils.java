package pl.mewash.subscriptions.a_subscriptions;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public class AlertUtils {

        public static void showAlertAndAwait(String title, String content, Alert.AlertType alertType) {
            Platform.runLater(() -> {
                Alert alert = new Alert(alertType);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                alert.showAndWait();
            });
        }
}
