package org.stiffrock;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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

import java.awt.*;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

public class AppController {

    //TODO Allow multithreaded loading
    //TODO Make dark theme
    //TODO Handle errors properly
    //TODO Handle media errors (repeat or maybe give out the mediaPlayer directly)

    private ImageView load;
    private ImageView cross;
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
    private Button btnLoad;
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

    private final Map<HBox, VideoCardController> activeVideoCards = new HashMap<>();
    private VideoCardController currentVideoCardController;

    private boolean isProgressBarDragged;
    private boolean isLoopEnabled;
    private boolean isAutoplayEnabled;
    private boolean isQueueVisible;
    private boolean isFadeOutActive;

    private static Task<Void> currentLoadingTask;

    /**
     * JavaFX required initialize() function for the controller.
     * Configures listeners and loads UI icons for dynamic updates.
     */
    @FXML
    public void initialize() {
        // Loads the UI icons for dynamic loading
        initializeIcons();

        /*
         * Sets up a listener to handle updates to the video queue through the `VideoLoader` class.
         * The listener triggers whenever there is a change in the queue (new video added or queue becomes empty).
         */
        VideoLoader.setOnQueueUpdateListener(newAddedToQueue -> {
            if (newAddedToQueue && mediaPlayer != null && !VideoLoader.isQueueEmpty()) {
                currentVideoCardController.setVideo(VideoLoader.peekVideoFromQueue(VideoLoader.getQueueSize() - 1));
                btnNext.setDisable(false);
            } else if (!newAddedToQueue && !VideoLoader.isQueueEmpty()) {
                updateVideoCardQueueIndexes();
            } else if (!newAddedToQueue) {
                btnNext.setDisable(true);
            }
            Platform.runLater(() -> queueTitledPanel.setText("Video Queue - (" + VideoLoader.getQueueSize() + ")"));
        });

        // Initializes the volume control functionality using the UI slider.
        masterVolume = volumeSlider.getValue();
        volumeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            masterVolume = newValue.doubleValue();
            if (mediaPlayer != null) mediaPlayer.setVolume(masterVolume);
        });
    }

    /**
     * Handles the action when the user presses the "Load" button.
     * If a loading task is currently active, this method acts as a cancel button,
     */
    @FXML
    private void loadBtn() {
        if (currentLoadingTask != null && currentLoadingTask.isRunning()) {
            currentLoadingTask.cancel();
            queueDisplayPanel.getChildren().remove(queueDisplayPanel.getChildren().size() - 1);
            Platform.runLater(() -> btnLoad.setGraphic(load));
        } else {
            loadRequest();
            Platform.runLater(() -> btnLoad.setGraphic(cross));
        }
    }

    /**
     * It retrieves the URL from the input field, checks if it is a valid URL, and determines whether it
     * corresponds to a playlist or a single video depending of wether the string contains "list=" or not.
     * Depending on the case, it calls the appropriate method.
     */
    private void loadRequest() {
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

    /**
     * This method is called by the `loadBtn()` function, passing the appropriate task (`startUrlLoading`)
     * to handle the video or playlist loading process.
     */
    private void loadUrlsTask(Task<Void> startUrlLoading) {
        startUrlLoading.setOnSucceeded(event -> loadNextStream());

        if (mediaPlayer == null) {
            progressInd.setVisible(true);
        } else {
            addVideoCardToQueue();
        }

        new Thread(startUrlLoading).start();
    }

    /**
     * Loads the next video(s) from the queue by retrieving the stream URL in a background task.
     * If no media player is active, it plays the video; otherwise, it adds the video to the queue.
     * It recursively loads more videos if the requests queue isn't empty.
     */
    private void loadNextStream() {
        currentLoadingTask = VideoLoader.retrieveStreamUrl();

        currentLoadingTask.setOnSucceeded(streamEvent -> {
            if (mediaPlayer == null) {
                displayVideo(VideoLoader.pollStreamUrl());
            }

            if (VideoLoader.getVideoRequestsSize() != 0) {
                addVideoCardToQueue();
                loadNextStream();
            } else {
                btnLoad.setDisable(false);
                Toolkit.getDefaultToolkit().beep();
                System.out.println("--------------------");
                System.out.println("Finished loading video/s");
                System.out.println("--------------------");
            }
        });

        new Thread(currentLoadingTask).start();
    }

    /**
     * Loads the FXML structure for the video card in a loading state and adds it to the queue.
     * The video info is populated when triggered by the setOnQueueUpdateListener.
     */
    private void addVideoCardToQueue() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("queueCard.fxml"));
                HBox videoCard = loader.load();

                currentVideoCardController = loader.getController();

                currentVideoCardController.setAppController(this);
                currentVideoCardController.setParent(queueDisplayPanel);
                activeVideoCards.put(videoCard, currentVideoCardController);

                queueDisplayPanel.getChildren().add(videoCard);
            } catch (IOException e) {
                System.err.println("Error loading video card: " + e.getMessage());
            }
        });
    }

    /**
     * Plays the selected video by its index in the queue.
     *
     * @param queueIndex The index of the selected video card.
     */
    public void playSelectedVideoCard(int queueIndex) {
        changeVideo(VideoLoader.pollVideoByIndex(queueIndex));
    }

    /**
     * Toggles the video state based on the current playback status,
     * triggered by the play/pause button in the UI.
     */
    @FXML
    private void changeVideoState() {
        if (isNotPlaying()) {
            playBtn();
        } else if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            pauseBtn();
        }
    }

    /**
     * Loads the next video from the queue and removes it from the UI,
     * triggered by the "next" button in the UI.
     */
    @FXML
    private void nextBtn() {
        if (mediaPlayer != null && !VideoLoader.isQueueEmpty()) {
            changeVideo(VideoLoader.pollStreamUrl());
            Platform.runLater(() -> queueDisplayPanel.getChildren().remove(0));
        }
    }

    /**
     * Changes the currently playing video to the specified video entry.
     *
     * @param videoEntry The video entry containing the video information.
     */
    private void changeVideo(SimpleEntry<Media, String[]> videoEntry) {
        mediaPlayer.stop();
        progressBar.setValue(0);
        toggleBtnPlayPause(play);
        displayVideo(videoEntry);
        if (VideoLoader.isQueueEmpty()) btnNext.setDisable(true);
    }

    /**
     * Starts playback of the current video and updates the play/pause button state.
     */
    private void playBtn() {
        mediaPlayer.play();

        toggleBtnPlayPause(pause);
    }

    /**
     * Pauses the current video playback.
     * If a fade-out effect is active, it initiates the fade-out; otherwise, it pauses the video and updates the button state.
     */
    private void pauseBtn() {
        if (isFadeOutActive) {
            fadeOut();
        } else {
            mediaPlayer.pause();
            toggleBtnPlayPause(play);
        }
    }

    /**
     * Toggles the loop feature for the current video playback.
     * Updates the button graphic based on the current loop state.
     */
    @FXML
    private void loopBtn() {
        if (mediaPlayer != null) {
            isLoopEnabled = !isLoopEnabled;
            btnLoop.setGraphic(isLoopEnabled ? loopEnabled : loopDisabled);
            mediaPlayer.setCycleCount(isLoopEnabled ? MediaPlayer.INDEFINITE : 1);
        }
    }

    /**
     * Toggles the autoplay feature on or off.
     * Updates the button graphic based on the current autoplay state.
     */
    @FXML
    private void autoplayBtn() {
        if (mediaPlayer != null) {
            isAutoplayEnabled = !isAutoplayEnabled;
            btnAutoplay.setGraphic(isAutoplayEnabled ? autoplayEnabled : autoplayDisabled);
        }
    }

    /**
     * Displays the specified video by setting up the MediaPlayer and updating UI elements.
     * It initializes video metadata, handles autoplay and looping, and manages error handling.
     *
     * @param video The video entry containing the video URL and title.
     */
    private void displayVideo(SimpleEntry<Media, String[]> video) {
        lblVideoTitle.setText(video.getValue()[0]);

        progressInd.setVisible(true);

        Media media = video.getKey();
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnReady(() -> {
            progressInd.setVisible(false);
            lblVideoCurrentTime.setText("00:00");
            lblVideoDuration.setText(formatTime(mediaPlayer.getTotalDuration().toSeconds()));
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            if (isAutoplayEnabled) {
                nextBtn();
                toggleBtnPlayPause(pause);
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

    /**
     * Initializes listeners for the progress bar to handle user interactions
     * and updates the current playback time.
     * <p>
     * Listeners include:
     * - Dragging the slider to change the playback position
     * - Pressing and releasing the slider to seek to a specific time
     * - Updating the slider as the media plays.
     */
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

    /**
     * Formats a given time in seconds into a string representation
     * in the format of minutes:seconds or hours:minutes:seconds.
     *
     * @param seconds The time in seconds to format.
     * @return A formatted string representing the time.
     */
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

    /**
     * Toggles the visibility of the video queue.
     * Updates the button text and adjusts the size of the media view panel accordingly.
     */
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

    /**
     * Updates the play/pause button graphic to the specified image.
     *
     * @param image The ImageView to set as the button graphic,
     *              representing either play or pause state.
     */
    private void toggleBtnPlayPause(ImageView image) {
        Platform.runLater(() -> btnPlayPause.setGraphic(image));
    }

    /**
     * Checks if the media player is not currently playing.
     *
     * @return true if the media player is paused, ready, or stopped; false if playing.
     */
    private boolean isNotPlaying() {
        return mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED || mediaPlayer.getStatus() == MediaPlayer.Status.READY || mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED;
    }

    /**
     * Toggles the fade-out effect for the audio playback.
     * Updates the button text to reflect the current state of the fade-out effect.
     */
    @FXML
    private void toggleFadeOut() {
        isFadeOutActive = !isFadeOutActive;
        String txt = isFadeOutActive ? "Enabled" : "Disabled";
        btnToggleFadeOut.setText("Fade out: " + txt);
    }

    /**
     * Gradually decreases the volume to zero over time, creating a fade-out effect.
     * Disables playback controls during the fade-out process and pauses the media player
     * once the volume reaches zero.
     */
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
                        //noinspection BusyWait
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

    /**
     * Initializes UI icons for playback controls and settings by loading images from resources.
     * These images allow for dynamic changes to the user interface.
     */
    private void initializeIcons() {
        load = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/download.png"))));

        cross = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/cross.png"))));

        play = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/play.png"))));

        pause = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/pause.png"))));

        loopDisabled = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/loop0.png"))));

        loopEnabled = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/loop1.png"))));

        autoplayEnabled = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/autoplay1.png"))));

        autoplayDisabled = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("media/autoplay0.png"))));
    }

    /**
     * Updates all the videoCardControllers in the activeVideoCards Map
     */
    public void updateVideoCardQueueIndexes() {
        LinkedList<SimpleEntry<Media, String[]>> queue = VideoLoader.getQueue();

        for (Map.Entry<HBox, VideoCardController> cardEntry : activeVideoCards.entrySet()) {
            String cardVideoId = cardEntry.getValue().getVideoId();

            for (int i = 0; i < queue.size(); i++) {
                SimpleEntry<Media, String[]> queueEntry = queue.get(i);
                if (queueEntry.getValue()[3].equals(cardVideoId)) {
                    cardEntry.getValue().updateQueueIndex(i);
                    break;
                }
            }
        }
    }
}
