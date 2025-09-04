import pl.mewash.common.spi.tabs.TabPlugin;

module pl.mewash.contentpocket {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    requires pl.mewash.common;

    // tabs dependencies are needed only for building modular zip with jlink
    requires pl.mewash.batch;
    requires pl.mewash.subscriptions;

    uses TabPlugin;

    opens pl.mewash.contentpocket.app to javafx.graphics;
    opens pl.mewash.contentpocket.ui to javafx.fxml;
}