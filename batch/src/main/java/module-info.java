import pl.mewash.common.spi.tabs.TabPlugin;

module pl.mewash.batch {
    requires static lombok; // To removal in stable version

    requires javafx.controls;
    requires javafx.fxml;

    requires pl.mewash.common;

    provides TabPlugin
        with pl.mewash.batch.api.BatchTabPlugin;

    exports pl.mewash.batch.api;

    // opens needed for resources loading on plugged tab controller creation
    opens pl.mewash.batch.ui;
    opens pl.mewash.batch.i18n;
}