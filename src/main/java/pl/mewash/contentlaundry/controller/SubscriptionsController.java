package pl.mewash.contentlaundry.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import pl.mewash.contentlaundry.models.channel.ChannelSettings;
import pl.mewash.contentlaundry.models.general.AdvancedOptions;
import pl.mewash.contentlaundry.models.general.GeneralSettings;
import pl.mewash.contentlaundry.models.general.enums.Formats;
import pl.mewash.contentlaundry.models.general.enums.GroupingMode;
import pl.mewash.contentlaundry.models.general.enums.MultithreadingMode;
import pl.mewash.contentlaundry.models.ui.ChannelUiState;
import pl.mewash.contentlaundry.models.channel.enums.ChannelFetchingStage;
import pl.mewash.contentlaundry.models.content.FetchedUpload;
import pl.mewash.contentlaundry.models.channel.enums.ChannelValidationStage;
import pl.mewash.contentlaundry.service.DownloadService;
import pl.mewash.contentlaundry.models.content.FetchingResults;
import pl.mewash.contentlaundry.service.SubscriptionService;
import pl.mewash.contentlaundry.subscriptions.ChannelManager;
import pl.mewash.contentlaundry.models.channel.SubscribedChannel;
import pl.mewash.contentlaundry.subscriptions.SettingsManager;
import pl.mewash.contentlaundry.utils.AlertUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SubscriptionsController {

    @FXML private TextField subsPathField;

    // add/check channel element
    @FXML private TextField channelUrlInput;
    ChannelValidationStage channelValidationStage;
    @FXML public Button checkAddButton;

    // subscribed channels list
    private final ObservableList<ChannelUiState> channelsUiStates = FXCollections.observableArrayList();
    @FXML private ListView<ChannelUiState> channelListView; // or ObservableList<String>, depending on usage

    // fetched contents list
    private final ObservableList<FetchedUpload> currentFetchedUploads = FXCollections.observableArrayList();
    @FXML public ListView<FetchedUpload> fetchedUploadListView;

    private GeneralSettings generalSettings = SettingsManager.load();

    @FXML
    protected void initialize() {
        channelValidationStage = ChannelValidationStage.NEW;
        channelListView.setItems(channelsUiStates);

        if (generalSettings.lastSelectedPath != null && !generalSettings.lastSelectedPath.isBlank()) {
            subsPathField.setText(generalSettings.lastSelectedPath);
        }

        try {
            List<SubscribedChannel> loaded = ChannelManager.loadChannels();
            List<ChannelUiState> channelUiStates = loaded.stream()
                    .map(channel -> new ChannelUiState(channel, ChannelFetchingStage.TO_FETCH))
                    .toList(); // FIXME: consider delegating auto tasks
            channelsUiStates.addAll(channelUiStates);
        } catch (IOException e) {
            System.err.println("Failed to load subscribed channels: " + e.getMessage());
            e.printStackTrace();
        }
        loadChannelsListOnUi();
    }

    @FXML
    protected void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        File selectedDirectory = chooser.showDialog(null);
        if (selectedDirectory != null) {
            String selectedPath = selectedDirectory.getAbsolutePath();
            subsPathField.setText(selectedPath);

            generalSettings.lastSelectedPath = selectedPath;
            SettingsManager.saveSettings(generalSettings);
        }
    }

    private void handleFetchAsync(ChannelUiState channelState, Duration increasedTimeout) {
        channelState.setFetchingStage(ChannelFetchingStage.FETCHING);
        Platform.runLater(channelListView::refresh);

        SubscribedChannel channel = channelState.getSubscribedChannel();
        LocalDateTime fetchBase = Optional.ofNullable(channel.getLastFetched())
                .orElse(LocalDateTime.now());
        LocalDateTime dateAfter = fetchBase.minusDays(14); // FIXME: MockDate

        Duration currentTimeout = Optional.ofNullable(increasedTimeout)
                .orElse(channel.getChannelSettings().getTimeout());

        CompletableFuture.runAsync(() -> {
            Optional<FetchingResults> resultsOp = SubscriptionService
                    .fetchUploadsAfter(channel, dateAfter, currentTimeout);

            resultsOp.ifPresentOrElse(results -> {
                List<FetchedUpload> fetchedUploads = results.fetchedUploads();

                if (!results.completedBeforeTimeout()) {
                    long estimatedTimeout = results.estimatedTimeout();
                    boolean retry = AlertUtils.getFetchTimeoutAlertAnswer(
                            channel.getChannelName(), fetchedUploads, dateAfter,
                            currentTimeout.getSeconds(), estimatedTimeout
                    );
                    if (retry) {
                        handleFetchAsync(channelState, Duration.ofSeconds(estimatedTimeout));
                        return;
                    }
                }
                // Fetch completed - manipulate channel here
                channel.setLastFetched(LocalDateTime.now());

                Platform.runLater(() -> {
                    currentFetchedUploads.clear();
                    currentFetchedUploads.addAll(fetchedUploads);
                    fetchedUploadListView.setItems(currentFetchedUploads);
                    loadFetchedUploadsListOnUi();
                    channelState.setFetchingStage(ChannelFetchingStage.FETCHED);
                    channelListView.refresh();
                });

            }, () -> Platform.runLater(() -> {
                channelState.setFetchingStage(ChannelFetchingStage.TO_FETCH);
                channelListView.refresh();
            }));
        });
    }

    @FXML
    public void handleAddChannel() {
        CompletableFuture.runAsync(() -> {
            SubscribedChannel subscribedChannel = null;
            if (channelValidationStage == ChannelValidationStage.NEW) subscribedChannel = checkAndGetBasicChannelData();
            if (subscribedChannel == null) return;
            if (channelValidationStage == ChannelValidationStage.VALIDATED) setupAndAddChannel(subscribedChannel);
        });
    }

    private ChannelSettings showChannelSettingsSetupPopup() {
        //TODO: implement settings selection popup
        return ChannelSettings.defaultAudioSettings();
    }

    public SubscribedChannel checkAndGetBasicChannelData() {
        String channelUrl = channelUrlInput.getText().trim();
        if (channelUrl.isEmpty()) {
            AlertUtils.showAlertAndAwait("Input URL Required", "Please enter a channel URL.", Alert.AlertType.INFORMATION);
            return null;
        }

        if (checkIfAlreadySubscribedWithAlert(channelUrl)) return null;

        if (channelValidationStage.equals(ChannelValidationStage.NEW)) {
            updateAddChannelButtonState(ChannelValidationStage.CHECKING);
            SubscribedChannel newChannel = SubscriptionService.checkAndGetChannelName(channelUrl);
            updateAddChannelButtonState(ChannelValidationStage.VALIDATED);
            return newChannel;
        }
        return null;
    }

    private void setupAndAddChannel(SubscribedChannel newChannel) {
        if (newChannel != null && channelValidationStage == ChannelValidationStage.VALIDATED) {

            ChannelSettings channelSettings = showChannelSettingsSetupPopup();
            newChannel.setChannelSettings(channelSettings);

            Platform.runLater(() -> {
                channelsUiStates.add(new ChannelUiState(newChannel, ChannelFetchingStage.TO_FETCH));
                channelUrlInput.clear();
                channelListView.refresh();
                ChannelManager.saveChannels(channelsUiStates.stream().map(ChannelUiState::getSubscribedChannel).toList());
                updateAddChannelButtonState(ChannelValidationStage.NEW);
            });

        }
    }

    private boolean checkIfAlreadySubscribedWithAlert(String channelUrl) {
        Optional<SubscribedChannel> alreadySubscribed = channelsUiStates.stream()
                .map(ChannelUiState::getSubscribedChannel)
                .filter(channel -> channel.getUrl().equals(channelUrl))
                .findFirst();

        if (alreadySubscribed.isPresent()) {
            String name = alreadySubscribed.get().getChannelName();
            AlertUtils.showAlertAndAwait("Channel already subscribed",
                    "This channel is already saved with name: " + name,
                    Alert.AlertType.INFORMATION);
            Platform.runLater(() -> channelUrlInput.clear());
            return true;
        }
        return false;
    }

    private void updateAddChannelButtonState(ChannelValidationStage stage) {
        channelValidationStage = stage;
        Platform.runLater(() -> {
            checkAddButton.setText(stage.getButtonTitle());
            checkAddButton.setDisable(stage.isButtonDisabled());
        });
    }

    private void loadChannelsListOnUi() {
        channelListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ChannelUiState channel, boolean empty) {
                super.updateItem(channel, empty);
                if (empty || channel == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label title = new Label(channel.getSubscribedChannel().getChannelName());
                    // Fetch Button
                    Button fetchButton = new Button(channel.getFetchingStage().getButtonTitle());
                    fetchButton.setDisable(channel.getFetchingStage().isDisabled());
                    fetchButton.setOnAction(e -> handleFetchAsync(channel, null));
                    // Manage Button
                    Button manageButton = new Button("Manage");
//                    manageButton.setOnAction(e -> handleManageChannelWindow(channel));

                    HBox hBox = new HBox(10, title, fetchButton, manageButton);
                    hBox.setStyle("-fx-alignment: center-left;");
                    setGraphic(hBox);
                }
            }
        });
    }

    private void loadFetchedUploadsListOnUi() {
        fetchedUploadListView.setCellFactory(listView -> new ListCell<>() {

            @Override
            protected void updateItem(FetchedUpload upload, boolean empty) {
                super.updateItem(upload, empty);
                if (empty || upload == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label title = new Label(upload.getTitle());
                    Button getMP3Button = new Button("Get MP3");
                    getMP3Button.setOnAction(e -> handleGet(upload, Formats.MP3));

                    Button getMP4Button = new Button("Get MP4");
                    getMP4Button.setOnAction(e -> handleGet(upload, Formats.MP4));

                    HBox hBox = new HBox(7, getMP3Button, getMP4Button, title);
                    hBox.setStyle("-fx-alignment: center-left;");
                    setGraphic(hBox);
                }
            }
        });
    }

    private void handleGet(FetchedUpload upload, Formats format) {
        String subsBasePath = subsPathField.getText().trim();
        if (subsBasePath.isEmpty()) {
            System.err.println("⚠ No download path selected!");
            AlertUtils.showAlertAndAwait("No download path selected", "Select a download path",
                    Alert.AlertType.WARNING);
//            outputLog.appendText(resources.getString("log.no_download_path") + "\n");
            return;
        }

        DownloadService downloadService = new DownloadService();
        AdvancedOptions advancedOptions = new AdvancedOptions(
                false, GroupingMode.NO_GROUPING, true, MultithreadingMode.SINGLE);
        CompletableFuture.runAsync(() -> {
            try {
                Path channelBasePath = Paths.get(subsBasePath + File.separator + upload.getChannelName());
                if (!Files.exists(channelBasePath)) {
                    Files.createDirectories(channelBasePath);
                }
                downloadService.downloadFetched(upload, format, channelBasePath.toString(), advancedOptions);
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtils.showAlertAndAwait("Download error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });

    }

    // DEV METHODS

//    @FXML public Button checkToolsButton;
//
//    public void handleCheckTools() {
//        CompletableFuture.runAsync(() -> ProcessFactory.checkTool("ffmpeg"));
//        CompletableFuture.runAsync(() -> ProcessFactory.checkTool("ffprobe"));
//    }
}


