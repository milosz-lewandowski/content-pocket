package pl.mewash.batch.internals.utils;

import javafx.scene.control.*;

import java.util.Optional;

public class Dialogs {

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
}
