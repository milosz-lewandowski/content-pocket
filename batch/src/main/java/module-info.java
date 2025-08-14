import pl.mewash.common.spi.tabs.TabPlugin;

module pl.mewash.batch {
    requires static lombok;
    requires javafx.controls;
    requires javafx.fxml;

    requires pl.mewash.commands;
    requires pl.mewash.common;

    provides TabPlugin
        with pl.mewash.batch.api.BatchTabPlugin;

    opens pl.mewash.batch.ui to javafx.fxml;
    exports pl.mewash.batch.api;

}