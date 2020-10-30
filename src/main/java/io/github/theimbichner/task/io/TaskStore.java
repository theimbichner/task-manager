package io.github.theimbichner.task.io;

import java.io.File;
import java.util.function.Function;

import org.json.JSONObject;

import io.github.theimbichner.task.Generator;
import io.github.theimbichner.task.Table;
import io.github.theimbichner.task.Task;

public class TaskStore {
   private class TableLoader implements DataStore<Table> {
      private final DataStore<Table> delegate;

      public TableLoader(DataStore<Table> delegate) {
         this.delegate = delegate;
      }

      @Override
      public String getId(Table t) {
         return delegate.getId(t);
      }

      @Override
      public Table getById(String id) throws TaskAccessException {
         Table result = delegate.getById(id);
         result.registerTaskStore(TaskStore.this);
         return result;
      }

      @Override
      public void save(Table t) throws TaskAccessException {
         delegate.save(t);
      }

      @Override
      public void deleteById(String id) throws TaskAccessException {
         delegate.deleteById(id);
      }
   }

   public static final int MAXIMUM_TASKS_CACHED = 10_000;
   public static final int MAXIMUM_GENERATORS_CACHED = 1000;
   public static final int MAXIMUM_TABLES_CACHED = 1000;

   private static final String TASKS_FOLDER_NAME = "tasks";
   private static final String GENERATORS_FOLDER_NAME = "generators";
   private static final String TABLES_FOLDER_NAME = "tables";

   private final DataStore<Task> tasks;
   private final DataStore<Generator> generators;
   private final DataStore<Table> tables;

   public TaskStore(
      DataStore<Task> tasks,
      DataStore<Generator> generators,
      DataStore<Table> tables
   ) {
      this.tasks = tasks;
      this.generators = generators;
      this.tables = new TableLoader(tables);
   }

   public DataStore<Task> getTasks() {
      return tasks;
   }

   public DataStore<Generator> getGenerators() {
      return generators;
   }

   public DataStore<Table> getTables() {
      return tables;
   }

   public static TaskStore getDefault(File root) {
      return new TaskStore(
         new CachedDataStore<>(
            new JsonFileDataStore<>(
               new File(root, TASKS_FOLDER_NAME),
               Task::getId,
               Task::toJson,
               Task::fromJson),
            MAXIMUM_TASKS_CACHED),
         new CachedDataStore<>(
            new JsonFileDataStore<>(
               new File(root, GENERATORS_FOLDER_NAME),
               Generator::getId,
               Generator::toJson,
               Generator::fromJson),
            MAXIMUM_GENERATORS_CACHED),
         new CachedDataStore<>(
            new JsonFileDataStore<>(
               new File(root, TABLES_FOLDER_NAME),
               Table::getId,
               Table::toJson,
               Table::fromJson),
            MAXIMUM_TABLES_CACHED));
   }
}
