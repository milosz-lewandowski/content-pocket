package pl.mewash.contentlaundry.service;

import javafx.scene.control.Alert;
import pl.mewash.contentlaundry.commands.ProcessFactoryV2;
import pl.mewash.contentlaundry.models.channel.ChannelFetchRepo;
import pl.mewash.contentlaundry.models.channel.SubscribedChannel;
import pl.mewash.contentlaundry.models.channel.enums.ChannelFetchingStage;
import pl.mewash.contentlaundry.models.channel.enums.ChannelFetchParams;
import pl.mewash.contentlaundry.models.content.FetchedContent;
import pl.mewash.contentlaundry.models.content.FetchingResults;
import pl.mewash.contentlaundry.utils.AlertUtils;
import pl.mewash.contentlaundry.utils.ScheduledFileLogger;

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

    private final ChannelFetchRepo repository = ChannelFetchRepo.getInstance();

    public ChannelFetchParams fetch(String channelName, ChannelFetchParams fetchParams) {
        SubscribedChannel channel = repository.getChannel(channelName);

        LocalDateTime dateAfter = calculateDateAfter(channel, fetchParams.stage(), fetchParams.fetchOlder());
        Duration timeout = calculateTimeout();

        Optional<FetchingResults> resultsOp = runFetchUploadsAfter(channel, dateAfter, timeout);

//        System.out.println(" after fetch in Fetch Service");
        if (resultsOp.isPresent()) {
            FetchingResults results = resultsOp.get();
            List<FetchedContent> fetchedContents = results.fetchedContents();
//            System.out.println(" results present in Fetch Service");

            // if required insert timeout logic

            channel.setLastFetched(LocalDateTime.now());
            channel.appendFetchedContents(fetchedContents);
            channel.setPreviousFetchOlderRangeDate(dateAfter);
            repository.updateChannel(channel);
//            System.out.println("after channel update in Fetch Service");

            SubscribedChannel updatedChannel = repository.getChannel(channelName); // test if re-synchro helps
//            System.out.println(" updatedChannel present in Fetch Service");

            LocalDateTime oldestContentOrPreviousFetchDateAfter = updatedChannel.calculateNextFetchOlderInputDate();
            ChannelFetchingStage fetchButtonStage = ChannelFetchingStage.FETCH_OLDER;
            ChannelFetchingStage.FetchOlderRange fetchOlderRange = fetchButtonStage
                    .getOlderRange(oldestContentOrPreviousFetchDateAfter);
//            System.out.println(" fetchOlderRange present in Fetch Service: " + fetchOlderRange.getButtonTitle());

            return new ChannelFetchParams(ChannelFetchingStage.FETCH_OLDER, fetchOlderRange);

            // if fetch failed return error state
        } else return new ChannelFetchParams(ChannelFetchingStage.FETCH_ERROR, null);
    }

    public Optional<FetchingResults> runFetchUploadsAfter(SubscribedChannel channel, LocalDateTime dateAfter, Duration timeout) {
        long currentTimeout = timeout.getSeconds();
        try {
            Path tempFile = Files.createTempFile("fetch_uploads_temp", ".txt");

            ProcessBuilder builder = ProcessFactoryV2.buildFetchUploadListCommand(
                    channel.getUrl(), dateAfter, tempFile.toAbsolutePath());

            // Redirect error output to console
//            builder.redirectError(ProcessBuilder.Redirect.INHERIT);
//            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            long startTime = System.currentTimeMillis();
            Process process = builder.start();
//            System.out.println("process started, before logger: " + (System.currentTimeMillis() - startTime) / 1000);

            // TODO: causes thread blocking until process finished and fully consumed. at this moment this disables timeout reaching at all
            ScheduledFileLogger.consumeAndLogProcessOutputToFile(process);
//            System.out.println("process started, after logger: " + (System.currentTimeMillis() - startTime) / 1000);

//            System.out.println("process before wait for");
            boolean finished = process.waitFor(timeout.getSeconds(), TimeUnit.SECONDS);
//            System.out.println("process after wait for, process finished: " + finished);
//            System.out.println("process after wait for, process finished: " + (System.currentTimeMillis() - startTime) / 1000);


            List<String> lines = Files.readAllLines(tempFile, StandardCharsets.UTF_8);
//            System.out.println("lines saved : "+ (System.currentTimeMillis() - startTime) / 1000);
//            lines.forEach(System.out::println);


            List<FetchedContent> fetchedContents = lines.stream()
                    .map(line -> FetchedContent.fromContentPropertiesResponse(line, channel.getChannelName()))
                    .toList();

//            System.out.println("fetched contents : "+ (System.currentTimeMillis() - startTime) / 1000);
//            fetchedContents.forEach(System.out::println);

            Files.deleteIfExists(tempFile);

            if (!finished) {

//                System.out.println("entered destroy process: " + (System.currentTimeMillis() - startTime) / 1000);

                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();

                long estimatedTimeout = calculateEstimatedTimeout(fetchedContents, dateAfter, currentTimeout);
//                System.out.println("destroyed process: " + (System.currentTimeMillis() - startTime) / 1000);
//                System.out.println("destroyed process, calcuilated timeout: " + estimatedTimeout);

                return Optional.of(new FetchingResults(fetchedContents, false, estimatedTimeout));
            }
            Files.deleteIfExists(tempFile);
            return Optional.of(new FetchingResults(fetchedContents, true, 0));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            AlertUtils.showAlertAndAwait("Fetch Error", e.getMessage(), Alert.AlertType.ERROR);
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

    // FIXME: insert logic
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

            default -> throw new IllegalStateException("Channel in " + stage + " stage cannot be fetched " + channel.getChannelName());
        };
    }
}
