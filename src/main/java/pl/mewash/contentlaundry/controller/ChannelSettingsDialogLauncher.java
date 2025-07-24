package pl.mewash.contentlaundry.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import pl.mewash.contentlaundry.models.channel.ChannelSettings;

import java.io.IOException;
import java.util.Optional;

public class ChannelSettingsDialogLauncher {

    public static Optional<ChannelSettings> showDialogAndWait(ChannelSettings currentSettings) {
        try {
            FXMLLoader loader = new FXMLLoader(ChannelSettingsDialogLauncher.class
                    .getResource("/pl/mewash/contentlaundry/channelSettingsDialog.fxml"));
            DialogPane pane = loader.load();
            ChannelSettingsController controller = loader.getController();
            controller.loadSettingsOnUi(currentSettings);

            Dialog<ChannelSettings> dialog = new Dialog<>();
            dialog.setTitle("Edit Channel Settings");
            dialog.setDialogPane(pane);

            dialog.showAndWait();

            ChannelSettings selected = controller.getSelectedSettings();
            return Optional.ofNullable(selected);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
