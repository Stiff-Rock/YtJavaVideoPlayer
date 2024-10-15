package org.stiffrock;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        stage.setOnCloseRequest(event -> {
            event.consume();
            clearDirectory();
            Platform.exit();
        });

        stage.show();

        verifyOsCompatibility();
    }

    private void verifyOsCompatibility() {
        String os = System.getProperty("os.name").toLowerCase();

        String ytdlpPath = "";
        String ffmpegPath = "";
        if (os.contains("win")) {
            ytdlpPath = "bin/windows/yt-dlp.exe";
            ffmpegPath = "bin/windows/ffmpeg.exe";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            ytdlpPath = "bin/linux/yt-dlp";
            ffmpegPath = "bin/windows/ffmpeg";
        } else {
            System.exit(0);
        }

        VideoLoader.ytdlpPath = ytdlpPath;
        VideoLoader.ffmpegPath = ffmpegPath;
    }

    private void clearDirectory() {
        try {
            Path tempVideosPath = Paths.get("tempVideoFiles");
            Files.list(tempVideosPath).forEach(file -> {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    System.err.println("Error deleting file: " + file + " - " + e.getMessage());
                }
            });
            System.out.println("All contents deleted successfully.");
        } catch (IOException e) {
            System.err.println("Error clearing directory: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        launch();
    }
}