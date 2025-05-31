package pl.mewash.contentlaundry;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import pl.mewash.contentlaundry.models.AdvancedOptions;
import pl.mewash.contentlaundry.models.OutputStructure;
import pl.mewash.contentlaundry.utils.Formats;
import pl.mewash.contentlaundry.utils.UrlExtractor;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class MainController {

    @FXML private ResourceBundle resources;

    @FXML private TextArea urlInput;

    @FXML private TextField pathField;

    @FXML private CheckBox mp3CheckBox;
    @FXML private CheckBox wavCheckBox;
    @FXML private CheckBox mp4CheckBox;

    @FXML private RadioButton fileOnlyRadio;
    @FXML private RadioButton fileWithMetadataRadio;

    @FXML private ToggleGroup groupingToggleGroup;
    @FXML private RadioButton groupByFormatRadio;
    @FXML private RadioButton groupByContentRadio;
    @FXML private RadioButton noGroupingRadio;

    @FXML
    private CheckBox addDateCheckbox;

    @FXML
    private TextArea outputLog;

    @FXML
    public void initialize() {
        outputLog.setFont(Font.font("JetBrains Mono", 13));
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

        DownloadService service = new DownloadService();
        // logger injection
        service.setLogConsumer(this::appendLog);
        service.setResources(resources);


        String urls = urlInput.getText();

        String basePath = pathField.getText().trim();
        if (basePath.isEmpty()) {
            System.err.println("⚠️ No download path selected!");
            appendLog("log.no_download_path");
            return;
        }

        boolean mp3 = mp3CheckBox.isSelected();
        boolean wav = wavCheckBox.isSelected();
        boolean mp4 = mp4CheckBox.isSelected();

        AdvancedOptions advancedOptions = getAdvancedOptions();

        List<String> urlList = UrlExtractor.toUrlList(urls);

        new Thread(() -> {
            for (String url : urlList) {

                try {
                    if (mp3) service.download(url, Formats.MP3, basePath, advancedOptions);
                    if (wav) service.download(url, Formats.WAV, basePath, advancedOptions);
                    if (mp4) service.download(url, Formats.MP4, basePath, advancedOptions);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
}
