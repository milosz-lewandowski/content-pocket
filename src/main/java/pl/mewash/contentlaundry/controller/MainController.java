package pl.mewash.contentlaundry.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import pl.mewash.contentlaundry.service.DownloadService;
import pl.mewash.contentlaundry.models.AdvancedOptions;
import pl.mewash.contentlaundry.utils.OutputStructure;
import pl.mewash.contentlaundry.utils.Formats;
import pl.mewash.contentlaundry.utils.InputUtils;

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

    // Language resources
    @FXML
    private ResourceBundle resources;

    // Input text window
    @FXML
    private TextArea urlInput;
    // Path directory selection
    @FXML
    private TextField pathField;

    // Format Selection
    @FXML
    private CheckBox mp3CheckBox;
    @FXML
    private CheckBox wavCheckBox;
    @FXML
    private CheckBox mp4CheckBox;

    // Metadata selection
    @FXML
    private RadioButton fileOnlyRadio;
    @FXML
    private RadioButton fileWithMetadataRadio;
    // Grouping Selection
    @FXML
    private ToggleGroup groupingToggleGroup;
    @FXML
    private RadioButton groupByFormatRadio;
    @FXML
    private RadioButton groupByContentRadio;
    @FXML
    private RadioButton noGroupingRadio;
    // Date checkbox
    @FXML
    private CheckBox addDateCheckbox;

    // Progress view elements
    @FXML
    private Label progressLabel;
    @FXML
    private ProgressBar progressBar;

    @FXML
    private TextArea outputLog;

    @FXML
    public void initialize() {
        outputLog.setFont(Font.font("JetBrains Mono", 13));
        fileOnlyRadio.setSelected(true);
        noGroupingRadio.setSelected(true);
        addDateCheckbox.setSelected(false);
//        appendLog("test.latin", "- test liter"); // test polskich liter w konsoli
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

        DownloadService service = new DownloadService();
        // logger injection
        service.setLogConsumer(this::appendLog);
//        service.setResources(resources);

        String basePath = pathField.getText().trim();
        if (basePath.isEmpty()) {
            System.err.println("⚠️ No download path selected!");
            appendLog("log.no_download_path");
            return;
        }

        EnumSet<Formats> selectedFormats = getSelectedFormats();

        AdvancedOptions advancedOptions = getAdvancedOptions();

        // For progress bar
//        Platform.runLater(() -> {
//            progressBar.setProgress(0.0);
//            progressBar.setVisible(true);
//            progressBar.setManaged(true); // 👈 helps if layout hides invisible items
//            progressLabel.setVisible(true);
//        });


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
        }).start();

        ScheduledExecutorService uiUpdater = Executors.newSingleThreadScheduledExecutor();
        uiUpdater.scheduleAtFixedRate(() -> {
            int completed = completedCount.get();
            Platform.runLater(() -> {
                progressLabel.setText("Completed: " + completed + " / " + totalDownloads + "  |  Failed: " + failedCount + " / " + totalDownloads);
//                progressBar.setProgress((double) completed / totalDownloads);
            });
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private static boolean getRemoveDuplicatesAlertDecision(int duplicatesCount, int allUrlsCount) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Duplicate URLs Detected");
        alert.setHeaderText(duplicatesCount + " out of " + allUrlsCount + "URLs are duplicates.");
        alert.setContentText("Do you want to remove duplicates before processing?");

        ButtonType removeButton = new ButtonType("Remove duplicates");
        ButtonType keepButton = new ButtonType("Duplicates are fine", ButtonBar.ButtonData.CANCEL_CLOSE);

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

    private void appendLog(String key, Object... params) {
        String pattern = resources.getString(key);
        String formatted = MessageFormat.format(pattern, params);
        outputLog.appendText(formatted + "\n");
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
}
