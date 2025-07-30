package pl.mewash.contentlaundry.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import pl.mewash.contentlaundry.commands.AudioOnlyQuality;
import pl.mewash.contentlaundry.commands.DownloadOption;
import pl.mewash.contentlaundry.commands.VideoQuality;
import pl.mewash.contentlaundry.models.channel.ChannelFetchRepo;
import pl.mewash.contentlaundry.models.channel.ChannelSettings;
import pl.mewash.contentlaundry.models.channel.enums.ChannelFetchParams;
import pl.mewash.contentlaundry.models.content.ContentDownloadStage;
import pl.mewash.contentlaundry.models.general.AdvancedOptions;
import pl.mewash.contentlaundry.models.general.GeneralSettings;
import pl.mewash.contentlaundry.models.general.enums.GroupingMode;
import pl.mewash.contentlaundry.models.general.enums.MultithreadingMode;
import pl.mewash.contentlaundry.models.ui.ChannelUiState;
import pl.mewash.contentlaundry.models.channel.enums.ChannelFetchingStage;
import pl.mewash.contentlaundry.models.content.FetchedContent;
import pl.mewash.contentlaundry.models.channel.enums.ChannelValidationStage;
import pl.mewash.contentlaundry.service.DownloadService;
import pl.mewash.contentlaundry.service.FetchService;
import pl.mewash.contentlaundry.service.SubscriptionsProcessor;
import pl.mewash.contentlaundry.models.channel.SubscribedChannel;
import pl.mewash.contentlaundry.subscriptions.SettingsManager;
import pl.mewash.contentlaundry.utils.AlertUtils;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SubscriptionsController {

    private static final AtomicInteger vThreadCounter = new AtomicInteger(1);
    private final ExecutorService virtualTasksExecutor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                    .name("VirT-", vThreadCounter.getAndIncrement())
                    .factory()
    );

    @FXML private TextField subsPathField;

    // add/check channel element
    @FXML private TextField channelUrlInput;
    @FXML public Button checkAddButton;
    private ChannelValidationStage channelValidationStage;

    // subscribed channels list
    private final ObservableList<ChannelUiState> channelsUiStates = FXCollections.observableArrayList();
    @FXML private ListView<ChannelUiState> channelListView; // or ObservableList<String>, depending on usage

    // fetched contents list
    private final ObservableList<FetchedContent> currentFetchedContents = FXCollections.observableArrayList();
    @FXML public ListView<FetchedContent> fetchedContentsListView;

    private GeneralSettings generalSettings = SettingsManager.load();

    private ChannelFetchRepo repository;
//    private final SubscriptionsService subscriptionsService = new SubscriptionsService();
    private final FetchService fetchService = new FetchService();

    @FXML
    protected void initialize() {
        repository = ChannelFetchRepo.getInstance();
        repository.load();
        channelValidationStage = ChannelValidationStage.NEW;
        channelListView.setItems(channelsUiStates);
        fetchedContentsListView.setItems(currentFetchedContents);

        if (generalSettings.subsLastSelectedPath != null && !generalSettings.subsLastSelectedPath.isBlank()) {
            subsPathField.setText(generalSettings.subsLastSelectedPath);
        }
        channelsUiStates.addAll(repository.loadChannelsUiList());
        loadChannelsListOnUi();

        // auto fetch implementation
        channelsUiStates.forEach(channelUiState -> {
            if (repository.getChannelSettings(channelUiState.getChannelName()).isAutoFetchLastestOnStartup()){
                submitFetchTask(channelUiState);
            }
        });
    }

    @FXML
    protected void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        File selectedDirectory = chooser.showDialog(null);
        if (selectedDirectory != null) {
            String selectedPath = selectedDirectory.getAbsolutePath();
            subsPathField.setText(selectedPath);

            generalSettings.subsLastSelectedPath = selectedPath;
            SettingsManager.saveSettings(generalSettings);
        }
    }

    private void submitFetchTask(ChannelUiState channelState) {
        virtualTasksExecutor.submit(() -> {

            ChannelFetchParams currentFetchParams = channelState.copyCurrentFetchParams();

            channelState.setFetchingStage(ChannelFetchingStage.FETCHING);
            Platform.runLater(channelListView::refresh);

            ChannelFetchParams resultStage = fetchService
                    .fetch(channelState.getChannelName(), currentFetchParams);

            channelState.setFetchingStage(resultStage.stage());
            channelState.setFetchOlderStage(resultStage.fetchOlder());

            Platform.runLater(() -> {
                channelListView.refresh();
                loadChannelsListOnUi();

                Optional<FetchedContent> currentView = fetchedContentsListView.getItems().stream().findAny();
                if (currentView.isPresent() && currentView.get().getChannelName().equals(channelState.getChannelName())) {
                    loadContentsView(channelState.getChannelName());
                }
            });
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
        Optional<ChannelSettings> settings = ChannelSettingsDialogLauncher
                .showDialogAndWait(ChannelSettings.defaultSettings());
        return settings.orElse(ChannelSettings.defaultSettings());
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
            SubscribedChannel newChannel = SubscriptionsProcessor.checkAndGetChannelName(channelUrl);
            updateAddChannelButtonState(ChannelValidationStage.VALIDATED);
            return newChannel;
        }
        return null;
    }

    private void setupAndAddChannel(SubscribedChannel newChannel) {
        if (newChannel != null && channelValidationStage == ChannelValidationStage.VALIDATED) {

            Platform.runLater(() -> {
                ChannelSettings channelSettings = showChannelSettingsSetupPopup();
                System.out.println("saving settings: " + channelSettings);
                newChannel.setChannelSettings(channelSettings);
                repository.addChannel(newChannel);

                ChannelUiState channelUiState = repository.getChannelUiState(newChannel.getChannelName());
                channelsUiStates.add(channelUiState);

                if (channelSettings.isFullFetch()){
                    channelUiState.setFetchingStage(ChannelFetchingStage.FETCH_OLDER);
                    channelUiState.setFetchOlderStage(ChannelFetchingStage.FetchOlderRange.LAST_25_YEARS);
                    submitFetchTask(channelUiState);

                } else if (channelSettings.isAutoFetchLastestOnStartup()){
                    channelUiState.setFetchingStage(ChannelFetchingStage.FIRST_FETCH);
                    submitFetchTask(channelUiState);
                }

                channelUrlInput.clear();
                channelListView.refresh();

                updateAddChannelButtonState(ChannelValidationStage.NEW);
            });

        }
    }

    private boolean checkIfAlreadySubscribedWithAlert(String channelUrl) {
        Optional<SubscribedChannel> existingChannel = repository.checkChannelExists(channelUrl);
        if (existingChannel.isPresent()) {
            String name = existingChannel.get().getChannelName();
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
            protected void updateItem(ChannelUiState channelState, boolean empty) {
                super.updateItem(channelState, empty);
                if (empty || channelState == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label title = new Label(channelState.getChannelName());
                    // Fetch Button
                    Button fetchButton = new Button(channelState.getFetchButtonTitle());
                    fetchButton.setDisable(channelState.getFetchingStage().isDisabled());
                    fetchButton.setOnAction(e -> submitFetchTask(channelState));
                    // Manage Button
                    Button manageButton = new Button("Manage");
                    manageButton.setOnAction(e -> handleManageChannelSettings(channelState));
                    // View Button
                    Button viewButton = new Button("View");
                    viewButton.setOnAction(e -> loadContentsView(channelState.getChannelName()));

                    HBox buttonsBox = new HBox(6, fetchButton, manageButton, viewButton);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    HBox titleBox = new HBox(10, title, spacer, buttonsBox);
                    titleBox.setAlignment(Pos.CENTER_LEFT);

                    setGraphic(titleBox);
                }
            }
        });
    }

    private void handleManageChannelSettings(ChannelUiState channelState) {
        SubscribedChannel subscribedChannel = repository.getChannel(channelState.getChannelName());

        ChannelSettings currentSettings = subscribedChannel.getChannelSettings();
        Optional<ChannelSettings> selected = ChannelSettingsDialogLauncher.showDialogAndWait(currentSettings);

        selected.ifPresent(newSettings -> {
            subscribedChannel.setChannelSettings(newSettings);
            repository.updateChannel(subscribedChannel);

            // FIXME: change if enable changing names
            // FIXME: causes
            int index = channelsUiStates.indexOf(channelState);
            if (index >= 0) {
                channelsUiStates.set(index, repository.getChannelUiState(subscribedChannel.getChannelName()));
            }
            Platform.runLater(() -> channelListView.refresh());

        });
    }

    private void loadContentsView(String channelName) {
        List<FetchedContent> fetchedContents = repository.getAllChannelContents(channelName);

        Platform.runLater(() -> {
            currentFetchedContents.clear();
            currentFetchedContents.addAll(fetchedContents);
            loadFetchedUploadsListOnUi();
        });

    }

    private void loadFetchedUploadsListOnUi() {
        fetchedContentsListView.setCellFactory(listView -> new ListCell<>() {

            @Override
            protected void updateItem(FetchedContent content, boolean empty) {
                super.updateItem(content, empty);
                if (empty || content == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label title = new Label(content.getDisplayTitle());

                    ContentDownloadStage audioStage = content.getAudioStage();
                    Button audioButton = new Button(audioStage.getAudioTitle());
                    audioButton.setDisable(audioStage.isDisabled());
                    if (audioStage == ContentDownloadStage.SAVED) audioButton
                            .setStyle("-fx-background-color: #cccccc;");
                    audioButton.setOnAction(e -> handleContentOptionButton(content,
                            repository.getChannelSettings(content.getChannelName()).getDefaultAudio()));

                    ContentDownloadStage videoStage = content.getVideoStage();
                    Button videoButton = new Button(videoStage.getVideoTitle());
                    videoButton.setDisable(videoStage.isDisabled());
                    if (videoStage == ContentDownloadStage.SAVED) videoButton
                            .setStyle("-fx-background-color: #cccccc;");
                    videoButton.setOnAction(e -> handleContentOptionButton(content,
                            repository.getChannelSettings(content.getChannelName()).getDefaultVideo()));

                    HBox hBox = new HBox(7, audioButton, videoButton, title);
                    hBox.setStyle("-fx-alignment: center-left;");
                    setGraphic(hBox);
                }
            }
        });
    }

    private void handleContentOptionButton(FetchedContent content, DownloadOption downloadOption) {
        if (content.isDownloaded(downloadOption)) {
            openInExplorer(content, downloadOption);
        } else {
            handleDownload(content, downloadOption);
        }
    }

    private void openInExplorer(FetchedContent content, DownloadOption downloadOption) {
        try {
            Path path = switch (downloadOption) {
                case VideoQuality vq -> Path.of(content.getVideoPath());
                case AudioOnlyQuality aq -> Path.of(content.getAudioPath());
                default -> throw new IllegalArgumentException("Unknown option");
            };

            if (Files.exists(path)) {
                Path toOpen = Files.isDirectory(path) ? path : path.getParent();
                Desktop.getDesktop().open(toOpen.toFile());
            } else {
                AlertUtils.showAlertAndAwait("File not found",
                        "The saved path does not exist:\n" + path,
                        Alert.AlertType.WARNING);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            AlertUtils.showAlertAndAwait("Open failed", ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleDownload(FetchedContent content, DownloadOption downloadOption) {
        String subsBasePath = subsPathField.getText().trim();
        if (subsBasePath.isEmpty()) {
            System.err.println("⚠ No download path selected!");
            AlertUtils.showAlertAndAwait("No download path selected", "Select a download path",
                    Alert.AlertType.WARNING);
//            outputLog.appendText(resources.getString("log.no_download_path") + "\n");
            return;
        }

        content.setDownloadingStage(downloadOption);
        Platform.runLater(fetchedContentsListView::refresh);

        virtualTasksExecutor.submit(() -> {
            DownloadService downloadService = new DownloadService();

            ChannelSettings channelSettings = repository.getChannelSettings(content.getChannelName());
            GroupingMode byFormatGrouping = channelSettings.isSeparateDirPerFormat()
                    ? GroupingMode.GROUP_BY_FORMAT
                    : GroupingMode.NO_GROUPING;
            AdvancedOptions advancedOptions = new AdvancedOptions(
                    false, byFormatGrouping, channelSettings.isAddDownloadDateDir(), MultithreadingMode.MEDIUM);

            try {
                Path channelBasePath = Paths.get(subsBasePath + File.separator + content.getChannelName());
                if (!Files.exists(channelBasePath)) {
                    Files.createDirectories(channelBasePath);
                }
                Path savedPath = downloadService
                        .downloadFetched(content, downloadOption, channelBasePath.toString(), advancedOptions);

                content.addAndSetDownloaded(downloadOption, savedPath);
                repository.updateContent(content);
                Platform.runLater(fetchedContentsListView::refresh);
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtils.showAlertAndAwait("Download error", e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }
}


