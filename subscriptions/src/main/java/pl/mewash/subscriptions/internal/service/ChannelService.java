package pl.mewash.subscriptions.internal.service;

import javafx.scene.control.Alert;
import pl.mewash.commands.api.ProcessFactory;
import pl.mewash.commands.api.ProcessFactoryProvider;
import pl.mewash.commands.settings.response.ChannelProperties;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.subscriptions.internal.domain.model.ChannelSettings;
import pl.mewash.subscriptions.internal.domain.state.ChannelUiState;
import pl.mewash.subscriptions.internal.domain.model.SubscribedChannel;
import pl.mewash.subscriptions.internal.persistence.impl.SubscriptionsJsonRepo;
import pl.mewash.subscriptions.internal.persistence.repo.SubscriptionsRepository;
import pl.mewash.subscriptions.ui.dialogs.DialogLauncher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ChannelService {

    private final ProcessFactory processFactory;
    private final SubscriptionsRepository repository;

    public ChannelService(AppContext appContext) {
        repository = SubscriptionsJsonRepo.getInstance();
        processFactory = ProcessFactoryProvider.createDefaultWithConsolePrintAndLogger(
            appContext.getYtDlpCommand(), appContext.getFfMpegCommand(),
            appContext.getFileLogger()::appendSingleLine, true
        );
    }

    public Optional<ChannelUiState> verifyAndAddChannel(String channelUrl) {
        return runCheckChannelProcessWithValidationAndRetry(channelUrl.trim())
            .map(verifiedChannel -> {
                ChannelSettings settings = showChannelSettingsSetupPopup();
                verifiedChannel.setChannelSettings(settings);

                repository.addChannel(verifiedChannel);
                return repository.getChannelUiState(verifiedChannel.getUrl());
            });
    }

    private Optional<SubscribedChannel> runCheckChannelProcessWithValidationAndRetry(String inputChannelUrl) {

        return runCheckChannelProcess(inputChannelUrl)
            .flatMap(initialResponse -> initialResponse.getChannelUrl().equals(inputChannelUrl)
                ? Optional.of(initialResponse)
                    // second call ensures getting valid channel data even if content url was prompted
                : runCheckChannelProcess(initialResponse.getChannelUrl()))
            .map(uniqueUrlResp -> SubscribedChannel.withLatestContent(inputChannelUrl,
                uniqueUrlResp.getChannelName(), uniqueUrlResp.getChannelUrl(), uniqueUrlResp.getLatestContentDate()));
    }

    private Optional<ChannelProperties.ChannelResponseDto> runCheckChannelProcess(String inputChannelUrl) {
        try {
            Path tempFile = Files.createTempFile("yt_channel", ".txt");

            ChannelProperties responseProperties = ChannelProperties.CHANNEL_NAME_LATEST_CONTENT;

            ProcessBuilder checkChannelProcess = processFactory
                .fetchChannelBasicData(inputChannelUrl, responseProperties, tempFile);
            checkChannelProcess.redirectOutput(tempFile.toFile());
            checkChannelProcess.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process process = checkChannelProcess.start();

            int exitCode = process.waitFor();
            String responseLine = Files.readString(tempFile, StandardCharsets.UTF_8).trim();

            ChannelProperties.ChannelResponseDto channelResponseDto = responseProperties.parseResponseToDto(responseLine);

            Files.deleteIfExists(tempFile);

            if (exitCode != 0 || channelResponseDto.getChannelName().isBlank()) {
                DialogLauncher.showAlertAndAwait("Channel check failed", "Could not retrieve channel name.", Alert.AlertType.ERROR);
                return Optional.empty();
            }
            return Optional.of(channelResponseDto);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            DialogLauncher.showAlertAndAwait("Error of channel check", e.getMessage(), Alert.AlertType.ERROR);
            return Optional.empty();
        }
    }

    private ChannelSettings showChannelSettingsSetupPopup() {
        Optional<ChannelSettings> settings = DialogLauncher
            .showChannelSettingsDialogAndWait(ChannelSettings.defaultSettings());
        return settings.orElse(ChannelSettings.defaultSettings());
    }
}
