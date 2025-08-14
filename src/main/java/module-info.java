import pl.mewash.common.spi.tabs.TabPlugin;

module pl.mewash.contentlaundry {
    requires static lombok;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    requires pl.mewash.common;

    uses TabPlugin;

    opens pl.mewash.contentlaundry to javafx.fxml;
    exports pl.mewash.contentlaundry;
}