package pl.mewash.subscriptions.internal.service;

import javafx.scene.control.Alert;
import pl.mewash.commands.api.processes.ProcessFactory;
import pl.mewash.commands.api.processes.ProcessFactoryProvider;
import pl.mewash.commands.settings.response.ChannelProperties;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.logging.api.FileLogger;
import pl.mewash.common.temporary.CommandsDiffDetector;
import pl.mewash.subscriptions.internal.domain.model.SubscribedChannel;
import pl.mewash.subscriptions.internal.domain.state.ChannelUiState;
import pl.mewash.subscriptions.internal.persistence.storage.SubscriptionsJsonRepo;
import pl.mewash.subscriptions.internal.persistence.repo.SubscriptionsRepository;
import pl.mewash.subscriptions.ui.dialogs.Dialogs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ChannelService {

    private final ProcessFactory processFactory;
    private final SubscriptionsRepository repository;
    private final FileLogger fileLogger;

    public ChannelService(AppContext appContext) {
        repository = SubscriptionsJsonRepo.getInstance();
        fileLogger = appContext.getFileLogger();
        processFactory = ProcessFactoryProvider.createDefaultFactoryWithLogger(
            appContext.getYtDlpCommand(), appContext.getFfMpegCommand(),
            fileLogger::appendSingleLine, true
        );
    }

    public Optional<SubscribedChannel> verifyAndGetChannel(String inputChannelUrl) {

        return runCheckChannelProcess(inputChannelUrl.trim())
            .flatMap(initialResponse -> initialResponse.getChannelUrl().equals(inputChannelUrl)
                ? Optional.of(initialResponse)
                    // second call ensures getting valid channel data even if content url was prompted
                : runCheckChannelProcess(initialResponse.getChannelUrl()))
            .map(uniqueUrlResp -> SubscribedChannel.withLatestContent(inputChannelUrl,
                uniqueUrlResp.getChannelName(), uniqueUrlResp.getChannelUrl(), uniqueUrlResp.getLatestContentDate()));
    }

    public ChannelUiState saveChannelAndGetState(SubscribedChannel subscribedChannel) {
        repository.addChannel(subscribedChannel);
        return repository.getChannelUiState(subscribedChannel.getUniqueUrl());
    }

    private Optional<ChannelProperties.ChannelResponseDto> runCheckChannelProcess(String inputChannelUrl) {
        try {
            Path tempFile = Files.createTempFile("yt_channel", ".txt");

            ChannelProperties responseProperties = ChannelProperties.CHANNEL_NAME_LATEST_CONTENT;



            // FIXME: TEMPORARY CHECKER
            CommandsDiffDetector commandsDiffDetector = new CommandsDiffDetector();
            commandsDiffDetector
                .fetchChannelBasicData(inputChannelUrl, responseProperties, tempFile);



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
                Dialogs.showAlertAndAwait("Channel check failed", "Could not retrieve channel name.", Alert.AlertType.ERROR);
                return Optional.empty();
            }
            return Optional.of(channelResponseDto);
        } catch (IOException | InterruptedException e) {
            fileLogger.logErrStackTrace(e, true);
            Dialogs.showAlertAndAwait("Error of channel check", e.getMessage(), Alert.AlertType.ERROR);
            return Optional.empty();
        }
    }


}
