module org.stiffrock {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;

    opens org.stiffrock to javafx.fxml;
    exports org.stiffrock;
}
