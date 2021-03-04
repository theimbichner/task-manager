package io.github.theimbichner.taskmanager.io;

import java.io.File;
import java.io.IOException;

import io.github.theimbichner.taskmanager.task.Generator;
import io.github.theimbichner.taskmanager.task.ItemId;
import io.github.theimbichner.taskmanager.task.Table;
import io.github.theimbichner.taskmanager.task.Task;

public class TaskStore {
   public static final int MAXIMUM_TASKS_CACHED = 10_000;
   public static final int MAXIMUM_GENERATORS_CACHED = 1000;
   public static final int MAXIMUM_TABLES_CACHED = 1000;

   private static final String TASKS_FOLDER_NAME = "tasks";
   private static final String GENERATORS_FOLDER_NAME = "generators";
   private static final String TABLES_FOLDER_NAME = "tables";

   private final DataStore<ItemId<Task>, Task> tasks;
   private final DataStore<ItemId<Generator>, Generator> generators;
   private final DataStore<ItemId<Table>, Table> tables;

   public TaskStore(
      DataStore<ItemId<Task>, Task> tasks,
      DataStore<ItemId<Generator>, Generator> generators,
      DataStore<ItemId<Table>, Table> tables
   ) {
      this.tasks = tasks;
      this.generators = generators;
      this.tables = tables;
   }

   public DataStore<ItemId<Task>, Task> getTasks() {
      return tasks;
   }

   public DataStore<ItemId<Generator>, Generator> getGenerators() {
      return generators;
   }

   public DataStore<ItemId<Table>, Table> getTables() {
      return tables;
   }

   public static TaskStore getDefault(File root) throws IOException {
      return new TaskStore(
         new CachedDataStore<>(
            new JsonFileDataStore<>(
               new File(root, TASKS_FOLDER_NAME),
               Task::toJson,
               Task::fromJson),
            MAXIMUM_TASKS_CACHED),
         new CachedDataStore<>(
            new JsonFileDataStore<>(
               new File(root, GENERATORS_FOLDER_NAME),
               Generator::toJson,
               Generator::fromJson),
            MAXIMUM_GENERATORS_CACHED),
         new CachedDataStore<>(
            new JsonFileDataStore<>(
               new File(root, TABLES_FOLDER_NAME),
               Table::toJson,
               Table::fromJson),
            MAXIMUM_TABLES_CACHED));
   }
}
