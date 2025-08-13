module pl.mewash.subscriptions {
    requires static lombok;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

    requires pl.mewash.commands;
    requires pl.mewash.common;

    provides pl.mewash.common.tabs.spi.TabPlugin
        with pl.mewash.subscriptions.api.SubscriptionsTabPlugin;

    opens pl.mewash.subscriptions.ui to javafx.fxml;
    exports pl.mewash.subscriptions.api;
}