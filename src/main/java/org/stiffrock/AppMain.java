package org.stiffrock;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class AppMain extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        if (!verifyOsCompatibility()) {
            showIncompatibilityWarning();
            return;
        }

        VideoLoader.checkYtDlpUpdates();

        FXMLLoader fxmlLoader = new FXMLLoader(AppMain.class.getResource("appView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/icon.png"))));
        stage.setTitle("Yt Video Player");
        stage.show();
    }

    /**
     * Verifies if the current operating system is compatible and sets the yt-dlp path corresponding to it.
     * Currently it only suports windows and linux based systems.
     * @return true if compatible, false otherwise
     */
    private boolean verifyOsCompatibility() {
        String os = System.getProperty("os.name").toLowerCase();

        String ytdlpPath;
        if (os.contains("win")) {
            ytdlpPath = "bin/yt-dlp.exe";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            ytdlpPath = "bin/yt-dlp_linux";
        } else {
            return false;
        }

        VideoLoader.ytdlpPath = ytdlpPath;
        return true;
    }

    /**
     * Shows a warning popup if the OS is incompatible, then exits the application.
     */
    private void showIncompatibilityWarning() {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Incompatible OS");
        alert.setHeaderText(null);
        alert.setContentText("Your operating system is not supported. The application will now exit.");
        alert.showAndWait();
        Platform.exit(); // Close the app
    }

    public static void main(String[] args) {
        launch();
    }
}
