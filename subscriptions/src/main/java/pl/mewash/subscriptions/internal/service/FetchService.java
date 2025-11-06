package pl.mewash.subscriptions.internal.service;

import javafx.scene.control.Alert;
import pl.mewash.commands.api.processes.ProcessFactory;
import pl.mewash.commands.api.processes.ProcessFactoryProvider;
import pl.mewash.commands.settings.response.ContentProperties;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.logging.api.FileLogger;
import pl.mewash.common.logging.api.ProcessLogger;
import pl.mewash.subscriptions.internal.domain.model.FetchedContent;
import pl.mewash.subscriptions.internal.domain.model.SubscribedChannel;
import pl.mewash.subscriptions.internal.domain.state.ChannelUiState;
import pl.mewash.subscriptions.internal.domain.state.ProgressiveFetchRange;
import pl.mewash.subscriptions.internal.domain.state.ProgressiveFetchStage;
import pl.mewash.subscriptions.internal.persistence.repo.SubscriptionsRepository;
import pl.mewash.subscriptions.internal.persistence.storage.SubscriptionsJsonRepo;
import pl.mewash.subscriptions.ui.dialogs.Dialogs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FetchService {

    private final SubscriptionsRepository repository;
    private final ProcessFactory processFactory;
    private final FileLogger fileLogger;

    public FetchService(AppContext appContext) {
        fileLogger = appContext.getFileLogger();
        repository = SubscriptionsJsonRepo.getInstance();
        processFactory = ProcessFactoryProvider.createDefaultFactoryWithLogger(
            appContext.getYtDlpCommand(), appContext.getFfMpegCommand(), fileLogger::appendSingleLine, true
        );
    }

    public boolean fetch(ChannelUiState channelState) {
        SubscribedChannel channel = channelState.getChannel();
        ProgressiveFetchStage initialStage = channelState.getFetchingStage().get();
        ProgressiveFetchRange initialRange = channelState.getFetchRange();

        channelState.setFetchingStage(ProgressiveFetchStage.FETCHING);

        LocalDateTime dateAfter = calculateDateAfter(channel, initialStage, initialRange);

        FetchResults fetchResults = runFetchUploadsAfter(channel, dateAfter);

        channel.setLastFetched(LocalDateTime.now());

        LocalDateTime previousRangeDate = channel.getPreviousFetchOlderRangeDate();
        LocalDateTime oldestFetchRange = previousRangeDate != null && previousRangeDate.isBefore(dateAfter)
            ? previousRangeDate
            : dateAfter;
        channel.setPreviousFetchOlderRangeDate(oldestFetchRange);

        boolean foundNew = channel.appendFetchedContentsIfNotPresent(fetchResults.fetchedContents);

        if (fetchResults.isFullFetched) channel.setFetchedSinceOldest(true);
        else if (!foundNew && initialRange == ProgressiveFetchRange.LAST_25_YEARS) channel.setFetchedSinceOldest(true);
        else if (initialRange == ProgressiveFetchRange.LAST_25_YEARS) channel.setFetchedSinceOldest(true);

        repository.updateChannel(channel);

        LocalDateTime oldestContentOrPreviousFetchDateAfter = channel.calculateNextFetchOlderInputDate();

        ProgressiveFetchStage resultStage = channel.isFetchedSinceOldest()
            ? ProgressiveFetchStage.ALL_FETCHED
            : ProgressiveFetchStage.FETCH_OLDER;

        ProgressiveFetchRange resultRange = (resultStage == ProgressiveFetchStage.ALL_FETCHED)
            ? ProgressiveFetchRange.LAST_25_YEARS
            : resultStage.getProgressiveRange(oldestContentOrPreviousFetchDateAfter);

        channelState.setFetchingStageWithRange(resultRange, resultStage);
        return foundNew;
    }

    private FetchResults runFetchUploadsAfter(SubscribedChannel channel, LocalDateTime dateAfter) {
        ProcessLogger processLogger = null;
        ScheduledExecutorService progressWatchdog = null;

        try {
            // --- Setup fetch process ---
            ContentProperties responseProperties = ContentProperties.CONTENT_PROPERTIES;
            Path tempFile = Files.createTempFile("fetch_uploads_temp", ".txt");
            //FIXME: temp: ensures single playlist with '/videos' suffix
            String resolvedUrl = channel.getUniqueUrl() + "/videos";
            ProcessBuilder builder = processFactory.fetchContentsPublishedAfter(
                resolvedUrl, dateAfter, responseProperties, tempFile.toAbsolutePath());

            // -- Setup logger and watchdog ---
            processLogger = fileLogger.getNewProcessLogger();
            progressWatchdog = Executors.newSingleThreadScheduledExecutor();
            ProcessLogger finalProcessLogger = processLogger;
            ScheduledExecutorService finalProgressWatchdog = progressWatchdog;

            // --- Start the fetch process, logger and watchdog ---
            Process process = builder.start();
            processLogger.startLogging(process);
            AtomicLong lastModified = new AtomicLong(System.currentTimeMillis());
            progressWatchdog.scheduleAtFixedRate(() -> {
                try {
                    if (!process.isAlive()) {
                        finalProgressWatchdog.shutdown();
                        return;
                    }

                    long currentModified = Files.getLastModifiedTime(tempFile).toMillis();
                    long lastMod = lastModified.get();

                    if (currentModified > lastMod) lastModified.set(currentModified);
                    else if (System.currentTimeMillis() - lastMod > 20_000) {
                        fileLogger.appendSingleLine("No new fetches for 20 seconds, destroying process...");
                        process.destroyForcibly();
                        finalProcessLogger.shutdownAndGetSnapshot();
                        finalProgressWatchdog.shutdown();
                    }
                } catch (IOException ioe) {
                    fileLogger.logErrWithMessage("Error accessing fetch temp file: ", ioe, true);
                }
            }, 5, 7, TimeUnit.SECONDS);

            // --- Await fetch and logger processes completion ---
            int exitCode = process.waitFor();
            List<String> output = processLogger.awaitCompletion(5500);
            boolean isFullFetched =  !output.isEmpty() && output.getLast().contains("Finished downloading playlist");
            fileLogger.appendSingleLine("Fetch process finished with exit code: " + exitCode);

            // --- Parse response from temp file and cleanup ---
            List<String> lines = Files.readAllLines(tempFile, StandardCharsets.UTF_8);
            List<FetchedContent> fetchedContents = lines.stream()
                .map(ContentProperties.CONTENT_PROPERTIES::parseResponseToDto)
                .map(contentDto -> FetchedContent
                    .fromContentPropertiesResponse(contentDto, channel.getUniqueUrl(), channel.getChannelName()))
                .toList();
            Files.deleteIfExists(tempFile);
            return new FetchResults(fetchedContents, isFullFetched);

        } catch (IOException | InterruptedException e) {
            fileLogger.logErrStackTrace(e, true);
            Dialogs.showAlertAndAwait("Fetch Error", e.getMessage(), Alert.AlertType.ERROR);
            return new FetchResults(Collections.emptyList(), false);

        } finally {
            if (processLogger != null) processLogger.shutdownAndGetSnapshot();
            if (progressWatchdog != null) progressWatchdog.shutdownNow();
        }
    }

    private LocalDateTime calculateDateAfter(SubscribedChannel channel,
                                             ProgressiveFetchStage stage,
                                             ProgressiveFetchRange fetchRange
    ) {
        return switch (stage) {
            case FIRST_FETCH -> {
                LocalDateTime defaultChannelDateAfter = LocalDateTime.now()
                    .minus(channel.getChannelSettings().getInitialFetchPeriod());
                LocalDateTime latestChannelContentDate = channel.getLatestContentOnChannelDate();

                yield latestChannelContentDate.isBefore(defaultChannelDateAfter)
                    ? latestChannelContentDate.minusDays(1)
                    : defaultChannelDateAfter;
            }
            case FETCH_LATEST -> {
                LocalDateTime lastFetchedDate = channel.getLastFetched();
                LocalDateTime mostRecentFetchedContent = channel.findMostRecentFetchedContent().getPublished();

                yield mostRecentFetchedContent.isBefore(lastFetchedDate)
                    ? mostRecentFetchedContent.minusDays(1)
                    : lastFetchedDate.minusDays(1);
            }
            case FETCH_OLDER, ALL_FETCHED -> fetchRange.calculateDateAfter();

            default -> throw new IllegalStateException("Channel in " + stage + " stage cannot be fetched " + channel
                .getChannelName());
        };
    }

    record FetchResults(List<FetchedContent> fetchedContents, boolean isFullFetched) {}
}
