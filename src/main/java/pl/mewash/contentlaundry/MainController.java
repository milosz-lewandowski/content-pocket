package pl.mewash.contentlaundry;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Popup;
import pl.mewash.common.tabs.spi.TabPlugin;

import java.net.URL;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.ServiceLoader;

public class MainController {

    @FXML private ResourceBundle resources;
    @FXML private Label creditsLabel;
    @FXML private TabPane tabs;

    private final Popup infoPopup = new Popup();
    private boolean popupShown = false;

    @FXML
    public void initialize() {

        setupCreditsPopup();

        var tabPlugins = ServiceLoader
            .load(TabPlugin.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .sorted(Comparator
                .comparingInt(TabPlugin::positionOrder))
            .toList();

        tabPlugins.forEach(tabProvider -> {
            try {
                URL url = tabProvider.getClass().getResource(tabProvider.fxmlPath());
                FXMLLoader fxmlLoader = new FXMLLoader(url, resources);
                Node content = fxmlLoader.load();

                Tab tab = new Tab(tabProvider.title(), content);
                tab.setClosable(false);
                tabs.getTabs().add(tab);
            } catch (Exception e) {
                System.err.println("Failed to load tab plugin " + tabProvider.id() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void setupCreditsPopup() {
        Label popupContent = new Label(resources.getString("app.credits"));
        popupContent.setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-padding: 10;");
        popupContent.setOnMouseClicked(e -> infoPopup.hide());

        infoPopup.getContent().add(popupContent);

        creditsLabel.setOnMouseClicked(event -> {
            if (popupShown) infoPopup.hide();
            else infoPopup.show(creditsLabel, event.getScreenX(), event.getScreenY() + 15);
            popupShown = !popupShown;
        });
    }
}
