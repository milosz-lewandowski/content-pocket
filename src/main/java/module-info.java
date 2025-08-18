import pl.mewash.common.spi.tabs.TabPlugin;

module pl.mewash.contentlaundry {
    requires static lombok;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    requires pl.mewash.common;

    uses TabPlugin;

    opens pl.mewash.contentlaundry.app to javafx.graphics;
    opens pl.mewash.contentlaundry.ui to javafx.fxml;
}