package io.github.theimbichner.taskmanager.fxclient;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import io.github.theimbichner.taskmanager.task.Task;

public class TaskSelectionButtons {
   public static HBox newTaskEntry(Controller controller, Task task) {
      HBox result = new HBox();
      result.getChildren().add(newSelectButton(controller, task));
      result.getChildren().add(newEditButton(task));

      return result;
   }

   private static Button newSelectButton(Controller controller, Task task) {
      Button button = new Button("ID: " + task.getId().toString());

      return button;
   }

   private static Button newEditButton(Task task) {
      return new Button("Edit");
   }
}
