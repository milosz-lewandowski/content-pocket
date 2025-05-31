module pl.mewash.contentlaundry {
    requires javafx.controls;
    requires javafx.fxml;


    opens pl.mewash.contentlaundry to javafx.fxml;
    exports pl.mewash.contentlaundry;
}