package pl.mewash.contentlaundry.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import pl.mewash.contentlaundry.models.content.FetchedUpload;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlertUtils {

    public static void showAlertAndAwait(String title, String content, Alert.AlertType alertType) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public static String showBinariesNotFoundAlert(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Missing Tools");
        alert.setHeaderText("Required binaries not found");
        alert.setContentText("We could not find yt-dlp, ffmpeg and ffprobe.\n\n" +
                "Please choose a folder that contains them, or close the application.");

        ButtonType choosePathBtn = new ButtonType("Select tools path");
        ButtonType closeAppBtn = new ButtonType("Close App", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(choosePathBtn, closeAppBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == choosePathBtn) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select directory containing yt-dlp, ffmpeg, ffprobe");

            File selectedDir = chooser.showDialog(stage);
            if (selectedDir != null) {
                return selectedDir.getAbsolutePath();
            } else {
                System.out.println("Directory selection canceled.");
                return null;
            }
        } else {
            System.out.println("User chose to close the app.");
            return null;
        }
    }

    public static boolean getFetchTimeoutAlertAnswer(String channelName, List<FetchedUpload> fetchedUploads,
                                                     LocalDateTime targetDate, long currentTimeout, long estimatedTimeout) {
        AtomicBoolean answer = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                LocalDateTime fetchedUntil = fetchedUploads.stream()
                        .map(FetchedUpload::getPublished)
                        .min(LocalDateTime::compareTo)
                        .orElseThrow();
                int fetchedCount = fetchedUploads.size();

                String contextText = new StringBuilder()
                        .append("Fetch process timed out after defined ").append(formatDurationHumanFriendly(currentTimeout))
                        .append(" before fetching all uploads since target date: ")
                        .append(targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)).append(".\n")
                        .append("Fetched ").append(fetchedCount).append(" uploads. ")
                        .append("Oldest fetch from ").append(fetchedUntil.format(DateTimeFormatter.ISO_LOCAL_DATE)).append(".\n")
                        .append("Estimated time to fetch all uploads since target date ")
                        .append(formatDurationHumanFriendly(estimatedTimeout))
                        .toString();

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Fetch Timeout of: " + channelName);
                alert.setHeaderText("Required estimated " + formatDurationHumanFriendly(estimatedTimeout) + " to fetch all uploads until target date.");
                alert.setContentText(contextText);

                ButtonType retryButton = new ButtonType("Retry with estimated timeout");
                ButtonType customButton = new ButtonType("(TODO) Set custom timeout / date / quantity");
                ButtonType skipButton = new ButtonType("Skip remaining uploads", ButtonBar.ButtonData.CANCEL_CLOSE);

                //TODO: custom fetch settings
                alert.getButtonTypes().setAll(retryButton, customButton, skipButton);

                Optional<ButtonType> result = alert.showAndWait();
                answer.set(result.isPresent() && result.get() == retryButton);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        System.out.println("anwser: " + answer.get());
        return answer.get();
    }

    private static String formatDurationHumanFriendly(long seconds) {
        Duration d = Duration.ofSeconds(seconds);
        long mins = d.toMinutes();
        long secs = d.minusMinutes(mins).getSeconds();
        if (mins > 0) return String.format("%d min %d sec", mins, secs);
        return String.format("%d sec", secs);
    }
}
