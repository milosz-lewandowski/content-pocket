package pl.mewash.batch.internals.utils;

import javafx.application.Platform;
import javafx.scene.control.*;

import java.util.Optional;

public class Dialogs {

    public static void showNoInputAlert() {
        showAlertAndAwait(Alert.AlertType.WARNING,
            "⚠ No input pasted!",
            "You must paste some urls to start laundry.");
    }

    public static void showNoDownloadPathAlert() {
        showAlertAndAwait(Alert.AlertType.WARNING,
            "⚠ No download path selected!",
            "You must specify 'Save to' directory.");
    }

    public static boolean getRemoveDuplicatesAlertDecision(int duplicatesCount, int allUrlsCount) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Duplicated URLs Detected");
        alert.setHeaderText(duplicatesCount + " out of " + allUrlsCount + " URLs are duplicates.");
        alert.setContentText("Do you want to skip duplicates processing?");

        ButtonType removeButton = new ButtonType("Remove duplicates");
        ButtonType keepButton = new ButtonType("Process with duplicates", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(removeButton, keepButton);

        DialogPane pane = alert.getDialogPane();
        Button remove = (Button) pane.lookupButton(removeButton);
        Button keep = (Button) pane.lookupButton(keepButton);
        remove.setDefaultButton(true);
        keep.setDefaultButton(false);

        pane.getStyleClass().add("confirm-retry");

        Optional<ButtonType> result = alert.showAndWait();
        return result.orElse(removeButton) == removeButton;
    }

    private static void showAlertAndAwait(Alert.AlertType alertType, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
