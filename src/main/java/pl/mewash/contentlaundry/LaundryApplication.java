package pl.mewash.contentlaundry;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import pl.mewash.contentlaundry.controller.MainController;
import pl.mewash.contentlaundry.utils.BinariesManager;

import java.util.Locale;
import java.util.ResourceBundle;

public class LaundryApplication extends Application {

    @Override
    public void start(Stage stage) {
        try {

            // Check and set platform
            BinariesManager binariesManager = new BinariesManager();
            BinariesManager.SupportedPlatforms platform = binariesManager.getPlatform();

            // Check and set tools dir for session
            String binariesDir = binariesManager.resolveValidatedToolsDir(stage);
            if (binariesDir == null) {
                System.out.println("Binaries are missing. Exiting application.");
                Platform.exit();
                return;
            }
            AppContext.getInstance().init(platform, binariesDir);

            Locale.setDefault(Locale.US);
            ResourceBundle bundle = ResourceBundle.getBundle(
                    "i18n.messages",
                    Locale.getDefault()
            );
            FXMLLoader fxmlLoader = new FXMLLoader(LaundryApplication.class.getResource("/pl/mewash/contentlaundry/main-view.fxml"), bundle);
            Parent root = fxmlLoader.load();
            MainController controller = fxmlLoader.getController();
            controller.onClose();
            Scene scene = new Scene(root, 1280, 800);
            stage.setTitle(bundle.getString("app.name") + " - " + bundle.getString("app.slogan"));
            stage.setScene(scene);
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
}