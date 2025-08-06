package pl.mewash.contentlaundry;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Popup;

import java.util.ResourceBundle;

public class MainController {

    @FXML private ResourceBundle resources;


    @FXML private Label creditsLabel; // fx:id="creditsLabel"

    private final Popup infoPopup = new Popup();
    private boolean popupShown = false;

    @FXML
    public void initialize() {
        setupCreditsPopup();
    }

    private void setupCreditsPopup() {
        Label popupContent = new Label(resources.getString("app.credits"));
        popupContent.setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-padding: 10;");
        popupContent.setOnMouseClicked(e -> infoPopup.hide()); // click to dismiss

//        infoPopup.setAutoHide(true); // ✅ hides when clicking outside
//        infoPopup.setHideOnEscape(true); // ✅ esc closes it

        infoPopup.getContent().add(popupContent);

        creditsLabel.setOnMouseClicked(event -> {
            if (popupShown) {
                infoPopup.hide();
            } else {
                infoPopup.show(creditsLabel, event.getScreenX(), event.getScreenY() + 15);
            }
            popupShown = !popupShown;
        });
    }
}
