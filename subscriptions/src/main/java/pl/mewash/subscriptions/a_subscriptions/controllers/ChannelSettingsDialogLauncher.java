package pl.mewash.subscriptions.a_subscriptions.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import pl.mewash.subscriptions.a_subscriptions.models.channel.ChannelSettings;
import pl.mewash.subscriptions.ui.ChannelSettingsController;

import java.io.IOException;
import java.util.Optional;

public class ChannelSettingsDialogLauncher {

    public static Optional<ChannelSettings> showDialogAndWait(ChannelSettings currentSettings) {
        try {
            FXMLLoader loader = new FXMLLoader(ChannelSettingsDialogLauncher.class
                .getResource("/pl/mewash/subscriptions/ui/channel-settings-view.fxml"));
            DialogPane pane = loader.load();
            ChannelSettingsController controller = loader.getController();
            controller.loadSettingsOnUi(currentSettings);

            Dialog<ChannelSettings> dialog = new Dialog<>();
            dialog.setTitle("Edit Channel Settings");
            dialog.setDialogPane(pane);

            dialog.setResultConverter(buttonType ->
                (buttonType != null && buttonType.getButtonData() == ButtonBar.ButtonData.APPLY)
                    ? controller.getSelectedSettings()
                    : null
            );

            return dialog.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
