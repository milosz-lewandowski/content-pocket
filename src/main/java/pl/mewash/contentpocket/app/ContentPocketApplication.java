package pl.mewash.contentpocket.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
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

    @Override
    public void start(Stage stage) {
        try {
            // --- load tabs ---
            List<TabPlugin> tabPlugins = ServiceLoader
                .load(TabPlugin.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator
                    .comparingInt(TabPlugin::positionOrder))
                .toList();

            // --- verify binaries ---
            boolean binariesFound = verifyBinariesAndInitContext(stage);
            if (!binariesFound) {
                System.out.println("Binaries are missing. Exiting application.");
                Platform.exit();
                return;
            }

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

            stage.show();

        } catch (Exception e) {
            System.err.println("✘ App startup failed: " + e.getMessage());

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            LoggersProvider.getFileLogger()
                .appendSingleLine(sw.toString());

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Startup failed, check log file for details.");
            alert.setContentText("Startup failed: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting Content Pocket Application");
        System.out.println(CURRENT_ISSUES_TEMPS_TODOS);
        launch();
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
}