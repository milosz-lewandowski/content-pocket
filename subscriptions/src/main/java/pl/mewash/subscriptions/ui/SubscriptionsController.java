package pl.mewash.subscriptions.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.app.settings.GeneralSettings;
import pl.mewash.common.app.settings.SettingsManager;
import pl.mewash.subscriptions.internal.persistence.repo.SubscriptionsRepository;
import pl.mewash.subscriptions.internal.persistence.impl.SubscriptionsJsonRepo;
import pl.mewash.subscriptions.internal.domain.model.ChannelSettings;
import pl.mewash.subscriptions.internal.domain.model.SubscribedChannel;
import pl.mewash.subscriptions.internal.domain.dto.ChannelFetchParams;
import pl.mewash.subscriptions.internal.domain.state.ChannelFetchingStage;
import pl.mewash.subscriptions.internal.domain.state.ChannelValidationStage;
import pl.mewash.subscriptions.internal.domain.state.ContentDownloadStage;
import pl.mewash.subscriptions.internal.domain.model.FetchedContent;
import pl.mewash.subscriptions.internal.domain.state.ChannelUiState;
import pl.mewash.subscriptions.internal.service.ChannelService;
import pl.mewash.subscriptions.internal.service.ContentService;
import pl.mewash.subscriptions.internal.service.FetchService;
import pl.mewash.subscriptions.ui.components.ContentsSummaryBar;
import pl.mewash.subscriptions.ui.dialogs.DialogLauncher;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

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
    @FXML private ListView<ChannelUiState> channelListView;

    // fetched contents list
    @FXML private StackPane contentsBarPane;
    private final ObservableList<FetchedContent> currentFetchedContents = FXCollections.observableArrayList();
    @FXML public ListView<FetchedContent> fetchedContentsListView;

    private GeneralSettings generalSettings;

    private SubscriptionsRepository repository;
    private FetchService fetchService;
    private ChannelService channelService;
    private ContentService contentService;

    @FXML
    protected void initialize() {
        AppContext appContext = AppContext.getInstance();
        SubscriptionsJsonRepo.getInstance().load();

        repository = SubscriptionsJsonRepo.getInstance();

        fetchService = new FetchService(appContext);
        channelService = new ChannelService(appContext);
        contentService = new ContentService(appContext);

        generalSettings = SettingsManager.load();
        if (Objects.nonNull(generalSettings.subsLastSelectedPath) && !generalSettings.subsLastSelectedPath.isBlank()) {
            subsPathField.setText(generalSettings.subsLastSelectedPath);
        }

        channelValidationStage = ChannelValidationStage.ADD_NEW;
        channelListView.setItems(channelsUiStates);
        channelsUiStates.addAll(repository.loadChannelsUiList());
        loadChannelsListOnUi();

        loadFetchedUploadsListOnUi();
        fetchedContentsListView.setItems(currentFetchedContents);

        channelsUiStates.forEach(channelUiState -> {
            if (repository.getChannelSettings(channelUiState.getUrl()).isAutoFetchLastestOnStartup()) {
                submitFetchTask(channelUiState);
            }
        });
    }


    // --- Initial view buttons actions ---

    @FXML
    protected void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        String currentPath = subsPathField.getText().trim();
        File initialDir = new File(currentPath);
        if (initialDir.exists() && initialDir.isDirectory()) {
            chooser.setInitialDirectory(initialDir);
        }

        File selectedDirectory = chooser.showDialog(null);
        if (selectedDirectory != null) {
            String selectedPath = selectedDirectory.getAbsolutePath();
            subsPathField.setText(selectedPath);

            generalSettings.subsLastSelectedPath = selectedPath;
            SettingsManager.saveSettings(generalSettings);
        }
    }

    @FXML
    public void handleAddChannel() {
        getChannelUrlInputWithAlert().ifPresent(channelUrl -> {
            if (!checkIfAlreadySubscribedWithAlert(channelUrl)) {

                updateAddChannelButtonState(ChannelValidationStage.CHECKING);

                Optional<ChannelUiState> newChannelState = channelService.verifyAndAddChannel(channelUrl);
                newChannelState.ifPresent(channelState -> {
                    ChannelSettings channelSettings = channelState.getChannelSettings();

                    if (channelSettings.isFullFetch()) {
                        channelState.setFetchingStage(ChannelFetchingStage.FETCH_OLDER);
                        channelState.setFetchOlderStage(ChannelFetchingStage.FetchOlderRange.LAST_25_YEARS);
                        submitFetchTask(channelState);

                    } else if (channelSettings.isAutoFetchLastestOnStartup()) {
                        channelState.setFetchingStage(ChannelFetchingStage.FIRST_FETCH);
                        submitFetchTask(channelState);
                    }

                    channelsUiStates.add(channelState);
                    channelListView.refresh();
                });
            }

            channelUrlInput.clear();
            updateAddChannelButtonState(ChannelValidationStage.ADD_NEW);
        });
    }


    // --- Cell factory methods ---

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
                    Button fetchButton = ButtonFactory.getFetchButton(channelState);
                    fetchButton.setOnAction(e -> submitFetchTask(channelState));

                    // Manage Button
                    Button manageButton = new Button("Manage");
                    manageButton.setOnAction(e -> handleManageChannelSettings(channelState));

                    // View Button
                    Button viewButton = new Button("View");
                    viewButton.setOnAction(e -> handleViewContents(channelState));

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

                    Button audioButton = ButtonFactory.getAudioContentButton(content);
                    audioButton.setOnAction(e -> handleContentOptionButton(content,
                            repository.getChannelSettings(content.getChannelUrl()).getDefaultAudio()));

                    Button videoButton = ButtonFactory.getVideoContentButton(content);
                    videoButton.setOnAction(e -> handleContentOptionButton(content,
                            repository.getChannelSettings(content.getChannelUrl()).getDefaultVideo()));

                    Button copyButton = ButtonFactory.getCopyContentUrlButton(content.getUrl());

                    HBox hBox = new HBox(7, copyButton, audioButton, videoButton, title);
                    hBox.setStyle("-fx-alignment: center-left;");
                    setGraphic(hBox);
                }
            }
        });
    }


    // --- Cell buttons actions ---

    private void submitFetchTask(ChannelUiState channelState) {
        if (channelState.getFetchingStage() == ChannelFetchingStage.ALL_FETCHED
            && DialogLauncher.showFullFetchRetryAlert()) {
            return;
        }

        virtualTasksExecutor.submit(() -> {

            ChannelFetchParams currentFetchParams = channelState.copyCurrentFetchParams();

            channelState.setFetchingStage(ChannelFetchingStage.FETCHING);
            Platform.runLater(channelListView::refresh);

            ChannelFetchParams resultStage = fetchService.fetch(channelState.getUrl(), currentFetchParams);

            channelState.setFetchingStage(resultStage.stage());
            channelState.setFetchOlderStage(resultStage.fetchOlder());

            Platform.runLater(() -> {
                channelListView.refresh();

                Optional<FetchedContent> currentView = fetchedContentsListView.getItems().stream().findAny();
                if (currentView.isPresent() && currentView.get().getChannelUrl().equals(channelState.getUrl())) {
                    handleViewContents(channelState);
                }
            });
        });
    }

    private void handleViewContents(ChannelUiState channelState) {
        List<FetchedContent> fetchedContents = repository.getAllChannelContents(channelState.getUrl());
        SubscribedChannel subscribedChannel = repository.getChannel(channelState.getUrl());

        Predicate<ChannelFetchingStage> fetchedLatest = (stage) ->
            stage == ChannelFetchingStage.ALL_FETCHED || stage == ChannelFetchingStage.FETCH_OLDER;


        ContentsSummaryBar.Params barParams = ContentsSummaryBar.Params.builder()
            .channelName(channelState.getChannelName())
            .contentsCount(fetchedContents.size())
            .lastFetchDate(subscribedChannel.getLastFetched())
            .fetchedLatest(fetchedLatest.test(channelState.getFetchingStage()))
            .fetchedSince(subscribedChannel.getPreviousFetchOlderRangeDate())
            .fetchedOldest(subscribedChannel.isFetchedSinceOldest())
            .savedAudios((int) fetchedContents.stream()
                .filter(fc -> fc.getAudioStage() == ContentDownloadStage.SAVED).count())
            .savedVideos((int) fetchedContents.stream()
                .filter(fc -> fc.getVideoStage() == ContentDownloadStage.SAVED).count())
            .build();

        GridPane bar = new ContentsSummaryBar().buildGridPane(barParams);


        // make it stretch horizontally inside the StackPane
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.prefWidthProperty().bind(contentsBarPane.widthProperty());
        StackPane.setAlignment(bar, Pos.CENTER_LEFT);

        // replace the placeholder content
        contentsBarPane.getChildren().setAll(bar);

        currentFetchedContents.clear();
        currentFetchedContents.setAll(fetchedContents);
    }

    private void handleManageChannelSettings(ChannelUiState channelState) {
        SubscribedChannel subscribedChannel = repository.getChannel(channelState.getUrl());

        ChannelSettings currentSettings = subscribedChannel.getChannelSettings();
        Optional<ChannelSettings> selectedSettings = DialogLauncher.showChannelSettingsDialogAndWait(currentSettings);

        selectedSettings.ifPresent(newSettings -> {
            channelState.setChannelSettings(newSettings);
            repository.updateChannelSettingsFromState(channelState);

            Platform.runLater(channelListView::refresh);
        });
    }

    private void handleContentOptionButton(FetchedContent content, DownloadOption downloadOption) {
        if (content.isDownloaded(downloadOption)) {
            openInExplorer(content, downloadOption);
        } else {
            submitDownloadTask(content, downloadOption);
        }
    }

    private void submitDownloadTask(FetchedContent content, DownloadOption downloadOption) {
        getSubsBasePathWithAlert().ifPresent(subsBasePath -> {

            virtualTasksExecutor.submit(() -> {
                content.setDownloadingStage(downloadOption);
                Platform.runLater(fetchedContentsListView::refresh);

                contentService.downloadFetched(content, downloadOption, subsBasePath);
                Platform.runLater(fetchedContentsListView::refresh);
            });
        });
    }

    private void openInExplorer(FetchedContent content, DownloadOption downloadOption) {
        try {
            Path path = switch (downloadOption) {
                case VideoQuality vq -> Path.of(content.getVideoPath());
                case AudioOnlyQuality aq -> Path.of(content.getAudioPath());
            };

            if (Files.exists(path)) {
                Path toOpen = Files.isDirectory(path) ? path : path.getParent();
                Desktop.getDesktop().open(toOpen.toFile());
            } else {
                DialogLauncher.showAlertAndAwait("File not found",
                        "The saved path does not exist:\n" + path,
                        Alert.AlertType.WARNING);
            }
        } catch (Exception ex) {
            System.err.println("Open failed: " + ex.getMessage());
            AppContext.getInstance().getFileLogger().appendSingleLine(ex.getMessage());
            DialogLauncher.showAlertAndAwait("Open failed", ex.getMessage(), Alert.AlertType.ERROR);
        }
    }


    // --- Button factory ---

    private static class ButtonFactory {
        static Button getCopyContentUrlButton(String contentUrl) {
            Button copyButton = new Button("📋");

            Tooltip tooltip = new Tooltip("Copy URL");
            tooltip.setShowDelay(Duration.millis(300));
            tooltip.setHideDelay(Duration.millis(200));
            copyButton.setTooltip(tooltip);

            copyButton.setOnAction(e -> {
                ClipboardContent clip = new ClipboardContent();
                clip.putString(contentUrl);
                Clipboard.getSystemClipboard().setContent(clip);

                String originalText = copyButton.getText();
                copyButton.setText("✔ Copied");
                copyButton.setDisable(true);

                PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
                pause.setOnFinished(ev -> {
                    copyButton.setText(originalText);
                    copyButton.setDisable(false);
                });
                pause.play();
            });
            return copyButton;
        }

        static Button getVideoContentButton(FetchedContent content) {
            ContentDownloadStage videoStage = content.getVideoStage();
            Button videoButton = new Button(videoStage.getVideoTitle());
            videoButton.setDisable(videoStage.isDisabled());
            if (videoStage == ContentDownloadStage.SAVED) videoButton
                    .setStyle("-fx-background-color: #cccccc;");
            return videoButton;
        }

        static Button getAudioContentButton(FetchedContent content) {
            ContentDownloadStage audioStage = content.getAudioStage();
            Button audioButton = new Button(audioStage.getAudioTitle());
            audioButton.setDisable(audioStage.isDisabled());
            if (audioStage == ContentDownloadStage.SAVED) audioButton
                    .setStyle("-fx-background-color: #cccccc;");
            return audioButton;
        }

        static Button getFetchButton(ChannelUiState channelState) {
            Button fetchButton = new Button(channelState.getFetchButtonTitle());
            fetchButton.setDisable(channelState.getFetchingStage().isDisabled());
            if (channelState.getFetchingStage() == ChannelFetchingStage.ALL_FETCHED)
                fetchButton.setStyle("-fx-background-color: #cccccc;");
            return fetchButton;
        }
    }


    // --- Helper methods ---

    private Optional<String> getSubsBasePathWithAlert() {
        String path = subsPathField.getText().trim();
        if (path.isEmpty()) {
            DialogLauncher.showAlertAndAwait("No download path selected",
                "Select a download path", Alert.AlertType.INFORMATION);
            return Optional.empty();
        }
        return Optional.of(path);
    }

    private Optional<String> getChannelUrlInputWithAlert() {
        String channelUrl = channelUrlInput.getText().trim();
        if (channelUrl.isEmpty()) {
            DialogLauncher.showAlertAndAwait("Channel URL required",
                "Enter a channel URL.", Alert.AlertType.INFORMATION);
            return Optional.empty();
        }
        return Optional.of(channelUrl);
    }

    private boolean checkIfAlreadySubscribedWithAlert(String channelUrl) {
        Optional<SubscribedChannel> existingChannel = repository.findChannelIfUrlExists(channelUrl);
        if (existingChannel.isPresent()) {
            String name = existingChannel.get().getChannelName();
            DialogLauncher.showAlertAndAwait("Channel already subscribed",
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
}


