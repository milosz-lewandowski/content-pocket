module pl.mewash.batch {
    requires static lombok;
    requires javafx.controls;
    requires javafx.fxml;

    requires pl.mewash.commands;
    requires pl.mewash.common;


    opens pl.mewash.batch to javafx.fxml;
    exports pl.mewash.batch;
}