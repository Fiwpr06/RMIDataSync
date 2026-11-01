module com.fiwpr06.rmidatasync {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.fiwpr06.rmidatasync to javafx.fxml;
    exports com.fiwpr06.rmidatasync;
}