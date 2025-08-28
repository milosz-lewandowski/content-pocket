package pl.mewash.subscriptions.internal.service;

import javafx.scene.control.Alert;
import pl.mewash.commands.api.processes.ProcessFactory;
import pl.mewash.commands.api.processes.ProcessFactoryProvider;
import pl.mewash.commands.settings.response.ContentProperties;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.logging.api.FileLogger;
import pl.mewash.common.temporary.CommandsDiffDetector;
import pl.mewash.subscriptions.internal.domain.model.FetchedContent;
import pl.mewash.subscriptions.internal.domain.model.SubscribedChannel;
import pl.mewash.subscriptions.internal.domain.state.ChannelUiState;
import pl.mewash.subscriptions.internal.domain.state.ProgressiveFetchRange;
import pl.mewash.subscriptions.internal.domain.state.ProgressiveFetchStage;
import pl.mewash.subscriptions.internal.persistence.impl.SubscriptionsJsonRepo;
import pl.mewash.subscriptions.internal.persistence.repo.SubscriptionsRepository;
import pl.mewash.subscriptions.ui.dialogs.Dialogs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
        try {
            ContentProperties responseProperties = ContentProperties.CONTENT_PROPERTIES;

            Path tempFile = Files.createTempFile("fetch_uploads_temp", ".txt");


            // FIXME: TEMPORARY CHECKER
            CommandsDiffDetector commandsDiffDetector = new CommandsDiffDetector();
            commandsDiffDetector
                .fetchContentsPublishedAfter(channel.getUniqueUrl(), dateAfter, responseProperties, tempFile.toAbsolutePath());


            ProcessBuilder builder = processFactory.fetchContentsPublishedAfter(
                channel.getUniqueUrl(), dateAfter, responseProperties, tempFile.toAbsolutePath());

            Process process = builder.start();

            List<String> output = fileLogger.getProcessOutputAndLogToFile(process);

            int finished = process.waitFor();

            boolean isFullFetched = output.getLast().contains("Finished downloading playlist");

            List<String> lines = Files.readAllLines(tempFile, StandardCharsets.UTF_8);
            List<FetchedContent> fetchedContents = lines.stream()
                .map(ContentProperties.CONTENT_PROPERTIES::parseResponseToDto)
                .map(contentDto -> FetchedContent
                    .fromContentPropertiesResponse(contentDto, channel.getUniqueUrl(), channel.getChannelName()))
                .toList();

            Files.deleteIfExists(tempFile);
            return new FetchResults(fetchedContents, isFullFetched);

        } catch (IOException | InterruptedException e) {
            fileLogger.appendSingleLine(e.getMessage());
            Dialogs.showAlertAndAwait("Fetch Error", e.getMessage(), Alert.AlertType.ERROR);
            return new FetchResults(Collections.emptyList(), false);
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
