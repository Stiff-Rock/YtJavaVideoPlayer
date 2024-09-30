package org.stiffrock;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.util.AbstractMap.SimpleEntry;

public class VideoCardController {
    @FXML
    private AnchorPane root;
    @FXML
    private Label title;
    @FXML
    private Label duration;
    @FXML
    private ImageView thumbnail;

    private String videoUrl;

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
        System.out.println(root.getWidth());

        VideoLoader.removeVideoFromQueue(videoUrl);
        if (thumbnail.getParent() != null) {
            VBox parent = (VBox) thumbnail.getParent();

            if (parent != null) {
                parent.getChildren().remove(root);
                System.out.println("Deleted");
            }
        }
    }
}
