import pl.mewash.common.spi.tabs.TabPlugin;

module pl.mewash.subscriptions {
    requires static lombok;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

    requires pl.mewash.commands;
    requires pl.mewash.common;

    provides TabPlugin
        with pl.mewash.subscriptions.api.SubscriptionsTabPlugin;

    exports pl.mewash.subscriptions.api;

    opens pl.mewash.subscriptions.ui to javafx.fxml;
    opens pl.mewash.subscriptions.ui.components to javafx.fxml;
}