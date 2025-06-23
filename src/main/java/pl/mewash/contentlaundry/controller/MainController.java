package pl.mewash.contentlaundry.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import pl.mewash.contentlaundry.models.general.AdvancedOptions;
import pl.mewash.contentlaundry.service.DownloadService;
import pl.mewash.contentlaundry.models.general.GeneralSettings;
import pl.mewash.contentlaundry.subscriptions.SettingsManager;
import pl.mewash.contentlaundry.models.general.enums.Formats;
import pl.mewash.contentlaundry.utils.InputUtils;
import pl.mewash.contentlaundry.models.general.enums.MultithreadingMode;
import pl.mewash.contentlaundry.models.general.enums.GroupingMode;

import java.io.File;
import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.*;
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

    // Allow multithreading checkbox
    @FXML private ToggleGroup multithreadingSelection;
    @FXML private RadioButton singleThreadRadio;
    @FXML private RadioButton lowThreadsRadio;
    @FXML private RadioButton mediumThreadsRadio;
    @FXML private RadioButton maximumThreadsRadio;

    // Main start/stop button & workers pool management
    @FXML private Button startStopButton;
    private boolean downloadRunning = false;
    private ExecutorService workersThreadPool;

    // Progress info elements
    @FXML private Label progressLabel;
    @FXML private TextArea outputLog;

    private GeneralSettings generalSettings = SettingsManager.load();


    @FXML
    public void initialize() {
        fileOnlyRadio.setSelected(true);
        noGroupingRadio.setSelected(true);
        addDateCheckbox.setSelected(false);
        singleThreadRadio.setSelected(true);

        if (generalSettings.lastSelectedPath != null && !generalSettings.lastSelectedPath.isBlank()) {
            pathField.setText(generalSettings.lastSelectedPath);
        }
    }

    @FXML
    protected void handleStartStopLaundry() {
        if (!downloadRunning) {
            startLaundry();
            startStopButton.setText("Stop Laundry");
            downloadRunning = true;
        } else {
            stopLaundry();
            startStopButton.setText("Start Laundry");
            downloadRunning = false;
        }
    }

    private void stopLaundry() {
        if (workersThreadPool != null && !workersThreadPool.isShutdown()) {
            workersThreadPool.shutdownNow();
            appendToOutputLog("❌ Laundry stopped by user.");
        }
    }


    @FXML
    public void onClose() {
        if (uiLoggerScheduledThread != null && !uiLoggerScheduledThread.isShutdown()) {
            uiLoggerScheduledThread.shutdownNow();
            uiLoggerThreadStarted = false;
        }

        if (workersThreadPool != null && !workersThreadPool.isShutdown()) {
            workersThreadPool.shutdownNow();
            downloadRunning = false;
        }
    }

    @FXML
    protected void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        File selectedDirectory = chooser.showDialog(null);
        if (selectedDirectory != null) {
            String selectedPath = selectedDirectory.getAbsolutePath();
            pathField.setText(selectedPath);

            generalSettings.lastSelectedPath = selectedPath;
            SettingsManager.saveSettings(generalSettings);
        }
    }


    @FXML
    protected void startLaundry() {
        List<String> refinedUrlList = refineInputToUrlList(urlInput.getText());

        DownloadService service = new DownloadService();  // Service selection
        service.setLogConsumer(this::appendLog); // Logger injection

        String basePath = pathField.getText().trim();
        if (basePath.isEmpty()) {
            System.err.println("⚠ No download path selected!");
            outputLog.appendText(resources.getString("log.no_download_path") + "\n");
            return;
        }

        EnumSet<Formats> selectedFormats = getSelectedFormats();
        AdvancedOptions advancedOptions = getAdvancedOptions();

        final int totalDownloads = refinedUrlList.size() * selectedFormats.size();
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        if (uiLoggerScheduledThread == null || uiLoggerScheduledThread.isShutdown()) {
            uiLoggerScheduledThread = Executors.newSingleThreadScheduledExecutor();
            UiLoggerJob uiTask = new UiLoggerJob(completedCount, failedCount, totalDownloads);
            uiLoggerScheduledThread.scheduleAtFixedRate(uiTask, 0, 500, TimeUnit.MILLISECONDS);
            uiLoggerThreadStarted = true;
        }

        workersThreadPool = getExecutorSetup();

        CompletableFuture.runAsync(() -> {
            try {
                List<Future<?>> tasksCompleted = new CopyOnWriteArrayList<>();
                for (String url : refinedUrlList) {
                    for (Formats format : selectedFormats) {
                        Future<?> task = workersThreadPool.submit(() -> {
                            try {
                                service.download(url, format, basePath, advancedOptions);
                                completedCount.incrementAndGet();
                            } catch (Exception e) {
                                System.err.printf("❌ Failed to download [%s] as [%s]%n", url, format);
                                appendToOutputLog("Failed to download [" + url + "]: " + e.getMessage());
                                e.printStackTrace();
                                failedCount.incrementAndGet();
                            }
                        });
                        tasksCompleted.add(task);
                    }
                }
                for (Future<?> task : tasksCompleted) {
                    try {
                        task.get();
                    } catch (CancellationException ignored) {
                        appendToOutputLog("Cancellation Exception");
                        System.err.println("Cancellation Exception");
                    }
                }
                appendToOutputLog("🎉 All downloads finished!");
            } catch (Exception e) {
                appendToOutputLog("❌ Unexpected error during download execution.");
                e.printStackTrace();
            } finally {
                if (workersThreadPool != null && !workersThreadPool.isShutdown()) {
                    workersThreadPool.shutdown();
                }
                downloadRunning = false;
                Platform.runLater(() -> startStopButton.setText("Start Laundry"));
            }
        });
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
        GroupingMode groupingMode;
        if (noGroupingRadio.isSelected()) {
            groupingMode = GroupingMode.NO_GROUPING;
        } else if (groupByContentRadio.isSelected()) {
            groupingMode = GroupingMode.GROUP_BY_CONTENT;
        } else if (groupByFormatRadio.isSelected()) {
            groupingMode = GroupingMode.GROUP_BY_FORMAT;
        } else groupingMode = GroupingMode.GROUP_BY_FORMAT;

        MultithreadingMode multithreadingMode;
        if (singleThreadRadio.isSelected()) {
            multithreadingMode = MultithreadingMode.SINGLE;
        } else if (lowThreadsRadio.isSelected()) {
            multithreadingMode = MultithreadingMode.LOW;
        } else if (mediumThreadsRadio.isSelected()) {
            multithreadingMode = MultithreadingMode.MEDIUM;
        } else if (maximumThreadsRadio.isSelected()) {
            multithreadingMode = MultithreadingMode.MAXIMUM;
        } else multithreadingMode = MultithreadingMode.SINGLE;

        boolean withMetadata = !fileOnlyRadio.isSelected();

        return new AdvancedOptions(
                withMetadata,
                groupingMode,
                addDateCheckbox.isSelected(),
                multithreadingMode
        );
    }

    private EnumSet<Formats> getSelectedFormats() {
        EnumSet<Formats> selectedFormats = EnumSet.noneOf(Formats.class);
        if (mp3CheckBox.isSelected()) selectedFormats.add(Formats.MP3);
        if (wavCheckBox.isSelected()) selectedFormats.add(Formats.WAV);
        if (mp4CheckBox.isSelected()) selectedFormats.add(Formats.MP4);
        return selectedFormats;
    }

    private ExecutorService getExecutorSetup(){
        int fixedThreads = getAdvancedOptions()
                .multithreadingMode()
                .calculateThreads();
        appendToOutputLog("Parallel worker threads: " + fixedThreads);
        return Executors.newFixedThreadPool(fixedThreads);
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
