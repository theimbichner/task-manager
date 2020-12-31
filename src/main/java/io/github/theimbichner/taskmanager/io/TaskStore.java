package io.github.theimbichner.taskmanager.io;

import java.io.File;

import io.vavr.control.Either;

import io.github.theimbichner.taskmanager.task.Generator;
import io.github.theimbichner.taskmanager.task.Table;
import io.github.theimbichner.taskmanager.task.Task;

public class TaskStore {
   private class TaskStoreLoader<T extends Storable> implements DataStore<T> {
      private final DataStore<T> delegate;

      public TaskStoreLoader(DataStore<T> delegate) {
         this.delegate = delegate;
      }

      @Override
      public Either<TaskAccessException, T> getById(String id) {
         return delegate.getById(id).peek(t -> t.setTaskStore(TaskStore.this));
      }

      @Override
      public Either<TaskAccessException, T> save(T t) {
         return delegate.save(t);
      }

      @Override
      public Either<TaskAccessException, Void> deleteById(String id) {
         return delegate.deleteById(id);
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
      this.tasks = new TaskStoreLoader<>(tasks);
      this.generators = new TaskStoreLoader<>(generators);
      this.tables = new TaskStoreLoader<>(tables);
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
