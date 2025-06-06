package pl.mewash.contentlaundry;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

public class LaundryApplication extends Application {

    @Override
    public void start(Stage stage) {
        try {
            Locale.setDefault(Locale.US);
            ResourceBundle bundle = ResourceBundle.getBundle(
                    "i18n.messages",
                    Locale.getDefault()
//                    , new UTF8Control()
            );
            FXMLLoader fxmlLoader = new FXMLLoader(LaundryApplication.class.getResource("/pl/mewash/contentlaundry/main-view.fxml"), bundle);
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 1024, 768);
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