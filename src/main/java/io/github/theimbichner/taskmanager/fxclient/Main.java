package io.github.theimbichner.taskmanager.fxclient;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
   public static void main(String[] args) {
      Application.launch();
   }

   @Override
   public void start(Stage stage) throws IOException {
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(Main.class.getResource("/fxml/main.fxml"));

      Scene scene = new Scene(loader.load(), 1280, 720);
      stage.setScene(scene);
      stage.show();
   }
}
