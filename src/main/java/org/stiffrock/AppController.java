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
            System.out.println("Reproduciendo: " + videoUrl);
        } else {
            System.out.println("Error al obtener el URL del video.");
        }
    }

    private String getDirectVideoUrl(String youtubeUrl) {
        String command = "lib/yt-dlp.exe -f best -g " + youtubeUrl;
        StringBuilder videoUrl = new StringBuilder();

        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                videoUrl.append(line).append("\n");
            }

            reader.close();
            process.waitFor();

            return videoUrl.toString().trim();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
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
