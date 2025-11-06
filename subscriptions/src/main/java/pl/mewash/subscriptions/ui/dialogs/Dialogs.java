package pl.mewash.subscriptions.ui.dialogs;

import javafx.application.Platform;
import javafx.scene.control.*;

import java.util.Optional;

public class Dialogs {

    public static void showAlertAndAwait(String title, String content, Alert.AlertType alertType) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public static boolean showFullFetchRetryAlert() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Already fetched till oldest content");
        alert.setHeaderText("All channel contents were already fetched till oldest.");
        alert.setContentText("Do you want to repeat full fetch again?");

        ButtonType abortButton = new ButtonType("Abort repeated full fetch", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType retryButton = new ButtonType("Fetch all again", ButtonBar.ButtonData.OTHER);
        alert.getButtonTypes().setAll(abortButton, retryButton);

        DialogPane pane = alert.getDialogPane();
        Button abort = (Button) pane.lookupButton(abortButton);
        Button retry = (Button) pane.lookupButton(retryButton);
        abort.setDefaultButton(true);
        retry.setDefaultButton(false);

        pane.getStyleClass().add("confirm-retry");

        Optional<ButtonType> result = alert.showAndWait();
        return result.orElse(abortButton) == abortButton;
    }
}