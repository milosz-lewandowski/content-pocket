package pl.mewash.batch.internals.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class BatchAlerts {

    public static boolean getRemoveDuplicatesAlertDecision(int duplicatesCount, int allUrlsCount) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Duplicated URLs Detected");
        alert.setHeaderText(duplicatesCount + " out of " + allUrlsCount + " URLs are duplicates.");
        alert.setContentText("Do you want to skip duplicates processing?");

        ButtonType removeButton = new ButtonType("Skip duplicates");
        ButtonType keepButton = new ButtonType("Process with duplicates", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(removeButton, keepButton);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == removeButton;
    }
}
