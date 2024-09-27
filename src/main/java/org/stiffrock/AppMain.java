package org.stiffrock;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class AppMain extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(AppMain.class.getResource("appView.fxml"));
        scene = new Scene(fxmlLoader.load());
        stage.getIcons().add(new Image(getClass().getResourceAsStream("media/icon.png")));
        stage.setScene(scene);
        stage.setTitle("Yt Video Player");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}