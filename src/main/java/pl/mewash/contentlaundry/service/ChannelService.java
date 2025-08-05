package pl.mewash.contentlaundry.service;

import javafx.scene.control.Alert;
import pl.mewash.commands.api.CommandLogger;
import pl.mewash.commands.api.ProcessFactory;
import pl.mewash.commands.api.ProcessFactoryProvider;
import pl.mewash.commands.settings.response.ChannelProperties;
import pl.mewash.contentlaundry.AppContext;
import pl.mewash.contentlaundry.controller.ChannelSettingsDialogLauncher;
import pl.mewash.contentlaundry.models.channel.ChannelFetchRepo;
import pl.mewash.contentlaundry.models.channel.ChannelSettings;
import pl.mewash.contentlaundry.models.channel.SubscribedChannel;
import pl.mewash.contentlaundry.models.ui.ChannelUiState;
import pl.mewash.contentlaundry.utils.AlertUtils;
import pl.mewash.contentlaundry.utils.ScheduledFileLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class ChannelService {

    private final ProcessFactory processFactory;

    public ChannelService(AppContext appContext, CommandLogger commandLogger){
        processFactory = ProcessFactoryProvider.createDefaultWithConsolePrintAndLogger(
                appContext.getYtDlpCommand(), appContext.getFfMpegCommand(), commandLogger, true
        );
    }

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

            ChannelProperties responseProperties = ChannelProperties.CHANNEL_NAME_LATEST_CONTENT;

            ProcessBuilder checkChannelProcess = processFactory
                    .fetchChannelBasicData(channelUrl, responseProperties, tempFile);
            checkChannelProcess.redirectOutput(tempFile.toFile());
            checkChannelProcess.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process process = checkChannelProcess.start();
            String channelName;

            int exitCode = process.waitFor();
            String responseLine = Files.readString(tempFile, StandardCharsets.UTF_8).trim();

            ChannelProperties.ChannelResponseDto channelResponseDto = responseProperties.parseResponseToDto(responseLine);
            channelName = channelResponseDto.getChannelName();


            Files.deleteIfExists(tempFile);

            if (exitCode != 0 || channelName.isBlank()) {
                AlertUtils.showAlertAndAwait("Channel check failed", "Could not retrieve channel name.", Alert.AlertType.ERROR);
                return Optional.empty();
            }
            return Optional.of(SubscribedChannel.withLatestContent(channelName, channelUrl, channelResponseDto.getLatestContentDate()));
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
