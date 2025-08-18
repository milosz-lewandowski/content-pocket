package pl.mewash.contentlaundry.app;

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
import pl.mewash.common.app.binaries.BinariesManager;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.logging.api.LoggersProvider;
import pl.mewash.common.spi.tabs.TabPlugin;
import pl.mewash.contentlaundry.ui.MainController;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class LaundryApplication extends Application {

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
            ResourceBundle commonResources = ResourceBundle
                .getBundle("pl.mewash.common.i18n.messages", Locale.getDefault());
            FXMLLoader fxmlLoader = new FXMLLoader(LaundryApplication.class
                .getResource("/pl/mewash/contentlaundry/main-view.fxml"), commonResources);

            // --- load controller ---
            fxmlLoader.setControllerFactory(type -> new MainController(tabPlugins));
            stage.setOnCloseRequest(event -> AppContext.getInstance().executeOnCloseHandlers());
            Parent root = fxmlLoader.load();

            // --- setup app window ---
            ResourceBundle appBundle = ResourceBundle
                .getBundle("pl.mewash.contentlaundry.i18n.messages", Locale.getDefault());
            stage.setTitle(appBundle.getString("app.name") + " - " + appBundle.getString("app.slogan"));
            Scene scene = new Scene(root, 1280, 800);
            stage.setScene(scene);

            stage.show();

        } catch (Exception e) {
            System.err.println("❌ App startup failed: " + e.getMessage());

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
        System.out.println("Starting Laundry Application");
        launch();
    }

    private boolean verifyBinariesAndInitContext(Stage stage) {
        AppContext appContext = AppContext.getInstance();
        BinariesManager binariesManager = new BinariesManager();

        BinariesManager.SupportedPlatforms platform = binariesManager.getPlatform();
        String confirmedBinariesLocation;

        confirmedBinariesLocation = binariesManager.resolveToolsAtDefaultLocations();
        if (confirmedBinariesLocation == null) {
            String givenLocation = showBinariesNotFoundAlert(stage);
            confirmedBinariesLocation = binariesManager.resolveToolsAtGivenLocation(givenLocation);
        }
        if (confirmedBinariesLocation != null) {
            appContext.init(platform, confirmedBinariesLocation);
            return true;
        } else return false;
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
            System.out.println("User chose to close the app.");
            return null;
        }
    }
}