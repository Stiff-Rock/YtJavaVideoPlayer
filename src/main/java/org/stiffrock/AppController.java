package org.stiffrock;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Objects;

public class AppController {

    //TODO Controlar volumen
    //TODO Pantalla de cola
    //TODO Fade in/out
    //TODO Botón para poner canción en bucle
    //TODO Botón next con fade in/out
    //TODO Gestionar y visualizar cola con tarjetas
    //TODO Documentar código
    //TODO Reintentar cargar video
    //TODO Animacion slider volumen
    //TODO AUTOPLAY NO VA
    //TODO NEW MEDIA zona da error
    //TODO Poner duración y que se adapte

    private ImageView play;
    private ImageView pause;
    private ImageView loopEnabled;
    private ImageView loopDisabled;
    private ImageView autoplayEnabled;
    private ImageView autoplayDisabled;

    @FXML
    private Label lblVideoTitle;
    @FXML
    private TextField tfUrl;
    @FXML
    private Button btnPlayPause;
    @FXML
    private Button btnNext;
    @FXML
    private Button btnLoop;
    @FXML
    private Button btnAutoplay;
    @FXML
    private StackPane mediaViewPanel;
    @FXML
    private MediaView mediaView;
    @FXML
    private ProgressIndicator progressInd;
    @FXML
    private Slider progressBar;
    @FXML
    private Slider volumeSlider;
    private static double masterVolume = 1.0;
    @FXML
    private VBox queueContainer;
    @FXML
    private VBox queueDisplayPanel;

    private MediaPlayer mediaPlayer;

    private boolean isProgressBarDragged;
    private boolean isLoopEnabled;
    private boolean isAutoplayEnabled;

    @FXML
    private void delete() {
        queueDisplayPanel.getChildren().remove(0);
    }

    @FXML
    public void initialize() {
        play = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("media/play.png"))));

        pause = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("media/pause.png"))));

        loopDisabled = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("media/loop0.png"))));

        loopEnabled = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("media/loop1.png"))));

        autoplayEnabled = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("media/autoplay1.png"))));

        autoplayDisabled = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("media/autoplay0.png"))));

        VideoLoader.setOnQueueUpdateListener(() -> {
            if (mediaPlayer != null && !VideoLoader.isQueueEmpty()) {
                addVideoCardToQueue(VideoLoader.peekVideoFromQueue(VideoLoader.getQueueSize() - 1));
                btnNext.setDisable(false);
            }
        });

        volumeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            masterVolume = newValue.doubleValue() / 100;
            updateMediaPlayerVolumes();
        });

        progressBar.setMin(0);
        progressBar.setMax(100);
    }

    private void updateMediaPlayerVolumes() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(masterVolume);
        }
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

        if (mediaPlayer == null)
            progressInd.setVisible(true);

        new Thread(startUrlLoading).start();
    }

    private void loadMedia() {
        if (mediaPlayer == null) {
            displayVideo();
        }
    }

    private void addVideoCardToQueue(SimpleEntry<String, String[]> videoInfo) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("queueCard.fxml"));
                AnchorPane videoCard = loader.load();

                VideoCardController controller = loader.getController();
                controller.setVideo(videoInfo);

                queueDisplayPanel.getChildren().add(videoCard);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void pollVideoCardQueue() {
        queueDisplayPanel.getChildren().remove(0);
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

        toggleBtnPlayPause(pause);
    }

    @FXML
    private void pause() {
        mediaPlayer.pause();

        toggleBtnPlayPause(play);
    }

    @FXML
    private void next() {
        if (mediaPlayer != null && !VideoLoader.isQueueEmpty()) {
            mediaPlayer.stop();
            progressBar.setValue(0);
            displayVideo();
            toggleBtnPlayPause(play);
            if (VideoLoader.isQueueEmpty()) btnNext.setDisable(true);
        }
    }

    @FXML
    private void setLoopOption() {
        if (mediaPlayer != null) {
            isLoopEnabled = !isLoopEnabled;
            btnLoop.setGraphic(isLoopEnabled ? loopEnabled : loopDisabled);
            mediaPlayer.setCycleCount(isLoopEnabled ? MediaPlayer.INDEFINITE : 1);
        }
    }

    @FXML
    private void setAutoplayOption() {
        if (mediaPlayer != null) {
            isAutoplayEnabled = !isAutoplayEnabled;
            btnAutoplay.setGraphic(isAutoplayEnabled ? autoplayEnabled : autoplayDisabled);
            mediaPlayer.setAutoPlay(isAutoplayEnabled);
        }
    }

    private void displayVideo() {
        SimpleEntry<String, String[]> video = VideoLoader.pollStreamUrl();

        lblVideoTitle.setText(video.getValue()[0]);
        Media media = new Media(video.getKey());

        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnEndOfMedia(() -> toggleBtnPlayPause(play));
        mediaPlayer.setCycleCount(isLoopEnabled ? MediaPlayer.INDEFINITE : 1);
        mediaPlayer.setAutoPlay(isAutoplayEnabled);
        mediaPlayer.setVolume(masterVolume);
        initializeProgressBarListeners();

        mediaView.setMediaPlayer(mediaPlayer);
        progressBar.setDisable(false);
        btnPlayPause.setDisable(false);
        btnAutoplay.setDisable(false);
        btnLoop.setDisable(false);
        progressInd.setVisible(false);

        MediaPlayer finalMediaPlayer = mediaPlayer;
        mediaPlayer.setOnError(() -> System.out.println("Error: " + finalMediaPlayer.getError().getMessage()));
    }

    private void initializeProgressBarListeners() {
        // Slider change by dragging
        progressBar.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> isProgressBarDragged = isChanging);

        // Slider pressing
        progressBar.setOnMousePressed(event -> {
            isProgressBarDragged = true;
            double value = (event.getX() / progressBar.getWidth()) * 100;
            if (value < 0) value = 0;
            if (value > 100) value = 100;
            progressBar.setValue(value);
        });

        // Slider released
        progressBar.setOnMouseReleased(event -> {
            double sliderValue = progressBar.getValue();
            double totalDuration = mediaPlayer.getTotalDuration().toSeconds();
            if (totalDuration > 0) {
                mediaPlayer.seek(Duration.seconds(sliderValue / 100 * totalDuration));
            }
            isProgressBarDragged = false;
        });

        // Slider progress updating (not updating slider value in this case)
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!isProgressBarDragged) {
                double currentTime = mediaPlayer.getCurrentTime().toSeconds();
                double totalDuration = mediaPlayer.getTotalDuration().toSeconds();
                if (totalDuration > 0) {
                    progressBar.setValue((currentTime / totalDuration) * 100);
                }
            }
        });
    }

    @FXML
    private void toggleQueueVisibility() {
        queueContainer.setVisible(!queueContainer.isVisible());
        queueContainer.setManaged(!queueContainer.isManaged());

        int width = queueContainer.isVisible() ? 480 : 640;
        int height = queueContainer.isVisible() ? 270 : 360;

        mediaViewPanel.setMaxWidth(width);
        mediaViewPanel.setMaxHeight(height);

        mediaView.setFitWidth(width);
        mediaView.setFitHeight(height);
    }

    private void toggleBtnPlayPause(ImageView image) {
        btnPlayPause.setGraphic(image);
    }

    private boolean isNotPlaying() {
        return mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED || mediaPlayer.getStatus() == MediaPlayer.Status.READY || mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED;
    }
}
