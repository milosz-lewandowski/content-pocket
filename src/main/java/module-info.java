module pl.mewash.contentlaundry {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires static lombok;
    requires java.desktop;


    opens pl.mewash.contentlaundry to javafx.fxml;
    exports pl.mewash.contentlaundry;
    exports pl.mewash.contentlaundry.controller;
    opens pl.mewash.contentlaundry.controller to javafx.fxml;
    exports pl.mewash.contentlaundry.service;
    opens pl.mewash.contentlaundry.service to javafx.fxml;
    exports pl.mewash.contentlaundry.models.general.enums;
    opens pl.mewash.contentlaundry.models.general.enums to javafx.fxml;
    exports pl.mewash.contentlaundry.subscriptions;
    opens pl.mewash.contentlaundry.subscriptions to javafx.fxml;
    exports pl.mewash.contentlaundry.utils;
    opens pl.mewash.contentlaundry.utils to javafx.fxml;
    exports pl.mewash.contentlaundry.models.general;
    opens pl.mewash.contentlaundry.models.general to javafx.fxml;
    exports pl.mewash.contentlaundry.models.channel;
    opens pl.mewash.contentlaundry.models.channel to javafx.fxml;
    exports pl.mewash.contentlaundry.models.content;
    opens pl.mewash.contentlaundry.models.content to javafx.fxml;
    exports pl.mewash.contentlaundry.models.channel.enums;
    opens pl.mewash.contentlaundry.models.channel.enums to javafx.fxml;
    exports pl.mewash.contentlaundry.models.ui;
    opens pl.mewash.contentlaundry.models.ui to javafx.fxml;
    exports pl.mewash.contentlaundry.commands;
    opens pl.mewash.contentlaundry.commands to javafx.fxml;
}