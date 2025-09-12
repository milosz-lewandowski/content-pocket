package pl.mewash.contentpocket.app;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import pl.mewash.common.app.binaries.BinariesInstallation;
import pl.mewash.common.app.binaries.BinariesManager;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.logging.api.LoggersProvider;
import pl.mewash.common.spi.tabs.TabPlugin;
import pl.mewash.contentpocket.ui.MainController;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ContentPocketApplication extends Application {

    private final static String CURRENT_ISSUES_TEMPS_TODOS = """
        List of Issues - todos, temporary solutions and technical debts:
        1. Issue: Overwhelmed FileLogger used as a complete process consumer, but returning process output.
        - Reason: No need for process pipeline watchdogs and real-time analysis in nearest release.
            atm logs aggregation is quite nice and efficient with single scheduled to file flusher.
        - Todo: Extract process consumption from logger but remain periodical writing in logger responsibility.
            introduce process pipeline class with possibility to inject watchdogs and output analyzers.
        - Features:
            a. timeout watchdog detecting remote source disconnection on low stability connections,
            b. properly implement detecting full fetch (instead of current analysis of logs returned by logger
        
        2. Issue: In near future i would like to drop lombok entirely (in case of possible supply-chain-attacks after
            in case of possible long term pause in application maintaining and development
        - Reason: Until reaching more stable version of this app i am staying with lombok as it makes access management
            way faster and more explicit at first glance.
        
        3. Issue: introduce ffprobe
        - Reason: at this moment selection of source streams is just some fallbacks heuristic based on target
            codec/format requirements. While this is good enough for everyday watching in subscriptions tab,
            in my opinion is not enough for real archiving use cases.
        - Todo: store information about each content all possible codecs, introduce algorithm selecting best codecs for
            target format, ffprobe partial downloads to select optimal conversion parameters (avoid reconversion on
            no real quality gain)
        
        4. Issue: Introduce per binary installation setup, to enable having one on system path, other on in directory
        """;


    private Label splashLabel;
    private Image logo;

    public static void main(String[] args) {
        System.out.println("Starting Content Pocket Application");
        System.out.println(CURRENT_ISSUES_TEMPS_TODOS);
        launch();
    }

    @Override
    public void start(Stage stage) {
        Stage splashStage = showSplash();

        CompletableFuture.runAsync(() -> initAppStartup(stage, splashStage));
    }

    public void initAppStartup(Stage stage, Stage splashStage) {
        try {
            // --- verify binaries ---
            updateSplash("Verifying yt-dlp & FFmpeg binaries...");
            boolean binariesFound = verifyBinariesAndInitContext(stage);
            if (!binariesFound) {
                updateSplash("Failed to load binaries from the binaries folder. Closing application...");
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    splashStage.close();
                    Platform.exit();
                });
                return;
            }

            // --- load tabs ---
            updateSplash("Detecting and loading tabs...");
            List<TabPlugin> tabPlugins = ServiceLoader
                .load(TabPlugin.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator
                    .comparingInt(TabPlugin::positionOrder))
                .toList();

            String detectedTabsMessage = "Detected tabs: " + tabPlugins.stream()
                .map(TabPlugin::title)
                .collect(Collectors.joining(", "));
            updateSplash(detectedTabsMessage);

            // --- load main view ---
            updateSplash("Loading ContentPocket UI...");
            Platform.runLater(() -> {
                try {
                    // --- set resources  ---
                    Locale.setDefault(Locale.US);
                    ResourceBundle appBundle = ResourceBundle
                        .getBundle("i18n.messages", Locale.getDefault());
                    FXMLLoader fxmlLoader = new FXMLLoader(ContentPocketApplication.class
                        .getResource("/pl/mewash/contentpocket/main-view.fxml"), appBundle);

                    // --- load main controller with tabs ---
                    fxmlLoader.setControllerFactory(type -> new MainController(tabPlugins, appBundle));
                    stage.setOnCloseRequest(event -> AppContext.getInstance().executeOnCloseHandlers());
                    Parent root = fxmlLoader.load();

                    // --- setup app window ---
                    stage.setTitle(appBundle.getString("app.name") + " - " + appBundle.getString("app.slogan"));
                    Scene scene = new Scene(root, 1280, 800);
                    stage.setScene(scene);

                    stage.getIcons().add(loadLogo());

                    // --- close splash and load main window ---
                    splashStage.close();
                    stage.show();
                } catch (Exception e) {
                    showStartupError(e);
                }
            });
        } catch (Exception e) {
            showStartupError(e);
        }
    }

    private void showStartupError(Exception e) {
        System.err.println("✘ App startup failed: " + e.getMessage());
        updateSplash("Startup failed. Reason: " + e.getMessage() +
            "\n Check log file for details.");

        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        LoggersProvider.getFileLogger().appendSingleLine(sw.toString());

        Platform.runLater(() -> {
            PauseTransition delay = new PauseTransition(Duration.seconds(5));
            delay.setOnFinished(ev -> Platform.exit());
            delay.play();
        });
    }

    private Stage showSplash() {
        Stage splash = new Stage();
        splash.setTitle("ContentPocket - initializing...");

        ImageView logoView = new ImageView(loadLogo());
        logoView.setFitWidth(64);
        logoView.setPreserveRatio(true);

        splashLabel = new Label("Verifying Yt-dlp & FFmpeg binaries, creating app files...");
        splashLabel.setWrapText(true);
        splashLabel.setPadding(new Insets(10, 20, 20, 20));

        VBox content = new VBox(10, logoView, splashLabel);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.CENTER);

        Scene scene = new Scene(content, 500, 150);
        splash.setScene(scene);

        splash.initStyle(StageStyle.UTILITY);
        splash.show();

        return splash;
    }

    private void updateSplash(String message) {
        Platform.runLater(() -> splashLabel.setText(message));
    }

    private boolean verifyBinariesAndInitContext(Stage stage) {
        AppContext appContext = AppContext.getInstance();
        BinariesManager binariesManager = new BinariesManager();

        Optional<BinariesInstallation> confirmedInstallation = binariesManager
            .verifyBinariesDefaultInstallation();

        if (confirmedInstallation.isEmpty()) {
            String givenLocation = showBinariesNotFoundAlert(stage);
            confirmedInstallation = binariesManager
                .verifyBinariesAtUsersLocation(givenLocation);
        }

        if (confirmedInstallation.isPresent()) {
            appContext.init(confirmedInstallation.get());
            return true;
        }
        return false;
    }

    private static String showBinariesNotFoundAlert(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Missing Tools");
        alert.setHeaderText("Required binaries not found");
        alert.setContentText("""
            We could not find yt-dlp and ffmpeg.
            Please choose a folder that contains them, or close the application.""");

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
            System.out.println("Binaries not found and new path not specified - exiting application.");
            return null;
        }
    }

    private Image loadLogo() {
        if (logo != null) return logo;
        logo = new Image(
            Objects.requireNonNull(ContentPocketApplication.class
                .getResourceAsStream("/icons/app-icon.png"))
        );
        return logo;
    }
}