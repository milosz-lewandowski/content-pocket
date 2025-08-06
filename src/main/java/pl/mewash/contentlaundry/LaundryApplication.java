package pl.mewash.contentlaundry;

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
import pl.mewash.batch.BatchController;
import pl.mewash.common.AppContext;
import pl.mewash.common.BinariesManager;

import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class LaundryApplication extends Application {

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

    @Override
    public void start(Stage stage) {
        try {
            boolean binariesFound = verifyBinariesAndInitContext(stage);
            if (!binariesFound) {
                System.out.println("Binaries are missing. Exiting application.");
                Platform.exit();
            }
            Locale.setDefault(Locale.US);
            ResourceBundle bundle = ResourceBundle.getBundle(
                    "i18n.messages",
                    Locale.getDefault()
            );
            FXMLLoader fxmlLoader = new FXMLLoader(LaundryApplication.class.getResource("/pl/mewash/contentlaundry/main-view.fxml"), bundle);
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 1280, 800);
            stage.setTitle(bundle.getString("app.name") + " - " + bundle.getString("app.slogan"));
            stage.setScene(scene);
            stage.setOnCloseRequest(event -> AppContext.getInstance().executeOnCloseHandlers());
            stage.show();
        } catch (Exception e) {
            System.err.println("❌ App startup failed: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Startup failed: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting Laundry Application");
        launch();
    }

    private static String showBinariesNotFoundAlert(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Missing Tools");
        alert.setHeaderText("Required binaries not found");
        alert.setContentText("""
                We could not find yt-dlp, ffmpeg and ffprobe.
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