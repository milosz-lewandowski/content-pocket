package pl.mewash.web;

import com.jpro.webapi.JProApplication;
import javafx.stage.Stage;
import pl.mewash.contentpocket.app.ContentPocketApplication;

public class ContentPocketJProLauncher extends JProApplication {
    @Override
    public void start(Stage stage) {
        new ContentPocketApplication().start(stage);
    }
}