package pl.mewash.subscriptions.ui.dialogs;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.subscriptions.internal.domain.model.ChannelSettings;
import pl.mewash.subscriptions.ui.ChannelSettingsController;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ChannelSettingsDialogs {
    public static Optional<ChannelSettingsResponse> showEditChannelSettingsDialogAndWait(ChannelSettings currentSettings,
                                                                                         String channelName) {
        return showChannelSettingsDialogAndWait(currentSettings, channelName, false);
    }

    public static Optional<ChannelSettingsResponse> showNewChannelSettingsDialogAndWait(String channelName) {
        return showChannelSettingsDialogAndWait(ChannelSettings.defaultSettings(), channelName, true);
    }

    private static Optional<ChannelSettingsResponse> showChannelSettingsDialogAndWait(ChannelSettings currentSettings,
                                                                                      String channelName, boolean isNew) {
        try {
            FXMLLoader loader = new FXMLLoader(Dialogs.class
                .getResource("/pl/mewash/subscriptions/ui/channel-settings-view.fxml"));

            DialogPane pane = loader.load();
            ChannelSettingsController controller = loader.getController();
            controller.loadSettingsOnUi(currentSettings);

            Dialog<ChannelSettingsResponse> dialog = setupDialogContext(pane, channelName, isNew);

            dialog.setResultConverter(buttonType -> {
                if (buttonType == null) return new ChannelSettingsResponse(AnswerButton.CANCEL, null);
                AnswerButton answer = AnswerButton.mapAnswer(buttonType.getButtonData());
                return switch (answer) {
                    case APPLY -> new ChannelSettingsResponse(answer, controller.getSelectedSettings());
                    case CANCEL -> new ChannelSettingsResponse(answer, currentSettings);
                    case OTHER -> {
                        boolean confirmedDeletion = isNew
                            ? true // no confirmation needed for aborting new channel addition
                            : showConfirmationDialog("""
                            Are you sure you want to delete channel?
                            You will lose fetched history and tracking of all saved contents!""");
                        yield confirmedDeletion
                            ? new ChannelSettingsResponse(answer, null)
                            : new ChannelSettingsResponse(AnswerButton.CANCEL, currentSettings);
                    }
                };
            });

            return dialog.showAndWait();

        } catch (IOException e) {
            AppContext.getInstance().getFileLogger().logErrStackTrace(e, true);
            return Optional.empty();
        }
    }

    private static Dialog<ChannelSettingsResponse> setupDialogContext(DialogPane pane, String chName, boolean isNew) {
        Dialog<ChannelSettingsResponse> dialog = new Dialog<>();
        dialog.setTitle(isNew
            ? String.format("Setup new channel: %s", chName)
            : String.format("Edit channel %s settings", chName));
        dialog.setDialogPane(pane);

        List<ButtonType> resolvedButtons = pane.getButtonTypes().stream()
            .map(btn -> AnswerButton.resolveButtonTitle(btn.getButtonData(), isNew))
            .toList();
        pane.getButtonTypes().setAll(resolvedButtons);

        Node applyButton = pane.lookupButton(resolvedButtons.stream()
            .filter(btn -> btn.getButtonData() == ButtonBar.ButtonData.APPLY)
            .findFirst()
            .orElse(null));
        if (applyButton instanceof Button) {
            ((Button) applyButton).setDefaultButton(true);
        }

        Node otherButton = pane.lookupButton(resolvedButtons.stream()
            .filter(btn -> btn.getButtonData() == ButtonBar.ButtonData.OTHER)
            .findFirst()
            .orElse(null));
        if (otherButton != null) {
            otherButton.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white;");
        }
        return dialog;
    }

    private static boolean showConfirmationDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);

        alert.getButtonTypes().setAll(yesButton, noButton);

        alert.getDialogPane().lookupButton(yesButton).setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white;");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yesButton;
    }

    @RequiredArgsConstructor
    public enum AnswerButton {
        APPLY("Apply", "Apply", ButtonBar.ButtonData.APPLY),
        OTHER("Abort", "Delete Channel", ButtonBar.ButtonData.OTHER),
        CANCEL("Set Defaults", "Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        private final String newTitle;
        private final String editTitle;
        private final ButtonBar.ButtonData type;

        private static ButtonType resolveButtonTitle(ButtonBar.ButtonData buttonData, boolean isNew) {
            return Arrays.stream(values())
                .filter(v -> v.type.equals(buttonData))
                .findFirst()
                .map(v -> new ButtonType(isNew ? v.newTitle : v.editTitle, buttonData))
                .orElseThrow(() -> new IllegalArgumentException("No such button type"));
        }

        private static AnswerButton mapAnswer(ButtonBar.ButtonData buttonData) {
            return Arrays.stream(values())
                .filter(v -> v.type.equals(buttonData))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such button type"));
        }
    }

    public record ChannelSettingsResponse(AnswerButton answerButton, ChannelSettings settings) {
    }
}
