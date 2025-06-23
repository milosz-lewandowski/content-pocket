package pl.mewash.contentlaundry.service;

import javafx.scene.control.Alert;
import pl.mewash.contentlaundry.models.content.FetchedUpload;
import pl.mewash.contentlaundry.models.channel.SubscribedChannel;
import pl.mewash.contentlaundry.utils.AlertUtils;
import pl.mewash.contentlaundry.utils.ProcessFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class SubscriptionService {

    public static SubscribedChannel checkAndGetChannelName(String channelUrl) {
        try {
            Path tempFile = Files.createTempFile("yt_channel", ".txt");

            ProcessBuilder checkChannelProcess = ProcessFactory.buildCheckChannelCommand(channelUrl, tempFile.toFile());
            checkChannelProcess.redirectOutput(tempFile.toFile());
            checkChannelProcess.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process process = checkChannelProcess.start();
            String channelName;

            int exitCode = process.waitFor();
            channelName = Files.readString(tempFile, StandardCharsets.UTF_8).trim();
            Files.deleteIfExists(tempFile);

            if (exitCode != 0 || channelName.isBlank()) {
                AlertUtils.showAlertAndAwait("Channel check failed", "Could not retrieve channel name.", Alert.AlertType.ERROR);
                return null;
            }
            return SubscribedChannel.withBasicProperties(channelName, channelUrl);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            AlertUtils.showAlertAndAwait("Error of channel check", e.getMessage(), Alert.AlertType.ERROR);
            return null;
        }
    }

    public static Optional<FetchingResults> fetchUploadsAfter(SubscribedChannel channel, LocalDateTime dateAfter, Duration timeout) {
        long currentTimeout = timeout.getSeconds();
        try {
            Path tempFile = Files.createTempFile("fetch_uploads_temp", ".txt");
            ProcessBuilder builder = ProcessFactory.buildFetchUploadListCommand(
                    channel.getUrl(), dateAfter, tempFile.toFile());

            // Redirect error output to console
            builder.redirectError(ProcessBuilder.Redirect.INHERIT);
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            Process process = builder.start();

            boolean finished = process.waitFor(timeout.getSeconds(), TimeUnit.SECONDS);
            List<String> lines = Files.readAllLines(tempFile, StandardCharsets.UTF_8);
            List<FetchedUpload> fetchedUploads = lines.stream()
                    .map(line -> fetchedUploadFromString(line, channel.getChannelName()))
                    .toList();
            Files.deleteIfExists(tempFile);

            if (!finished) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();

                long estimatedTimeout = calculateEstimatedTimeout(fetchedUploads, dateAfter, currentTimeout);
                return Optional.of(new FetchingResults(fetchedUploads, false, estimatedTimeout));
            }
            Files.deleteIfExists(tempFile);
            return Optional.of(new FetchingResults(fetchedUploads, true, 0));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            AlertUtils.showAlertAndAwait("Fetch Error", e.getMessage(), Alert.AlertType.ERROR);
            return Optional.empty();
        }
    }


    private static FetchedUpload fetchedUploadFromString(String line, String channelName) {
        String[] parts = line.split("\\|\\|\\|");

        String dateString = parts[0].trim();
        String title = parts[1].trim();
        String url = parts[2].trim();
        String id = parts[3].trim();

        LocalDate publishedDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDateTime publishedDateTime = LocalDateTime.of(publishedDate, LocalTime.MIN);

        return new FetchedUpload(
                title,
                url,
                publishedDateTime,
                id,
                channelName
        );
    }

    private static long calculateEstimatedTimeout(List<FetchedUpload> fetchedUploads,
                                                  LocalDateTime targetDate, long currentTimeout) {
        LocalDateTime fetchedUntil = fetchedUploads.stream()
                .map(FetchedUpload::getPublished)
                .min(LocalDateTime::compareTo)
                .orElseThrow();
        long targetDuration = Duration.between(targetDate, LocalDateTime.now()).getSeconds();
        long currentDuration = Duration.between(fetchedUntil, LocalDateTime.now()).getSeconds();
        double completedProportion = (double) currentDuration / targetDuration;
        double exactResult = currentTimeout / completedProportion;
        double estimated = exactResult * 1.25;
        return (long) estimated + 10;
    }

    //    public static List<FetchedUpload> fetchUploadsAfter(SubscribedChannel channel, LocalDateTime dateAfter) {
//        try {
//            Path tempFile = Files.createTempFile("fetch_uploads_temp", ".txt");
//            ProcessBuilder fetchUploadsProcess = ProcessFactory
//                    .buildFetchUploadListCommand(channel.getUrl(), dateAfter, tempFile.toFile());
//

    /// /            fetchUploadsProcess.redirectOutput(tempFile.toFile());
    /// /            fetchUploadsProcess.redirectError(ProcessBuilder.Redirect.DISCARD);
    /// /            fetchUploadsProcess.redirectErrorStream(true);
//            Process process = fetchUploadsProcess.start();
//
//
//            List<String> lines = Files.readAllLines(tempFile, StandardCharsets.UTF_8);
//            Files.deleteIfExists(tempFile);
//            if (!process.waitFor(20, TimeUnit.SECONDS)) AlertUtils.showAlertAndAwait("Fetching Error",
//                    "Error while fetching uploads. Exit code: X - Timeout" ,
//                    Alert.AlertType.ERROR);
//            process.destroyForcibly();
//            if (lines.isEmpty()) {
//                return Collections.emptyList();
//            }
//            return lines.stream()
//                    .map(SubscriptionService::fetchedUploadFromString)
//                    .toList();
//        } catch (Exception e){
//            e.printStackTrace();
//            AlertUtils.showAlertAndAwait("Fetching Error", e.getMessage(), Alert.AlertType.ERROR);
//        }
//        return null;
//    }
}