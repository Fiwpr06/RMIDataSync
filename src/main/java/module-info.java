module com.fiwpr06.rmidatasync {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires java.rmi;

    opens com.fiwpr06.rmidatasync.shared.model to javafx.base;
    opens com.fiwpr06.rmidatasync.server.database to javafx.base;

    exports com.fiwpr06.rmidatasync.shared.model;
    exports com.fiwpr06.rmidatasync.shared.rmi;
    exports com.fiwpr06.rmidatasync.shared.util;
    exports com.fiwpr06.rmidatasync.server.core;
    exports com.fiwpr06.rmidatasync.server.database;
}
