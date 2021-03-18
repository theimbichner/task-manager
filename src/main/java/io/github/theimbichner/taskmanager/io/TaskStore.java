package io.github.theimbichner.taskmanager.io;

import java.io.File;
import java.io.IOException;

import io.vavr.control.Either;

import io.github.theimbichner.taskmanager.task.Generator;
import io.github.theimbichner.taskmanager.task.ItemId;
import io.github.theimbichner.taskmanager.task.Table;
import io.github.theimbichner.taskmanager.task.Task;

public class TaskStore {
   public static final int MAXIMUM_TASKS_CACHED = 10_000;
   public static final int MAXIMUM_GENERATORS_CACHED = 1000;
   public static final int MAXIMUM_TABLES_CACHED = 1000;

   private static final String TASKS_CHANNEL_NAME = "tasks";
   private static final String GENERATORS_CHANNEL_NAME = "generators";
   private static final String TABLES_CHANNEL_NAME = "tables";

   private static final String DEFAULT_EXTENSION = ".json";

   private final MultiChannelDataStore<String, StringStorable> base;
   private final DataStore<ItemId<Task>, Task> tasks;
   private final DataStore<ItemId<Generator>, Generator> generators;
   private final DataStore<ItemId<Table>, Table> tables;

   public TaskStore(
      MultiChannelDataStore<String, StringStorable> base,
      int numTasksCached,
      int numGeneratorsCached,
      int numTablesCached
   ) {
      this.base = base;

      this.tasks = new CachedDataStore<>(
         new JsonAdapterDataStore<>(
            base.getChannel(TASKS_CHANNEL_NAME),
            Task::toJson,
            Task::fromJson),
         numTasksCached);
      this.generators = new CachedDataStore<>(
         new JsonAdapterDataStore<>(
            base.getChannel(GENERATORS_CHANNEL_NAME),
            Generator::toJson,
            Generator::fromJson),
         numGeneratorsCached);
      this.tables = new CachedDataStore<>(
         new JsonAdapterDataStore<>(
            base.getChannel(TABLES_CHANNEL_NAME),
            Table::toJson,
            Table::fromJson),
         numTablesCached);
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

   public Either<TaskAccessException, Void> commit() {
      return base.commit();
   }

   public void cancelTransaction() {
      base.cancelTransaction();
   }

   public static TaskStore getDefault(File root) throws IOException {
      return new TaskStore(
         new FileDataStore(root, DEFAULT_EXTENSION),
         MAXIMUM_TASKS_CACHED,
         MAXIMUM_GENERATORS_CACHED,
         MAXIMUM_TABLES_CACHED);
   }
}
