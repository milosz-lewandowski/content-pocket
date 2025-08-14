package pl.mewash.subscriptions.internal.service;

import javafx.scene.control.Alert;
import pl.mewash.commands.api.ProcessFactory;
import pl.mewash.commands.api.ProcessFactoryProvider;
import pl.mewash.commands.settings.response.ContentProperties;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.logging.api.FileLogger;
import pl.mewash.subscriptions.internal.persistence.impl.SubscriptionsJsonRepo;
import pl.mewash.subscriptions.internal.domain.model.SubscribedChannel;
import pl.mewash.subscriptions.internal.domain.dto.ChannelFetchParams;
import pl.mewash.subscriptions.internal.domain.state.ChannelFetchingStage;
import pl.mewash.subscriptions.internal.domain.model.FetchedContent;
import pl.mewash.subscriptions.internal.domain.dto.FetchingResults;
import pl.mewash.subscriptions.internal.persistence.repo.SubscriptionsRepository;
import pl.mewash.subscriptions.ui.dialogs.DialogLauncher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class FetchService {

    private final SubscriptionsRepository repository;
    private final ProcessFactory processFactory;
    private final FileLogger fileLogger;

    public FetchService(AppContext appContext) {
        fileLogger = appContext.getFileLogger();
        repository = SubscriptionsJsonRepo.getInstance();
        processFactory = ProcessFactoryProvider.createDefaultWithConsolePrintAndLogger(
                appContext.getYtDlpCommand(), appContext.getFfMpegCommand(), fileLogger::appendSingleLine, false
        );
    }

    public ChannelFetchParams fetch(String channelUrl, ChannelFetchParams fetchParams) {
        SubscribedChannel channel = repository.getChannel(channelUrl);

        LocalDateTime dateAfter = calculateDateAfter(channel, fetchParams.stage(), fetchParams.fetchOlder());
        Duration timeout = calculateTimeout();

        Optional<FetchingResults> resultsOp = runFetchUploadsAfter(channel, dateAfter, timeout);

        if (resultsOp.isPresent()) {
            FetchingResults results = resultsOp.get();
            List<FetchedContent> fetchedContents = results.fetchedContents();

            channel.setLastFetched(LocalDateTime.now());
            channel.appendFetchedContents(fetchedContents);
            channel.setPreviousFetchOlderRangeDate(dateAfter);

            repository.updateChannel(channel);

            SubscribedChannel updatedChannel = repository.getChannel(channelUrl); // test if re-synchro helps

            LocalDateTime oldestContentOrPreviousFetchDateAfter = updatedChannel.calculateNextFetchOlderInputDate();
            ChannelFetchingStage fetchButtonStage = ChannelFetchingStage.FETCH_OLDER;
            ChannelFetchingStage.FetchOlderRange fetchOlderRange = fetchButtonStage
                    .getOlderRange(oldestContentOrPreviousFetchDateAfter);

            return new ChannelFetchParams(ChannelFetchingStage.FETCH_OLDER, fetchOlderRange);

        } else return new ChannelFetchParams(ChannelFetchingStage.FETCH_ERROR, null);
    }

    public Optional<FetchingResults> runFetchUploadsAfter(SubscribedChannel channel, LocalDateTime dateAfter, Duration timeout) {
        long currentTimeout = timeout.getSeconds();
        try {
            ContentProperties responseProperties = ContentProperties.CONTENT_PROPERTIES;

            Path tempFile = Files.createTempFile("fetch_uploads_temp", ".txt");

            ProcessBuilder builder = processFactory.fetchContentsPublishedAfter(
                    channel.getUrl(), dateAfter, responseProperties, tempFile.toAbsolutePath());

            Process process = builder.start();

            fileLogger.consumeAndLogProcessOutputToFile(process);

            boolean finished = process.waitFor(timeout.getSeconds(), TimeUnit.SECONDS);

            List<String> lines = Files.readAllLines(tempFile, StandardCharsets.UTF_8);
            List<FetchedContent> fetchedContents = lines.stream()
                    .map(line -> FetchedContent.fromContentPropertiesResponse(line, channel.getUrl()))
                    .toList();

            Files.deleteIfExists(tempFile);

            if (!finished) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();

                long estimatedTimeout = calculateEstimatedTimeout(fetchedContents, dateAfter, currentTimeout);
                return Optional.of(new FetchingResults(fetchedContents, false, estimatedTimeout));
            }

            Files.deleteIfExists(tempFile);
            return Optional.of(new FetchingResults(fetchedContents, true, 0));

        } catch (IOException | InterruptedException e) {
            fileLogger.appendSingleLine(e.getMessage());
            DialogLauncher.showAlertAndAwait("Fetch Error", e.getMessage(), Alert.AlertType.ERROR);
            return Optional.empty();
        }
    }

    private static long calculateEstimatedTimeout(List<FetchedContent> fetchedContents,
                                                  LocalDateTime targetDate, long currentTimeout) {
        LocalDateTime fetchedUntil = fetchedContents.stream()
                .map(FetchedContent::getPublished)
                .min(LocalDateTime::compareTo)
                .orElseThrow();
        long targetDuration = Duration.between(targetDate, LocalDateTime.now()).getSeconds();
        long currentDuration = Duration.between(fetchedUntil, LocalDateTime.now()).getSeconds();
        double completedProportion = (double) currentDuration / targetDuration;
        double exactResult = currentTimeout / completedProportion;
        double estimated = exactResult * 1.25;
        return (long) estimated + 10;
    }

    private Duration calculateTimeout(){
        return Duration.ofSeconds(180);
    }

    private LocalDateTime calculateDateAfter(SubscribedChannel channel,
                                             ChannelFetchingStage stage,
                                             ChannelFetchingStage.FetchOlderRange fetchOlderRange
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
            case FETCH_OLDER -> fetchOlderRange.calculateDateAfter();

            default -> throw new IllegalStateException("Channel in " + stage + " stage cannot be fetched " + channel
                .getChannelName());
        };
    }
}
