package io.github.theimbichner.taskmanager.io;

import java.io.File;

import io.vavr.control.Either;

import io.github.theimbichner.taskmanager.task.Generator;
import io.github.theimbichner.taskmanager.task.ItemId;
import io.github.theimbichner.taskmanager.task.Table;
import io.github.theimbichner.taskmanager.task.Task;

public class TaskStore {
   private class TaskStoreLoader<K, V extends Storable<K>> implements DataStore<K, V> {
      private final DataStore<K, V> delegate;

      public TaskStoreLoader(DataStore<K, V> delegate) {
         this.delegate = delegate;
      }

      @Override
      public Either<TaskAccessException, V> getById(K id) {
         return delegate.getById(id).peek(t -> t.setTaskStore(TaskStore.this));
      }

      @Override
      public Either<TaskAccessException, V> save(V value) {
         return delegate.save(value);
      }

      @Override
      public Either<TaskAccessException, Void> deleteById(K id) {
         return delegate.deleteById(id);
      }
   }

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
      this.tasks = new TaskStoreLoader<>(tasks);
      this.generators = new TaskStoreLoader<>(generators);
      this.tables = new TaskStoreLoader<>(tables);
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

   public static TaskStore getDefault(File root) {
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
