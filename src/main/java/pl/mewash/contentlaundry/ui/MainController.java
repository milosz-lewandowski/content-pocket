package pl.mewash.contentlaundry.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Popup;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.spi.tabs.TabPlugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class MainController {

    @FXML private ResourceBundle resources;
    @FXML private Label creditsLabel;
    @FXML private TabPane tabs;

    private final Popup infoPopup = new Popup();
    private boolean popupShown = false;

    private final List<TabPlugin> tabPlugins;

    public MainController(List<TabPlugin> tabPlugins) {
        this.tabPlugins = tabPlugins;
    }

    @FXML
    public void initialize() {

        setupCreditsPopup();

        if (tabPlugins.isEmpty()) {
            Tab info = new Tab("No tabs detected");
            info.setClosable(false);
            info.setContent(new Label("No tab plugins found. Check configuration or module path."));
            tabs.getTabs().add(info);

        } else {
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

                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    AppContext.getInstance().getFileLogger()
                        .appendSingleLine(sw.toString());
                }
            });
        }
    }

    private void setupCreditsPopup() {
        Label popupContent = new Label(AppContext.getInstance().getResource.apply(ResourceBundle
            .getBundle("pl.mewash.contentlaundry.i18n.messages", Locale.getDefault()).getString("app.credits")));

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
