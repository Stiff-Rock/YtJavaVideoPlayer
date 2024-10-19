package org.stiffrock;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;

public class VideoCardController {
    private AppController appController;
    @FXML
    private HBox rootPanel;
    @FXML
    private ProgressIndicator progressInd;
    @FXML
    private Label title;
    @FXML
    private Label duration;
    @FXML
    private ImageView thumbnail;
    @FXML
    private HBox buttonArea;

    private String videoId;

    private VBox parent;

    private int queueIndex;

    /**
     * Sets the application controller for this video card.
     *
     * @param appController the AppController instance to be set
     */
    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    /**
     * Sets the parent VBox container for this video card.
     *
     * @param parent the VBox instance that serves as the parent
     */
    public void setParent(VBox parent) {
        this.parent = parent;
    }

    /**
     * Updates the video card UI with the provided video information, including title, thumbnail,
     * duration, and video ID. Sets visibility for relevant UI components.
     *
     * @param videoInfo a SimpleEntry containing the video ID and an array of video details:
     *                  [0] = title, [1] = thumbnail URL, [2] = duration, [3] = video ID
     */
    public void setVideo(SimpleEntry<String, String[]> videoInfo) {
        Platform.runLater(() -> {
            title.setText(videoInfo.getValue()[0]);
            loadThumbnail(videoInfo.getValue()[1]);
            duration.setText(videoInfo.getValue()[2]);
            videoId = videoInfo.getValue()[3];

            duration.setVisible(true);
            progressInd.setVisible(false);
            buttonArea.setVisible(true);
        });

        queueIndex = parent.getChildren().indexOf(rootPanel);
    }

    /**
     * Loads a thumbnail image from the specified URL, converts it from WebP format to JPEG,
     * and sets it as the image for the thumbnail ImageView.
     *
     * @param imageUrl the URL of the thumbnail image to be loaded
     */
    private void loadThumbnail(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            InputStream inputStream = url.openStream();

            BufferedImage webpImage = ImageIO.read(inputStream);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(webpImage, "jpg", outputStream);

            InputStream jpegInputStream = new ByteArrayInputStream(outputStream.toByteArray());

            Image fxImage = new Image(jpegInputStream);
            thumbnail.setImage(fxImage);

            inputStream.close();
            outputStream.close();

        } catch (Exception e) {
            System.err.println("Error loading the converted thumbnail. " + e.getMessage());
        }
    }

    /**
     * Triggered when the card's thumbnail in the UI is clicked.
     * Removes the video card from the displayed queue and plays the selected video.
     */
    @FXML
    private void play() {
        parent.getChildren().remove(rootPanel);
        appController.playSelectedVideoCard(queueIndex);
    }

    /**
     * Triggered when the delete action is initiated for this video card.
     * Removes the video card from the displayed queue and updates the video queue.
     */
    @FXML
    private void delete() {
        parent.getChildren().remove(rootPanel);
        VideoLoader.removeVideoFromQueue(queueIndex);
    }

    /**
     * Moves the current video card down in the queue if it's not already at the bottom.
     */
    @FXML
    private void moveCardDown() {
        // Ensure the current index is not at the last position
        if (queueIndex < VideoLoader.getQueueSize() - 1) {
            moveCard(queueIndex + 1);
        } else {
            System.out.println("Cannot move down, already at the bottom of the queue.");
        }
    }

    /**
     * Moves the current video card up in the queue if it's not already at the top.
     */
    @FXML
    private void moveCardUp() {
        // Ensure the current index is not at the first position
        if (queueIndex > 0) {
            moveCard(queueIndex - 1);
        } else {
            System.out.println("Cannot move up, already at the top of the queue.");
        }
    }

    /**
     * Moves the video card to a new position in the queue.
     *
     * @param index The new index to which the card should be moved.
     */
    private void moveCard(int index) {
        VideoLoader.changeVideoPositionInQueue(queueIndex, index);
        Platform.runLater(() -> {
            parent.getChildren().remove(rootPanel);
            parent.getChildren().add(queueIndex, rootPanel);
        });
    }

    /**
     * Updates the index of the video card in the queue.
     *
     * @param queueIndex The new index for the video card.
     */
    public void updateQueueIndex(int queueIndex) {
        this.queueIndex = queueIndex;
    }

    /**
     * Returns the unique identifier for the video associated with this card.
     *
     * @return The video ID, or null if not set.
     */
    public String getVideoId() {
        return videoId;
    }
}
