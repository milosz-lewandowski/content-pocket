import pl.mewash.common.spi.tabs.TabPlugin;

module pl.mewash.subscriptions {
    requires static lombok; // To removal in stable version

    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    requires pl.mewash.common;

    provides TabPlugin
        with pl.mewash.subscriptions.api.SubscriptionsTabPlugin;

    exports pl.mewash.subscriptions.api;

    opens pl.mewash.subscriptions.internal.domain.model to com.fasterxml.jackson.databind;
    opens pl.mewash.subscriptions.internal.domain.state to com.fasterxml.jackson.databind;
    opens pl.mewash.subscriptions.internal.persistence.config to com.fasterxml.jackson.databind;
    opens pl.mewash.subscriptions.internal.persistence.storage to com.fasterxml.jackson.databind;

    // opens needed for resources loading on plugged tab controller creation
    opens pl.mewash.subscriptions.ui;
    opens pl.mewash.subscriptions.ui.components;
}