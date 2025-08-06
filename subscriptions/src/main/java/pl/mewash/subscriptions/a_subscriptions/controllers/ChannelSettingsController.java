package pl.mewash.subscriptions.a_subscriptions.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Window;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.subscriptions.a_subscriptions.models.channel.ChannelSettings;

import java.time.Period;

public class ChannelSettingsController {

    @FXML private CheckBox autoFetchLastestOnStartup;
    @FXML private CheckBox fullFetch;

    @FXML private ComboBox<AudioOnlyQuality> audioQualityCombo;
    @FXML private ComboBox<VideoQuality> videoQualityCombo;
    @FXML private CheckBox autoDownloadAudio;
    @FXML private CheckBox autoDownloadVideo;

    @FXML private CheckBox addDownloadDateDir;
    @FXML private CheckBox separateDirPerFormat;

    @FXML private Button okButton;
    @FXML private Button cancelButton;

    private ChannelSettings selectedSettings;

    public void initialize() {
        audioQualityCombo.getItems().setAll(AudioOnlyQuality.values());
        videoQualityCombo.getItems().setAll(VideoQuality.values());

        okButton.setOnAction(e -> {
            Period fetchPeriod = fullFetch.isSelected()
                    ? Period.ofYears(25)
                    : Period.ofDays(14);

            selectedSettings = ChannelSettings.builder()
                    .autoFetchLastestOnStartup(autoFetchLastestOnStartup.isSelected())
                    .fullFetch(fullFetch.isSelected())
                    .defaultAudio(audioQualityCombo.getValue())
                    .defaultVideo(videoQualityCombo.getValue())
                    .autoDownloadAudio(autoDownloadAudio.isSelected())
                    .autoDownloadVideo(autoDownloadVideo.isSelected())
                    .addDownloadDateDir(addDownloadDateDir.isSelected())
                    .separateDirPerFormat(separateDirPerFormat.isSelected())
                    .initialFetchPeriod(fetchPeriod)
                    .build();

            closeDialog(okButton);
        });

        cancelButton.setOnAction(e -> {
            selectedSettings = null;
            closeDialog(cancelButton);
        });
    }

    private void closeDialog(Control source) {
        DialogPane pane = (DialogPane) source.getScene().getRoot();
        Window window = pane.getScene().getWindow();
        window.hide();
    }

    public void loadSettingsOnUi(ChannelSettings settings) {
        autoFetchLastestOnStartup.setSelected(settings.isAutoFetchLastestOnStartup());
        fullFetch.setSelected(settings.isFullFetch());
        audioQualityCombo.setValue(settings.getDefaultAudio());
        videoQualityCombo.setValue(settings.getDefaultVideo());
        autoDownloadAudio.setSelected(settings.isAutoDownloadAudio());
        autoDownloadVideo.setSelected(settings.isAutoDownloadVideo());
        addDownloadDateDir.setSelected(settings.isAddDownloadDateDir());
        separateDirPerFormat.setSelected(settings.isSeparateDirPerFormat());
    }

    public ChannelSettings getSelectedSettings() {
        return selectedSettings;
    }
}
