package pl.mewash.contentlaundry.service;

import javafx.scene.control.Alert;
import pl.mewash.contentlaundry.commands.CommandBuilder;
import pl.mewash.contentlaundry.commands.ProcessFactoryV2;
import pl.mewash.contentlaundry.models.channel.SubscribedChannel;
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

public class SubscriptionsProcessor {

    public static SubscribedChannel checkAndGetChannelName(String channelUrl) {
        try {
            Path tempFile = Files.createTempFile("yt_channel", ".txt");

//            ProcessBuilder checkChannelProcess = ProcessFactoryV2.buildCheckChannelCommand(channelUrl, tempFile);
            ProcessBuilder checkChannelProcess = ProcessFactoryV2.buildCheckChannelAndLatestContent(channelUrl, tempFile);
            checkChannelProcess.redirectOutput(tempFile.toFile());
            checkChannelProcess.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process process = checkChannelProcess.start();
            String channelName;
            String responseLine;

            int exitCode = process.waitFor();
//            channelName = Files.readString(tempFile, StandardCharsets.UTF_8).trim();
            responseLine = Files.readString(tempFile, StandardCharsets.UTF_8).trim();

            String[] lines = responseLine.split(CommandBuilder.PrintToFileOptions.CHANNEL_NAME_LATEST_CONTENT.getSplitRegex());
            channelName = lines[0].trim();
            String latestContentString = lines[1].trim();

            Files.deleteIfExists(tempFile);

            if (exitCode != 0 || channelName.isBlank()) {
                AlertUtils.showAlertAndAwait("Channel check failed", "Could not retrieve channel name.", Alert.AlertType.ERROR);
                return null;
            }
//            return SubscribedChannel.withBasicProperties(channelName, channelUrl);
            return SubscribedChannel.withLatestContent(channelName, channelUrl, latestContentString);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            AlertUtils.showAlertAndAwait("Error of channel check", e.getMessage(), Alert.AlertType.ERROR);
            return null;
        }
    }

    public Optional<FetchingResults> fetchUploadsAfter(SubscribedChannel channel, LocalDateTime dateAfter, Duration timeout) {
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
}