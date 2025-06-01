module pl.mewash.contentlaundry {
    requires javafx.controls;
    requires javafx.fxml;


    opens pl.mewash.contentlaundry to javafx.fxml;
    exports pl.mewash.contentlaundry;
    exports pl.mewash.contentlaundry.controller;
    opens pl.mewash.contentlaundry.controller to javafx.fxml;
    exports pl.mewash.contentlaundry.service;
    opens pl.mewash.contentlaundry.service to javafx.fxml;
}