package pl.mewash.subscriptions.ui.dialogs;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import pl.mewash.subscriptions.internal.domain.model.ChannelSettings;
import pl.mewash.subscriptions.ui.ChannelSettingsController;

import java.io.IOException;
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

    public static Optional<ChannelSettings> showEditChannelSettingsDialogAndWait(ChannelSettings currentSettings,
                                                                                String channelName) {
        return showChannelSettingsDialogAndWait(currentSettings, channelName, false);
    }

    public static Optional<ChannelSettings> showNewChannelSettingsDialogAndWait(String channelName) {
        return showChannelSettingsDialogAndWait(ChannelSettings.defaultSettings(), channelName, true);
    }

    private static Optional<ChannelSettings> showChannelSettingsDialogAndWait(ChannelSettings currentSettings,
                                                                             String channelName, boolean isNew) {
        try {
            FXMLLoader loader = new FXMLLoader(Dialogs.class
                .getResource("/pl/mewash/subscriptions/ui/channel-settings-view.fxml"));

            DialogPane pane = loader.load();
            ChannelSettingsController controller = loader.getController();
            controller.loadSettingsOnUi(currentSettings);

            Dialog<ChannelSettings> dialog = new Dialog<>();
            String title = isNew
                ? "Setup new channel: " + channelName
                : "Edit channel " + channelName + " settings";
            dialog.setTitle(title);
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
