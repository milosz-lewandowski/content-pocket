package pl.mewash.contentlaundry;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import pl.mewash.contentlaundry.config.UTF8Control;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class LaundryApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Locale.setDefault(Locale.US);
        ResourceBundle bundle = ResourceBundle.getBundle(
                "i18n.messages",
                Locale.getDefault(),
                new UTF8Control()
        );
        FXMLLoader fxmlLoader = new FXMLLoader(LaundryApplication.class.getResource("main-view.fxml"), bundle);
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 1024, 768);
        stage.setTitle(bundle.getString("app.name") + " - " + bundle.getString("app.slogan"));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}