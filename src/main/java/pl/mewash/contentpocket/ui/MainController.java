package pl.mewash.contentpocket.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Popup;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.logging.api.FileLogger;
import pl.mewash.common.spi.tabs.TabPlugin;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class MainController {

    private final ResourceBundle defaultAppBundle;
    private final List<TabPlugin> tabPlugins;

    @FXML private Label creditsLabel;
    @FXML private TabPane tabs;

    private final Popup infoPopup = new Popup();
    private boolean popupShown = false;

    public MainController(List<TabPlugin> tabPlugins, ResourceBundle defaultAppBundle) {
        this.tabPlugins = tabPlugins;
        this.defaultAppBundle = defaultAppBundle;
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
                    ResourceBundle resultBundle = tabProvider.resBundleLocation().isPresent()
                        ? ResourceBundle.getBundle(tabProvider.resBundleLocation().get(), Locale.getDefault())
                        : defaultAppBundle;
                    FXMLLoader fxmlLoader = new FXMLLoader(url, resultBundle);
                    Node content = fxmlLoader.load();

                    Tab tab = new Tab(tabProvider.title(), content);
                    tab.setClosable(false);
                    tabs.getTabs().add(tab);

                } catch (Exception e) {
                    FileLogger logger = AppContext.getInstance().getFileLogger();
                    logger.logErrWithMessage("Failed to load tab plugin " + tabProvider.id(), e, true);
                    logger.logErrStackTrace(e, true);
                }
            });
        }
    }

    private void setupCreditsPopup() {
        Label popupContent = new Label(AppContext.getInstance().getResource.apply(ResourceBundle
            .getBundle("i18n.messages", Locale.getDefault()).getString("app.credits")));

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
