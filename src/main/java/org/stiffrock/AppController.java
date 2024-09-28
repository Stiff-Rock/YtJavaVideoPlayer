package org.stiffrock;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.util.AbstractMap.SimpleEntry;
import java.util.Objects;

public class AppController {

    //TODO Controlar volumen
    //TODO Pantalla de carga
    //TODO Fade in/out
    //TODO Botón para poner canción en bucle
    //TODO Botón next con fade in/out
    //TODO Gestionar y visualizar cola
    //TODO Barra de progreso y borrar botón de stop
    //TODO Documentar código

    @FXML
    private Label lblVideoTitle;
    @FXML
    private TextField tfUrl;
    @FXML
    private Button btnPlayPause;
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
            btnPlayPause.setDisable(false);
            displayVideo();
        }
    }

    @FXML
    private void changeVideoState() {
        if (isNotPlaying()) {
            play();
        } else if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            pause();
        }
    }

    @FXML
    private void play() {
        mediaPlayer.play();

        toggleBtnPlayPause(false);
        btnStop.setDisable(false);
    }

    @FXML
    private void pause() {
        mediaPlayer.pause();

        toggleBtnPlayPause(true);
    }

    @FXML
    private void stop() {
        mediaPlayer.stop();

        toggleBtnPlayPause(true);
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

    private void toggleBtnPlayPause(boolean state) {
        if (state) {
            btnPlayPause.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/play.png")))));
        } else {
            btnPlayPause.setGraphic(new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/pause.png")))));
        }
    }

    private boolean isNotPlaying() {
        return mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED || mediaPlayer.getStatus() == MediaPlayer.Status.READY || mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED;
    }
}
