module org.stiffrock {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;
    requires jdk.jdi;

    opens org.stiffrock to javafx.fxml;
    exports org.stiffrock;
}
