package org.stiffrock;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
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
    private BorderPane btnDeleteVideoCard;

    private String videoUrl;

    private VBox parent;

    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    public void setParent(VBox parent) {
        this.parent = parent;
    }

    public void setVideo(SimpleEntry<String, String[]> videoInfo) {
        Platform.runLater(() -> {
            videoUrl = videoInfo.getKey();
            title.setText(videoInfo.getValue()[0]);
            loadThumbnail(videoInfo.getValue()[1]);
            duration.setText(videoInfo.getValue()[2]);
            duration.setVisible(true);
            progressInd.setVisible(false);
            btnDeleteVideoCard.setVisible(true);
        });
    }

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

    @FXML
    private void play() {
        appController.playSelectedVideoCard(videoUrl);
        delete();
    }

    @FXML
    private void delete() {
        parent.getChildren().remove(rootPanel);
        VideoLoader.removeVideoFromQueue(videoUrl);
    }
}
