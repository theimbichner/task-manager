package io.github.theimbichner.taskmanager.fxclient;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import io.vavr.collection.Vector;

import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.task.ItemId;
import io.github.theimbichner.taskmanager.task.Table;
import io.github.theimbichner.taskmanager.task.TableMutator;
import io.github.theimbichner.taskmanager.task.Task;
import io.github.theimbichner.taskmanager.task.TaskMutator;

public class Controller {
   public VBox tableList;
   public VBox taskList;

   private TaskStore taskStore;
   private ItemId<Table> currentTable;

   public void initialize() {
      try {
         taskStore = getDefaultTaskStore();
      }
      catch (IOException e) {
         // TODO figure out how the error handling changes when we get the task
         // store the real way
         showError(e);
         return;
      }

      populateTableList();
   }

   private void populateTableList() {
      taskStore.getTables().listIds()
         .andThen(tableIds -> {
            Vector<Button> buttons = Vector.empty();
            // TODO order of iteration is completely random
            for (ItemId<Table> id : tableIds) {
               Table table = taskStore.getTables().getById(id).get();
               buttons = buttons.append(newSelectTableButton(table));
            }
            buttons = buttons.append(newCreateTableButton());

            return buttons;
         })
         .peek(buttons -> {
            tableList.getChildren().clear();
            tableList.getChildren().addAll(buttons.asJava());
         })
         .peekLeft(this::showError);
   }

   private void populateTaskList() {
      TableMutator mutator = new TableMutator(taskStore, currentTable);
      mutator.getTasksFromTable(Instant.now())
         .andThen(taskIds -> {
            Vector<Button> buttons = Vector.empty();
            for (ItemId<Task> id : taskIds.asList()) {
               Task task = taskStore.getTasks().getById(id).get();
               buttons = buttons.append(newSelectTaskButton(task));
            }
            buttons = buttons.append(newCreateTaskButton());

            return buttons;
         })
         .peek(buttons -> {
            taskList.getChildren().clear();
            taskList.getChildren().addAll(buttons.asJava());
         })
         .peekLeft(this::showError);
   }

   private Button newSelectTableButton(Table table) {
      Button button = new Button(table.getId().toString());
      button.setOnAction(event -> {
         currentTable = table.getId();
         populateTaskList();
      });

      return button;
   }

   private Button newSelectTaskButton(Task task) {
      Button button = new Button(task.getId().toString());

      return button;
   }

   private Button newCreateTableButton() {
      Button button = new Button("+");
      button.setOnAction(event -> {
         TableMutator.createTable(taskStore).peekLeft(this::showError);
         populateTableList();
      });

      return button;
   }

   private Button newCreateTaskButton() {
      Button button = new Button("+");
      button.setOnAction(event -> {
         TaskMutator.createTask(taskStore, currentTable).peekLeft(this::showError);
         populateTaskList();
      });

      return button;
   }

   private TaskStore getDefaultTaskStore() throws IOException {
      File home = new File(System.getProperty("user.home"));
      return TaskStore.getDefault(new File(home, ".taskmanager"));
   }

   private void showError(Exception e) {
      Platform.runLater(() -> {
         StringWriter writer = new StringWriter();
         e.printStackTrace(new PrintWriter(writer));
         String errorText = writer.toString();

         Alert alert = new Alert(Alert.AlertType.ERROR);
         alert.setContentText(errorText);

         alert.showAndWait();
         Platform.exit();
      });
   }
}
