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

    //TODO Controlar volumen
    //TODO Pantalla de carga
    //TODO Fade in/out
    //TODO Añadir playlists o colas
    //TODO Poner canción en bucle
    //TODO Boton play/pause se combinan
    //TODO Botón next con fade in/out
    //TODO Pausa
    //TODO Stop
    //TODO Mostrar titulo video
    //TODO Gestionar cola

    private MediaPlayer mediaPlayer;
    @FXML
    private TextField tfUrl;
    @FXML
    private Button btnLoad;
    @FXML
    private Button btnPlay;
    @FXML
    private Button btnPause;
    @FXML
    private Button btnStop;
    @FXML
    private Button btnNext;
    @FXML
    private MediaView mediaView;

    @FXML
    private void load() {

    }

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

    @FXML
    private void pause() {

    }

    @FXML
    private void stop() {

    }

    @FXML
    private void next() {

    }

    private String getDirectVideoUrl(String youtubeUrl) {
        String ytdlpPath;

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            ytdlpPath = "lib/yt-dlp.exe";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            ytdlpPath = "lib/yt-dlp_linux";
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

        mediaPlayer.setOnError(() -> System.out.println("Error: " + mediaPlayer.getError().getMessage()));

        mediaPlayer.play();
    }
}
