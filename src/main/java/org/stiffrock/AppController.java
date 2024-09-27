package org.stiffrock;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AppController {
    private String ytdlpWindows = "lib/yt-dlp.exe";
    private String ytdlpLinux = "lib/yt-dlp_linux";

    private MediaPlayer mediaPlayer;

    @FXML
    private TextField tfUrl;
    @FXML
    private Button btnPlay;
    @FXML
    private MediaView mediaView;

    @FXML
    private void play() {
        String youtubeUrl = tfUrl.getText();
        String videoUrl = getDirectVideoUrl(youtubeUrl);
        if (videoUrl != null) {
            playVideo(videoUrl, mediaView);
            System.out.println("Reproduciendo video");
        } else {
            System.out.println("Error al obtener el URL del video.");
        }
    }

    private String getDirectVideoUrl(String youtubeUrl) {
        String ytdlpPath = "";

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            ytdlpPath = ytdlpWindows;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            ytdlpPath = ytdlpLinux;
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + os);
        }


        printVideoTitle(ytdlpPath, youtubeUrl);

        ProcessBuilder processBuilder = new ProcessBuilder(ytdlpPath, "-f", "best", "-g", youtubeUrl);
        StringBuilder videoUrl = new StringBuilder();

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                videoUrl.append(line).append("\n");
            }

            reader.close();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Error occurred while executing yt-dlp. Exit code: " + exitCode);
            }

            return videoUrl.toString().trim();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void printVideoTitle(String ytdlpPath, String youtubeUrl) {
        try {
            Process process = new ProcessBuilder(ytdlpPath, "--get-title", youtubeUrl).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            System.out.println("Loading: " + reader.readLine());
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void playVideo(String videoUrl, MediaView mediaView) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        Media media = new Media(videoUrl);
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);

        mediaPlayer.setOnError(() -> {
            System.out.println("Error: " + mediaPlayer.getError().getMessage());
        });

        mediaPlayer.play();
    }
}
