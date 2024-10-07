package org.stiffrock;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.awt.*;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Objects;

public class AppController {

    //TODO Move queue cards and change order
    //TODO Handle errors properly
    /*TODO:
     * Error loading current media: [com.sun.media.jfxmediaimpl.platform.gstreamer.GSTMediaPlayer@4759d1aa] ERROR_MEDIA_INVALID: ERROR_MEDIA_INVALID
     * Troubleshooting info:
     *  - Media: javafx.scene.media.Media@1d562ce4
     *  - Stream Url: ...
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
    private Button btnToggleFadeOut;
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

    private static double masterVolume;
    private MediaPlayer mediaPlayer;
    private VideoCardController currentVideoCardController;

    private boolean isProgressBarDragged;
    private boolean isLoopEnabled;
    private boolean isAutoplayEnabled;
    private boolean isQueueVisible;
    private boolean isFadeOutActive;

    @FXML
    public void initialize() {
        initializeIcons();

        masterVolume = volumeSlider.getValue();

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
            masterVolume = newValue.doubleValue();
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

        if (mediaPlayer == null) {
            progressInd.setVisible(true);
        } else {
            addVideoCardToQueue();
        }

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
            } else {
                Toolkit.getDefaultToolkit().beep();
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

    public void playSelectedVideoCard(int queueIndex) {
        changeVideo(VideoLoader.pollVideoByIndex(queueIndex));
    }

    private void pollVideoCardQueue() {
        Platform.runLater(() -> queueDisplayPanel.getChildren().remove(0));
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

    private void playBtn() {
        mediaPlayer.play();

        toggleBtnPlayPause(pause);
    }

    private void pauseBtn() {
        if (isFadeOutActive) {
            fadeOut();
        } else {
            mediaPlayer.pause();
            toggleBtnPlayPause(play);
        }
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

        mediaPlayer.setOnEndOfMedia(() -> {
            if (isAutoplayEnabled) {
                nextBtn();
            } else {
                toggleBtnPlayPause(play);
            }
        });

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
        Platform.runLater(() -> btnPlayPause.setGraphic(image));
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

    @FXML
    private void toggleFadeOut() {
        isFadeOutActive = !isFadeOutActive;
        String txt = isFadeOutActive ? "Enabled" : "Disabled";
        btnToggleFadeOut.setText("Fade out: " + txt);
    }

    private void fadeOut() {
        Task<Void> fadeTask = new Task<>() {
            @Override
            protected Void call() {
                double i = 0.005;

                while (masterVolume > 0) {
                    btnPlayPause.setDisable(true);
                    btnNext.setDisable(true);

                    masterVolume -= i;
                    if (masterVolume < 0) {
                        masterVolume = 0;
                    }

                    Platform.runLater(() -> volumeSlider.setValue(masterVolume));

                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException e) {
                        if (isCancelled()) {
                            break;
                        }
                        throw new RuntimeException(e);
                    }
                }

                mediaPlayer.pause();
                toggleBtnPlayPause(play);
                btnPlayPause.setDisable(false);
                btnNext.setDisable(false);

                return null;
            }
        };

        Thread thread = new Thread(fadeTask);
        thread.setDaemon(true);
        thread.start();
    }
}
