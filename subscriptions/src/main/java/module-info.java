module pl.mewash.subscriptions {
    requires javafx.controls;
    requires javafx.fxml;

    requires pl.mewash.commands;
    requires static lombok; requires pl.mewash.common;
    requires java.desktop;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

//    requires org.controlsfx.controls;

    opens pl.mewash.subscriptions to javafx.fxml;
    exports pl.mewash.subscriptions;
}