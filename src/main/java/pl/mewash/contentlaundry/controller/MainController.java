package pl.mewash.contentlaundry.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import pl.mewash.contentlaundry.models.AdvancedOptions;
import pl.mewash.contentlaundry.service.DownloadService;
import pl.mewash.contentlaundry.utils.Formats;
import pl.mewash.contentlaundry.utils.InputUtils;
import pl.mewash.contentlaundry.utils.OutputStructure;

import java.io.File;
import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MainController {

    private final StringBuilder logBuffer = new StringBuilder();
    private ScheduledExecutorService uiLoggerScheduledThread;
    protected boolean uiLoggerThreadStarted = false;

    // Language resources
    @FXML private ResourceBundle resources;

    // Input text window
    @FXML private TextArea urlInput;
    // Path directory selection
    @FXML private TextField pathField;

    // Format Selection
    @FXML private CheckBox mp3CheckBox;
    @FXML private CheckBox wavCheckBox;
    @FXML private CheckBox mp4CheckBox;

    // Metadata selection
    @FXML private RadioButton fileOnlyRadio;
    @FXML private RadioButton fileWithMetadataRadio;
    // Grouping Selection
    @FXML private ToggleGroup groupingToggleGroup;
    @FXML private RadioButton groupByFormatRadio;
    @FXML private RadioButton groupByContentRadio;
    @FXML private RadioButton noGroupingRadio;
    // Date dir checkbox
    @FXML private CheckBox addDateCheckbox;

    // Progress info elements
    @FXML private Label progressLabel;
    @FXML private TextArea outputLog;

    @FXML
    public void onClose() {
        if (uiLoggerScheduledThread != null && !uiLoggerScheduledThread.isShutdown()) {
            uiLoggerScheduledThread.shutdownNow();
            uiLoggerThreadStarted = false;
        }
    }

    @FXML
    public void initialize() {
        fileOnlyRadio.setSelected(true);
        noGroupingRadio.setSelected(true);
        addDateCheckbox.setSelected(false);
    }

    @FXML
    protected void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        File selectedDirectory = chooser.showDialog(null);
        if (selectedDirectory != null) {
            pathField.setText(selectedDirectory.getAbsolutePath());
        }
    }


    @FXML
    protected void handleDownload() {
        List<String> refinedUrlList = refineInputToUrlList(urlInput.getText());

        DownloadService service = new DownloadService();  // Service selection
        service.setLogConsumer(this::appendLog); // Logger injection

        String basePath = pathField.getText().trim();
        if (basePath.isEmpty()) {
            System.err.println("⚠️ No download path selected!");
            outputLog.appendText(resources.getString("log.no_download_path") + "\n");
            return;
        }

        EnumSet<Formats> selectedFormats = getSelectedFormats();

        AdvancedOptions advancedOptions = getAdvancedOptions();

        final int totalDownloads = refinedUrlList.size() * selectedFormats.size();
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        new Thread(() -> {
            for (String url : refinedUrlList) {
                for (Formats format : selectedFormats) {
                    try {
                        service.download(url, format, basePath, advancedOptions);
                        completedCount.incrementAndGet();
                    } catch (Exception e) {
                        System.err.printf("❌ Failed to download [%s] as [%s]%n", url, format);
                        e.printStackTrace();
                        failedCount.incrementAndGet();
                    }
                }
            }

            // ensure final message print and uiLogger shutdown
            appendToOutputLog("🎉 All downloads finished!");
            try {
                Thread.sleep(1100);
            } catch (InterruptedException ignored) {}
            uiLoggerScheduledThread.shutdown();
            uiLoggerThreadStarted = false;
        }).start();

        if (uiLoggerScheduledThread == null || uiLoggerScheduledThread.isShutdown()) {
            uiLoggerScheduledThread = Executors.newSingleThreadScheduledExecutor();
            UiLoggerJob uiTask = new UiLoggerJob(completedCount, failedCount, totalDownloads);
            uiLoggerScheduledThread.scheduleAtFixedRate(uiTask, 0, 500, TimeUnit.MILLISECONDS);
            uiLoggerThreadStarted = true;
        }
    }

    private static boolean getRemoveDuplicatesAlertDecision(int duplicatesCount, int allUrlsCount) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Duplicate URLs Detected");
        alert.setHeaderText(duplicatesCount + " out of " + allUrlsCount + " URLs are duplicates.");
        alert.setContentText("Do you want to skip duplicates processing?");

        ButtonType removeButton = new ButtonType("Skip duplicates");
        ButtonType keepButton = new ButtonType("Process with duplicates", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(removeButton, keepButton);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == removeButton;
    }

    private List<String> refineInputToUrlList(String initialInput) {
        List<String> initialUrlsList = InputUtils.toUrlList(initialInput);
        int duplicatesCount = InputUtils.getDetectedDuplicatesCount(initialUrlsList);
        boolean removeDuplicates = false;
        if (duplicatesCount > 0) {
            removeDuplicates = getRemoveDuplicatesAlertDecision(duplicatesCount, initialUrlsList.size());
        }
        return removeDuplicates
                ? InputUtils.removeDuplicates(initialUrlsList)
                : initialUrlsList;
    }

    private AdvancedOptions getAdvancedOptions() {
        OutputStructure outputStructure;
        if (noGroupingRadio.isSelected()) {
            outputStructure = OutputStructure.NO_GROUPING;
        } else if (groupByContentRadio.isSelected()) {
            outputStructure = OutputStructure.GROUP_BY_CONTENT;
        } else if (groupByFormatRadio.isSelected()) {
            outputStructure = OutputStructure.GROUP_BY_FORMAT;
        } else outputStructure = OutputStructure.GROUP_BY_FORMAT;

        boolean withMetadata = !fileOnlyRadio.isSelected();
        return new AdvancedOptions(
                withMetadata,
                outputStructure,
                addDateCheckbox.isSelected()
        );
    }

    private EnumSet<Formats> getSelectedFormats() {
        EnumSet<Formats> selectedFormats = EnumSet.noneOf(Formats.class);
        if (mp3CheckBox.isSelected()) selectedFormats.add(Formats.MP3);
        if (wavCheckBox.isSelected()) selectedFormats.add(Formats.WAV);
        if (mp4CheckBox.isSelected()) selectedFormats.add(Formats.MP4);
        return selectedFormats;
    }

    // Logging logic
    private void appendLog(String key, Object... params) {
        String pattern = resources.getString(key);
        String formatted = MessageFormat.format(pattern, params);
        appendToOutputLog(formatted);
    }

    private void appendToOutputLog(String message) {
        synchronized (logBuffer) {
            logBuffer.append(message).append("\n");
        }
    }

    class UiLoggerJob implements Runnable {
        private final AtomicInteger completedCount;
        private final AtomicInteger failedCount;
        private final int totalDownloads;

        public UiLoggerJob(AtomicInteger completedCount, AtomicInteger failedCount, int totalDownloads) {
            this.completedCount = completedCount;
            this.failedCount = failedCount;
            this.totalDownloads = totalDownloads;
        }

        @Override
        public void run() {
            int completed = completedCount.get();
            int failed = failedCount.get();

            String toAppend = getLogAndFlush();
            Platform.runLater(() -> {
                progressLabel.setText("Completed: " + completed + " | Failed: " + failed + " | Total: " + totalDownloads);
                if (!toAppend.isEmpty()) {
                    outputLog.appendText(toAppend);
                }
            });
        }

        private String getLogAndFlush() {
            synchronized (logBuffer) {
                String currentLog = logBuffer.toString();
                logBuffer.setLength(0);
                return currentLog;
            }
        }
    }
}
