package org.stiffrock;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class AppMain extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(AppMain.class.getResource("appView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/icon.png"))));
        stage.setTitle("Yt Video Player");
        stage.show();

        verifyOsCompatibility();
    }

    private void verifyOsCompatibility() {
        String os = System.getProperty("os.name").toLowerCase();

        String ytdlpPath;
        if (os.contains("win")) {
            ytdlpPath = "bin/yt-dlp.exe";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            ytdlpPath = "bin/yt-dlp_linux";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + os);
        }

        VideoLoader.ytdlpPath = ytdlpPath;
    }

    public static void main(String[] args) {
        launch();
    }
}