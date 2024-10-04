package org.stiffrock;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.util.stream.Stream;

public class AppController {

    //TODO Animacion de mostrar/ocultar slider volumen
    //TODO Loading indicator in video cards
    //TODO FadeOut
    //TODO Autoplay next video change
    //TODO make it so you cant use btnNext while card is loading
    //TODO using btnNext too fast breaks it

    /* TODO:
     * Error loading current media: [com.sun.media.jfxmediaimpl.platform.gstreamer.GSTMediaPlayer@4759d1aa] ERROR_MEDIA_INVALID: ERROR_MEDIA_INVALID
     * Troubleshooting info:
     *  - Media: javafx.scene.media.Media@1d562ce4
     *  - Stream Url:
     */

    private ImageView play;
    private ImageView pause;
    private ImageView loopEnabled;
    private ImageView loopDisabled;
    private ImageView autoplayEnabled;
    private ImageView autoplayDisabled;

    @FXML
    private Label lblVideoTitle;
    @FXML
    private Label lblVideoCurrentTime;
    @FXML
    private Label lblVideoDuration;
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
    private Button btnToggleQueue;
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
    @FXML
    private VBox queueContainer;
    @FXML
    private VBox queueDisplayPanel;
    @FXML
    private TitledPane queueTitledPanel;

    private static double masterVolume = 1.0;
    private MediaPlayer mediaPlayer;
    private VideoCardController currentVideoCardController;

    private boolean isProgressBarDragged;
    private boolean isLoopEnabled;
    private boolean isAutoplayEnabled;
    private boolean isQueueVisible;

    @FXML
    public void initialize() {
        initializeIcons();

        VideoLoader.setOnQueueUpdateListener(newAddedToQueue -> {
            if (newAddedToQueue && mediaPlayer != null && !VideoLoader.isQueueEmpty()) {
                currentVideoCardController.setVideo(VideoLoader.peekVideoFromQueue(VideoLoader.getQueueSize() - 1));
                btnNext.setDisable(false);
            } else if (!newAddedToQueue && VideoLoader.isQueueEmpty()) {
                btnNext.setDisable(true);
            }

            Platform.runLater(() -> queueTitledPanel.setText("Video Queue - (" + VideoLoader.getQueueSize() + ")"));
        });

        volumeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            masterVolume = newValue.doubleValue() / 100;
            if (mediaPlayer != null) mediaPlayer.setVolume(masterVolume);
        });
    }

    @FXML
    private void loadBtn() {
        String videoUrl = tfUrl.getText();

        if (videoUrl == null) {
            System.err.println("Invalid Url.");
            return;
        }

        Task<Void> loadingTask;
        if (videoUrl.contains("list=")) {
            System.out.println("Entered Url corresponds to a playlist");
            loadingTask = VideoLoader.loadPlaylistUrls(videoUrl);
        } else {
            System.out.println("Entered Url corresponds to a video");
            loadingTask = VideoLoader.loadVideoUrl(videoUrl);
        }

        loadUrlsTask(loadingTask);
    }

    private void loadUrlsTask(Task<Void> startUrlLoading) {
        startUrlLoading.setOnSucceeded(event -> loadNextStream());

        if (mediaPlayer == null) progressInd.setVisible(true);

        new Thread(startUrlLoading).start();
    }

    private void loadNextStream() {
        Task<Void> streamLoadingTask = VideoLoader.retrieveStreamUrl();

        streamLoadingTask.setOnSucceeded(streamEvent -> {
            if (mediaPlayer == null) {
                displayVideo(VideoLoader.pollStreamUrl());
            }

            if (VideoLoader.getVideoRequestsSize() != 0) {
                addVideoCardToQueue();
                loadNextStream();
            }

            if (VideoLoader.getVideoRequestsSize() == 0) {
                System.out.println("--------------------");
                System.out.println("Finished loading video/s");
                System.out.println("--------------------");
            }
        });

        new Thread(streamLoadingTask).start();
    }

    private void addVideoCardToQueue() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("queueCard.fxml"));
                HBox videoCard = loader.load();

                currentVideoCardController = loader.getController();

                currentVideoCardController.setAppController(this);
                currentVideoCardController.setParent(queueDisplayPanel);

                queueDisplayPanel.getChildren().add(videoCard);
            } catch (IOException e) {
                System.err.println("Error loading video card: " + e.getMessage());
            }
        });
    }

    public void playSelectedVideoCard(String videoUrl) {
        changeVideo(VideoLoader.pollVideoFromQueueByUrl(videoUrl));
    }

    private void pollVideoCardQueue() {
        queueDisplayPanel.getChildren().remove(0);
    }

    @FXML
    private void changeVideoState() {
        if (isNotPlaying()) {
            playBtn();
        } else if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            pauseBtn();
        }
    }

    @FXML
    private void nextBtn() {
        if (mediaPlayer != null && !VideoLoader.isQueueEmpty()) {
            changeVideo(VideoLoader.pollStreamUrl());
            pollVideoCardQueue();
        }
    }

    private void changeVideo(SimpleEntry<String, String[]> videoEntry) {
        mediaPlayer.stop();
        progressBar.setValue(0);
        toggleBtnPlayPause(play);
        displayVideo(videoEntry);
        if (VideoLoader.isQueueEmpty()) btnNext.setDisable(true);
    }

    @FXML
    private void playBtn() {
        mediaPlayer.play();

        toggleBtnPlayPause(pause);
    }

    @FXML
    private void pauseBtn() {
        mediaPlayer.pause();

        toggleBtnPlayPause(play);
    }

    @FXML
    private void loopBtn() {
        if (mediaPlayer != null) {
            isLoopEnabled = !isLoopEnabled;
            btnLoop.setGraphic(isLoopEnabled ? loopEnabled : loopDisabled);
            mediaPlayer.setCycleCount(isLoopEnabled ? MediaPlayer.INDEFINITE : 1);
        }
    }

    @FXML
    private void autoplayBtn() {
        if (mediaPlayer != null) {
            isAutoplayEnabled = !isAutoplayEnabled;
            btnAutoplay.setGraphic(isAutoplayEnabled ? autoplayEnabled : autoplayDisabled);
        }
    }

    private void displayVideo(SimpleEntry<String, String[]> video) {
        lblVideoTitle.setText(video.getValue()[0]);

        progressInd.setVisible(true);

        Media media = new Media(video.getKey());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnReady(() -> {
            progressInd.setVisible(false);
            lblVideoCurrentTime.setText("00:00");
            lblVideoDuration.setText(formatTime(mediaPlayer.getTotalDuration().toSeconds()));
        });

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

        MediaPlayer finalMediaPlayer = mediaPlayer;
        mediaPlayer.setOnError(() -> {
            System.err.println("Error loading current media: " + finalMediaPlayer.getError().getMessage());
            System.err.println("Troubleshooting info: ");
            System.err.println(" - Media: " + media);
            System.err.println(" - Stream Url: " + video.getKey());
        });
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
                    lblVideoCurrentTime.setText(formatTime(currentTime));
                }
            }
        });
    }

    // Helper method to format time in minutes:seconds
    private String formatTime(double seconds) {
        int hours = (int) seconds / 3600;
        int minutes = ((int) seconds % 3600) / 60;
        int secs = (int) seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }

    @FXML
    private void toggleQueueBtn() {
        isQueueVisible = !isQueueVisible;

        queueContainer.setVisible(isQueueVisible);
        queueContainer.setManaged(isQueueVisible);

        String btnText = isQueueVisible ? "Hide Queue" : "Show Queue";
        btnToggleQueue.setText(btnText);

        int width = isQueueVisible ? 480 : 640;
        int height = isQueueVisible ? 270 : 360;

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

    private void initializeIcons() {
        play = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/play.png"))));

        pause = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/pause.png"))));

        loopDisabled = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/loop0.png"))));

        loopEnabled = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/loop1.png"))));

        autoplayEnabled = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/autoplay1.png"))));

        autoplayDisabled = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/autoplay0.png"))));
    }
}
