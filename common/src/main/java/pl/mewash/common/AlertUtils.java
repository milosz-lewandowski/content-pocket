package pl.mewash.common;

//import javafx.application.Platform;
//import javafx.scene.control.Alert;
//import javafx.scene.control.ButtonBar;
//import javafx.scene.control.ButtonType;
//import javafx.stage.DirectoryChooser;
//import javafx.stage.Stage;
//
//import java.io.File;
//import java.util.Optional;

//public class AlertUtils {

//    public static boolean getFetchTimeoutAlertAnswer(String channelName, List<FetchedContent> fetchedContents,
//                                                     LocalDateTime targetDate, long currentTimeout, long estimatedTimeout) {
//        AtomicBoolean answer = new AtomicBoolean(false);
//        CountDownLatch latch = new CountDownLatch(1);
//        Platform.runLater(() -> {
//            try {
//                LocalDateTime fetchedUntil = fetchedContents.stream()
//                        .map(FetchedContent::getPublished)
//                        .min(LocalDateTime::compareTo)
//                        .orElseThrow();
//                int fetchedCount = fetchedContents.size();
//
//                String contextText = new StringBuilder()
//                        .append("Fetch process timed out after defined ").append(formatDurationHumanFriendly(currentTimeout))
//                        .append(" before fetching all uploads since target date: ")
//                        .append(targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)).append(".\n")
//                        .append("Fetched ").append(fetchedCount).append(" uploads. ")
//                        .append("Oldest fetch from ").append(fetchedUntil.format(DateTimeFormatter.ISO_LOCAL_DATE)).append(".\n")
//                        .append("Estimated time to fetch all uploads since target date ")
//                        .append(formatDurationHumanFriendly(estimatedTimeout))
//                        .toString();
//
//                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//                alert.setTitle("Fetch Timeout of: " + channelName);
//                alert.setHeaderText("Required estimated " + formatDurationHumanFriendly(estimatedTimeout) + " to fetch all uploads until target date.");
//                alert.setContentText(contextText);
//
//                ButtonType retryButton = new ButtonType("Retry with estimated timeout");
//                ButtonType customButton = new ButtonType("(TODO) Set custom timeout / date / quantity");
//                ButtonType skipButton = new ButtonType("Skip remaining uploads", ButtonBar.ButtonData.CANCEL_CLOSE);
//
//                //TODO: custom fetch settings
//                alert.getButtonTypes().setAll(retryButton, customButton, skipButton);
//
//                Optional<ButtonType> result = alert.showAndWait();
//                answer.set(result.isPresent() && result.get() == retryButton);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                latch.countDown();
//            }
//        });
//        try {
//            latch.await();
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            e.printStackTrace();
//        }
//        System.out.println("anwser: " + answer.get());
//        return answer.get();
//    }
//
//    private static String formatDurationHumanFriendly(long seconds) {
//        Duration d = Duration.ofSeconds(seconds);
//        long mins = d.toMinutes();
//        long secs = d.minusMinutes(mins).getSeconds();
//        if (mins > 0) return String.format("%d min %d sec", mins, secs);
//        return String.format("%d sec", secs);
//    }
//}
