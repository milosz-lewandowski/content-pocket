module pl.mewash.contentlaundry {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires static lombok;
    requires java.desktop;
    requires pl.mewash.commands;
    requires pl.mewash.batch;
    requires pl.mewash.common;


    opens pl.mewash.contentlaundry to javafx.fxml;
    exports pl.mewash.contentlaundry;
}