package pl.mewash.contentlaundry.service;

import javafx.scene.control.Alert;
import pl.mewash.contentlaundry.commands.CommandBuilder;
import pl.mewash.contentlaundry.commands.ProcessFactoryV2;
import pl.mewash.contentlaundry.controller.ChannelSettingsDialogLauncher;
import pl.mewash.contentlaundry.models.channel.ChannelFetchRepo;
import pl.mewash.contentlaundry.models.channel.ChannelSettings;
import pl.mewash.contentlaundry.models.channel.SubscribedChannel;
import pl.mewash.contentlaundry.models.ui.ChannelUiState;
import pl.mewash.contentlaundry.utils.AlertUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ChannelService {

    private final ChannelFetchRepo repository = ChannelFetchRepo.getInstance();

    public Optional<ChannelUiState> verifyAndAddChannel(String channelUrl) {
        Optional<SubscribedChannel> subscribedChannel = runCheckChannelProcess(channelUrl);

        if (subscribedChannel.isPresent()) {
            SubscribedChannel channel = subscribedChannel.get();

            ChannelSettings channelSettings = showChannelSettingsSetupPopup();
            channel.setChannelSettings(channelSettings);

            repository.addChannel(channel);
            return Optional.of(repository.getChannelUiState(channel.getChannelName()));
        } else return Optional.empty();
    }

    private Optional<SubscribedChannel> runCheckChannelProcess(String channelUrl) {
        try {
            Path tempFile = Files.createTempFile("yt_channel", ".txt");

            ProcessBuilder checkChannelProcess = ProcessFactoryV2.buildCheckChannelAndLatestContent(channelUrl, tempFile);
            checkChannelProcess.redirectOutput(tempFile.toFile());
            checkChannelProcess.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process process = checkChannelProcess.start();
            String channelName;
            String responseLine;

            int exitCode = process.waitFor();
            responseLine = Files.readString(tempFile, StandardCharsets.UTF_8).trim();

            String[] lines = responseLine.split(CommandBuilder.PrintToFileOptions.CHANNEL_NAME_LATEST_CONTENT.getSplitRegex());
            channelName = lines[0].trim();
            String latestContentString = lines[1].trim();

            Files.deleteIfExists(tempFile);

            if (exitCode != 0 || channelName.isBlank()) {
                AlertUtils.showAlertAndAwait("Channel check failed", "Could not retrieve channel name.", Alert.AlertType.ERROR);
                return Optional.empty();
            }
            return Optional.of(SubscribedChannel.withLatestContent(channelName, channelUrl, latestContentString));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            AlertUtils.showAlertAndAwait("Error of channel check", e.getMessage(), Alert.AlertType.ERROR);
            return Optional.empty();
        }
    }

    private ChannelSettings showChannelSettingsSetupPopup() {
        Optional<ChannelSettings> settings = ChannelSettingsDialogLauncher
                .showDialogAndWait(ChannelSettings.defaultSettings());
        return settings.orElse(ChannelSettings.defaultSettings());
    }
}
