package io.github.theimbichner.taskmanager.fxclient;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import io.github.theimbichner.taskmanager.task.Table;

public class TableSelectionButtons {
   public static HBox newTableEntry(Controller controller, Table table) {
      HBox result = new HBox();
      result.getChildren().add(newSelectButton(controller, table));
      result.getChildren().add(newEditButton(table));

      return result;
   }

   private static Button newSelectButton(Controller controller, Table table) {
      Button button = new Button("ID: " + table.getId().toString());
      button.setOnAction(event -> {
         controller.setCurrentTable(table.getId());
      });

      return button;
   }

   private static Button newEditButton(Table table) {
      return new Button("Edit");
   }
}
