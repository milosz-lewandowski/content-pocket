package pl.mewash.subscriptions.ui.dialogs;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import pl.mewash.subscriptions.internal.domain.model.ChannelSettings;
import pl.mewash.subscriptions.ui.ChannelSettingsController;

import java.io.IOException;
import java.util.Optional;

public class DialogLauncher {
    public static void showAlertAndAwait(String title, String content, Alert.AlertType alertType) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public static Optional<ChannelSettings> showChannelSettingsDialogAndWait(ChannelSettings currentSettings) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogLauncher.class
                .getResource("/pl/mewash/subscriptions/ui/channel-settings-view.fxml"));

            DialogPane pane = loader.load();
            ChannelSettingsController controller = loader.getController();
            controller.loadSettingsOnUi(currentSettings);

            Dialog<ChannelSettings> dialog = new Dialog<>();
            dialog.setTitle("Edit Channel Settings");
            dialog.setDialogPane(pane);

            dialog.setResultConverter(buttonType -> buttonType != null
                && buttonType.getButtonData() == ButtonBar.ButtonData.APPLY
                    ? controller.getSelectedSettings()
                    : null);

            return dialog.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
