package org.stiffrock;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.util.AbstractMap.SimpleEntry;

public class VideoCardController {
    @FXML
    private AnchorPane rootPanel;
    @FXML
    private Label title;
    @FXML
    private Label duration;
    @FXML
    private ImageView thumbnail;

    private String videoUrl;

    private VBox parent;

    public void setParent(VBox parent) {
        this.parent = parent;
    }

    public void setVideo(SimpleEntry<String, String[]> videoInfo) {
        videoUrl = videoInfo.getKey();
        title.setText(videoInfo.getValue()[0]);
        thumbnail.setImage(new Image(videoInfo.getValue()[1]));
        duration.setText(videoInfo.getValue()[2]);
    }

    @FXML
    private void play() {

    }

    @FXML
    private void delete() {
        System.out.println(rootPanel.getWidth());
        parent.getChildren().remove(rootPanel);
        VideoLoader.removeVideoFromQueue(videoUrl);
    }
}
