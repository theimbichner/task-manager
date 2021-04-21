package io.github.theimbichner.taskmanager;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class Main extends Application {
   public static void main(String[] args) {
      Application.launch();
   }

   @Override
   public void start(Stage stage) {
      Scene scene = new Scene(new Label(""), 1280, 720);
      stage.setScene(scene);
      stage.show();
   }
}
