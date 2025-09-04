package pl.mewash.batch.ui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import pl.mewash.batch.internals.models.BatchJobParams;
import pl.mewash.batch.internals.models.BatchProcessingState;
import pl.mewash.batch.internals.models.MultithreadingMode;
import pl.mewash.batch.internals.service.BatchProcessor;
import pl.mewash.batch.internals.utils.Dialogs;
import pl.mewash.batch.internals.utils.InputUtils;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.commands.settings.storage.AdditionalFiles;
import pl.mewash.commands.settings.storage.GroupingMode;
import pl.mewash.commands.settings.storage.StorageOptions;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.app.lifecycle.OnCloseHandler;
import pl.mewash.common.app.settings.GeneralSettings;
import pl.mewash.common.app.settings.SettingsManager;
import pl.mewash.common.downloads.api.DownloadService;
import pl.mewash.common.downloads.api.DownloadServiceProvider;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchController implements OnCloseHandler {

    // Language resources
    @FXML private ResourceBundle resources;

    // URLs input text window
    @FXML private TextArea urlInput;

    // Path directory selection
    @FXML private TextField pathField;

    // Audio download options
    @FXML private CheckBox m4aHqCheckbox;
    @FXML private CheckBox m4aCompressedCheckbox;
    @FXML private CheckBox mp3HqCheckbox;
    @FXML private CheckBox wavNewCheckbox;
    @FXML private CheckBox nativeCheckbox;

    // Video download options
    @FXML private CheckBox maximumMP4checkbox;
    @FXML private CheckBox highMP4checkbox;
    @FXML private CheckBox standardMP4checkbox;
    @FXML private CheckBox compactMP4checkbox;

    // Metadata files selection
    @FXML private RadioButton fileOnlyRadio;
    @FXML private RadioButton fileWithDescRadio;
    @FXML private RadioButton fileWithMetadataRadio;

    // Grouping Selection
    @FXML private RadioButton groupByFormatRadio;
    @FXML private RadioButton groupByContentRadio;
    @FXML private RadioButton noGroupingRadio;

    // Date dir checkbox
    @FXML private CheckBox addDateCheckbox;

    // Multithreading level selection
    @FXML private RadioButton singleThreadRadio;
    @FXML private RadioButton lowThreadsRadio;
    @FXML private RadioButton mediumThreadsRadio;
    @FXML private RadioButton maximumThreadsRadio;

    // Progress info elements
    @FXML private Label progressLabel;
    @FXML private TextArea outputLog;

    // Batch button and state handling
    @FXML private Button batchButton;
    private final ObjectProperty<BatchProcessingState>
        processingState = new SimpleObjectProperty<>(BatchProcessingState.NOT_RUNNING);

    // Scheduled logger
    private final StringBuilder logBuffer = new StringBuilder();
    private ScheduledExecutorService uiLoggerScheduledThread;
    protected boolean uiLoggerThreadStarted = false;

    private GeneralSettings generalSettings;

    private BatchProcessor batchProcessor;

    @FXML
    protected void initialize() {
        if (resources == null) resources = ResourceBundle
            .getBundle("pl.mewash.batch.i18n.messages", Locale.US);

        AppContext appContext = AppContext.getInstance();
        appContext.registerOnCloseHandler(this);
        generalSettings = SettingsManager.load();

        DownloadService downloadService = DownloadServiceProvider
            .getDefaultDownloadService(appContext);

        batchProcessor = new BatchProcessor(downloadService, this::logResMessageToUi);

        batchProcessor.injectUpdateButtonAction((state) -> {
            if (Platform.isFxApplicationThread()) processingState.set(state);
            else Platform.runLater(() -> processingState.set(state));
        });

        batchButton.textProperty().bind(Bindings
            .createStringBinding(() -> processingState.get().getButtonTitle(), processingState));
        batchButton.disableProperty().bind(Bindings
            .createBooleanBinding(() -> processingState.get().isButtonDisabled(), processingState));

        fileOnlyRadio.setSelected(true);
        groupByContentRadio.setSelected(true);
        addDateCheckbox.setSelected(false);
        mediumThreadsRadio.setSelected(true);

        String lastSelectedPath = generalSettings.getBatchLastSelectedPath();
        if (lastSelectedPath != null && !lastSelectedPath.isBlank()) pathField
            .setText(lastSelectedPath);
    }

    @FXML
    public void onClose() {
        if (uiLoggerScheduledThread != null && !uiLoggerScheduledThread.isShutdown()) {
            uiLoggerScheduledThread.shutdownNow();
            uiLoggerThreadStarted = false;
        }
        if (batchProcessor.getProcessingState() == BatchProcessingState.PROCESSING
            || batchProcessor.getProcessingState() == BatchProcessingState.IN_GRACEFUL_SHUTDOWN) {
            batchProcessor.forceShutdown();
        }
    }

    @FXML
    protected void handleBatchButtonActions() {
        switch (batchProcessor.getProcessingState()) {
            case NOT_RUNNING -> startBatchProcessing();
            case PROCESSING -> batchProcessor.gracefulShutdownAsync();
            case IN_GRACEFUL_SHUTDOWN -> batchProcessor.forceShutdown();
            case IN_FORCED_SHUTDOWN -> throw new IllegalStateException("Button should be not clickable while forcing");
        }
    }

    @FXML
    protected void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();

        String currentPath = pathField.getText().trim();
        File initialDir = new File(currentPath);
        if (initialDir.exists() && initialDir.isDirectory()) chooser
            .setInitialDirectory(initialDir);


        File selectedDirectory = chooser.showDialog(null);
        if (selectedDirectory != null) {
            String selectedPath = selectedDirectory.getAbsolutePath();
            pathField.setText(selectedPath);

            generalSettings.setBatchLastSelectedPath(selectedPath);
            SettingsManager.saveSettings(generalSettings);
        }
    }

    @FXML
    protected void startBatchProcessing() {
        Optional<String> basePath = getBasePathWithEmptyCheck();
        if (basePath.isEmpty()) return;

        Optional<String> urlInput = getUrlInputWithEmptyCheck();
        if (urlInput.isEmpty()) return;

        List<String> refinedUrlList = refineInputToUrlListWithDuplicatesCheck(urlInput.get());

        CompletableFuture.runAsync(() -> {

            Set<DownloadOption> downloadOptions = getSelectedDownloadOptions();
            StorageOptions storageOptions = getStorageOptions(downloadOptions);

            BatchJobParams jobParams = BatchJobParams.builder()
                .basePath(basePath.get())
                .urlList(refinedUrlList)
                .selectedDownloadOptions(getSelectedDownloadOptions())
                .storageOptions(storageOptions)
                .multithreadingMode(getMultithreadingMode())
                .build();

            int totalDownloads = jobParams.calculateTotalDownloads();
            setupAndGetUiLoggerExecutor(jobParams.getCompletedCount(), jobParams.getFailedCount(), totalDownloads);

            batchProcessor.processBatch(jobParams);
        });
    }

    // --- Preprocess input ---

    private List<String> refineInputToUrlListWithDuplicatesCheck(String initialInput) {
        List<String> initialUrlsList = InputUtils.toUrlList(initialInput);
        int duplicatesCount = InputUtils.getDetectedDuplicatesCount(initialUrlsList);
        boolean removeDuplicates = false;
        if (duplicatesCount > 0) removeDuplicates = Dialogs
            .getRemoveDuplicatesAlertDecision(duplicatesCount, initialUrlsList.size());

        return removeDuplicates
            ? InputUtils.removeDuplicates(initialUrlsList)
            : initialUrlsList;
    }

    // --- Gather download settings from selected options ---

    private Set<DownloadOption> getSelectedDownloadOptions() {
        Set<DownloadOption> selectedOptions = new HashSet<>();
        selectedOptions.addAll(getSelectedAudios());
        selectedOptions.addAll(getSelectedVideoQuality());
        return selectedOptions;
    }

    private EnumSet<AudioOnlyQuality> getSelectedAudios() {
        EnumSet<AudioOnlyQuality> selectedAudios = EnumSet.noneOf(AudioOnlyQuality.class);
        if (mp3HqCheckbox.isSelected()) selectedAudios.add(AudioOnlyQuality.MP3);
        if (wavNewCheckbox.isSelected()) selectedAudios.add(AudioOnlyQuality.WAV);
        if (nativeCheckbox.isSelected()) selectedAudios.add(AudioOnlyQuality.ORIGINAL_SOURCE);
        if (m4aCompressedCheckbox.isSelected()) selectedAudios.add(AudioOnlyQuality.M4A_SMALL_SIZE);
        if (m4aHqCheckbox.isSelected()) selectedAudios.add(AudioOnlyQuality.M4A);
        return selectedAudios;
    }

    private EnumSet<VideoQuality> getSelectedVideoQuality() {
        EnumSet<VideoQuality> videoQualitySet = EnumSet.noneOf(VideoQuality.class);
        if (maximumMP4checkbox.isSelected()) videoQualitySet.add(VideoQuality.MAXIMUM);
        if (highMP4checkbox.isSelected()) videoQualitySet.add(VideoQuality.HIGH);
        if (standardMP4checkbox.isSelected()) videoQualitySet.add(VideoQuality.STANDARD);
        if (compactMP4checkbox.isSelected()) videoQualitySet.add(VideoQuality.COMPACT);
        return videoQualitySet;
    }

    private GroupingMode getGroupingMode() {
        GroupingMode groupingMode;
        if (noGroupingRadio.isSelected()) groupingMode = GroupingMode.NO_GROUPING;
        else if (groupByContentRadio.isSelected()) groupingMode = GroupingMode.GROUP_BY_CONTENT;
        else if (groupByFormatRadio.isSelected()) groupingMode = GroupingMode.GROUP_BY_FORMAT;
        else groupingMode = GroupingMode.GROUP_BY_CONTENT;
        return groupingMode;
    }

    private AdditionalFiles getAdditionalFiles() {
        AdditionalFiles additionalFiles;
        if (fileOnlyRadio.isSelected()) additionalFiles = AdditionalFiles.MEDIA_ONLY;
        else if (fileWithDescRadio.isSelected()) additionalFiles = AdditionalFiles.MEDIA_WITH_DESCRIPTION;
        else if (fileWithMetadataRadio.isSelected()) additionalFiles = AdditionalFiles.MEDIA_WITH_METADATA;
        else additionalFiles = AdditionalFiles.MEDIA_ONLY;
        return additionalFiles;
    }

    private StorageOptions getStorageOptions(Set<DownloadOption> downloadOptions) {
        return StorageOptions
            .withConflictsTest(
                getAdditionalFiles(),
                getGroupingMode(),
                addDateCheckbox.isSelected(),
                downloadOptions
            );
    }

    private MultithreadingMode getMultithreadingMode() {
        MultithreadingMode multithreadingMode;
        if (singleThreadRadio.isSelected()) multithreadingMode = MultithreadingMode.SINGLE;
        else if (lowThreadsRadio.isSelected()) multithreadingMode = MultithreadingMode.LOW;
        else if (mediumThreadsRadio.isSelected()) multithreadingMode = MultithreadingMode.MEDIUM;
        else if (maximumThreadsRadio.isSelected()) multithreadingMode = MultithreadingMode.MAXIMUM;
        else multithreadingMode = MultithreadingMode.MEDIUM;
        return multithreadingMode;
    }

    // --- Ui Logger setup ---

    private void setupAndGetUiLoggerExecutor(AtomicInteger completedCount, AtomicInteger failedCount, int totalDownloads) {
        if (uiLoggerScheduledThread != null && !uiLoggerScheduledThread.isShutdown())
            uiLoggerScheduledThread.shutdownNow();

        uiLoggerScheduledThread = Executors.newSingleThreadScheduledExecutor();
        UiLoggerJob uiTask = new UiLoggerJob(completedCount, failedCount, totalDownloads);
        uiLoggerScheduledThread.scheduleAtFixedRate(uiTask, 0, 500, TimeUnit.MILLISECONDS);
        uiLoggerThreadStarted = true;
    }

    class UiLoggerJob implements Runnable {
        private final AtomicInteger completedCount;
        private final AtomicInteger failedCount;
        private final int totalDownloads;

        private UiLoggerJob(AtomicInteger completedCount, AtomicInteger failedCount, int totalDownloads) {
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
                if (!toAppend.isEmpty()) outputLog.appendText(toAppend);
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

    // --- Log methods ---

    private void logResMessageToUi(String key, Object... params) {
        String pattern = resources.getString(key);
        String formatted = MessageFormat.format(pattern, params);
        logStringToUi(formatted);
    }

    private void logStringToUi(String message) {
        synchronized (logBuffer) {
            logBuffer.append(message).append("\n");
        }
    }

    // --- Text fields helper methods ---

    private Optional<String> getBasePathWithEmptyCheck() {
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            Dialogs.showNoDownloadPathAlert();
            return Optional.empty();
        }
        return Optional.of(path);
    }

    private Optional<String> getUrlInputWithEmptyCheck() {
        String inputString = urlInput.getText().trim();
        if (inputString.isEmpty()) {
            Dialogs.showNoInputAlert();
            return Optional.empty();
        }
        return Optional.of(inputString);
    }
}
