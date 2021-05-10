package io.github.theimbichner.taskmanager.fxclient;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;

import javafx.application.Platform;
import javafx.scene.Node;
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

      refreshTableList();
   }

   private void refreshTableList() {
      taskStore.getTables().listIds()
         .andThen(tableIds -> {
            ArrayList<Node> entries = new ArrayList<>();
            // TODO order of iteration is completely random
            for (ItemId<Table> id : tableIds) {
               Table table = taskStore.getTables().getById(id).get();
               entries.add(TableSelectionButtons.newTableEntry(this, table));
            }
            entries.add(newCreateTableButton());

            return entries;
         })
         .peek(entries -> {
            tableList.getChildren().clear();
            tableList.getChildren().addAll(entries);
         })
         .peekLeft(this::showError);
   }

   private void refreshTaskList() {
      TableMutator mutator = new TableMutator(taskStore, currentTable);
      mutator.getTasksFromTable(Instant.now())
         .andThen(taskIds -> {
            ArrayList<Node> entries = new ArrayList<>();
            for (ItemId<Task> id : taskIds.asList()) {
               Task task = taskStore.getTasks().getById(id).get();
               entries.add(TaskSelectionButtons.newTaskEntry(this, task));
            }
            entries.add(newCreateTaskButton());

            return entries;
         })
         .peek(entries -> {
            taskList.getChildren().clear();
            taskList.getChildren().addAll(entries);
         })
         .peekLeft(this::showError);
   }

   public void setCurrentTable(ItemId<Table> tableId) {
      currentTable = tableId;
      refreshTaskList();
   }

   private Button newCreateTableButton() {
      Button button = new Button("+");
      button.setOnAction(event -> {
         TableMutator.createTable(taskStore).peekLeft(this::showError);
         refreshTableList();
      });

      return button;
   }

   private Button newCreateTaskButton() {
      Button button = new Button("+");
      button.setOnAction(event -> {
         TaskMutator.createTask(taskStore, currentTable).peekLeft(this::showError);
         refreshTaskList();
      });

      return button;
   }

   private TaskStore getDefaultTaskStore() throws IOException {
      File home = new File(System.getProperty("user.home"));
      return TaskStore.getDefault(new File(home, ".taskmanager"));
   }

   private void showError(Exception e) {
      e.printStackTrace();
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
