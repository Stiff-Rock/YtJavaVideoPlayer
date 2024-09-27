module org.stiffrock {
    requires javafx.controls;
    requires javafx.fxml;

    opens org.stiffrock to javafx.fxml;
    exports org.stiffrock;
}
