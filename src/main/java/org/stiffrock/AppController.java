package org.stiffrock;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.util.AbstractMap.SimpleEntry;

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
    //TODO Documentar código

    @FXML
    private Label lblVideoTitle;
    @FXML
    private TextField tfUrl;
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

    private MediaPlayer mediaPlayer;

    @FXML
    public void initialize() {
        VideoLoader.setOnQueueUpdateListener(() -> {
            if (mediaPlayer != null && !VideoLoader.isQueueEmpty()) {
                btnNext.setDisable(false);
            }
        });
    }

    @FXML
    private void load() {
        String videoUrl = tfUrl.getText();

        if (videoUrl == null) {
            System.err.println("Invalid Url.");
            return;
        }

        Task<Void> loadingTask;
        if (videoUrl.contains("list=")) {
            loadingTask = VideoLoader.loadPlaylistUrls(videoUrl);
        } else {
            loadingTask = VideoLoader.loadVideoUrl(videoUrl);
        }

        loadTask(loadingTask);
    }

    private void loadTask(Task<Void> startUrlLoading) {
        startUrlLoading.setOnSucceeded(event -> {
            Task<Void> streamLoadingTask = VideoLoader.retrieveStreamUrl();
            streamLoadingTask.setOnSucceeded(streamEvent -> loadMedia());

            new Thread(streamLoadingTask).start();
        });

        new Thread(startUrlLoading).start();
    }

    private void loadMedia() {
        if (mediaPlayer == null) {
            btnPlay.setDisable(false);

            displayVideo();
        }
    }

    @FXML
    private void play() {
        mediaPlayer.play();

        btnPlay.setDisable(true);
        btnPause.setDisable(false);
        btnStop.setDisable(false);
    }

    @FXML
    private void pause() {
        mediaPlayer.pause();

        btnPlay.setDisable(false);
        btnPause.setDisable(true);
    }

    @FXML
    private void stop() {
        mediaPlayer.stop();
        btnPlay.setDisable(false);
        btnPause.setDisable(true);
        btnStop.setDisable(true);
    }

    @FXML
    private void next() {
        if (mediaPlayer != null && !VideoLoader.isQueueEmpty()) {
            mediaPlayer.stop();

            displayVideo();

            if (VideoLoader.isQueueEmpty()) btnNext.setDisable(true);
        }
    }

    private void displayVideo() {
        SimpleEntry<String, String> video = VideoLoader.pollStreamUrl();

        lblVideoTitle.setText(video.getValue());
        Media media = new Media(video.getKey());

        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);

        MediaPlayer finalMediaPlayer = mediaPlayer;
        mediaPlayer.setOnError(() -> System.out.println("Error: " + finalMediaPlayer.getError().getMessage()));
    }
}
