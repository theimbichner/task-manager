package io.github.theimbichner.taskmanager.task;

import java.time.Instant;

import io.vavr.Tuple2;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import io.vavr.control.Either;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.io.TaskAccessException;
import io.github.theimbichner.taskmanager.io.TaskAccessResult;
import io.github.theimbichner.taskmanager.io.TaskStore;

public class TableMutator {
   private final TaskStore taskStore;
   private final ItemId<Table> tableId;

   public TableMutator(TaskStore taskStore, ItemId<Table> tableId) {
      this.taskStore = taskStore;
      this.tableId = tableId;
   }

   public static TaskAccessResult<Table> createTable(TaskStore taskStore) {
      return TaskAccessResult.transaction(taskStore, () -> {
         Table table = Table.newTable();
         return taskStore.getTables().save(table).get();
      });
   }

   public TaskAccessResult<SetList<ItemId<Task>>> getTasksFromTable(Instant timestamp) {
      return TaskAccessResult.transaction(taskStore, () -> {
         Table table = taskStore.getTables().getById(tableId).get();

         for (ItemId<Generator> generatorId : table.getAllGeneratorIds().asList()) {
            Seq<ItemId<Task>> newTasks = runGenerator(generatorId, timestamp).get();
            table = table.withTasks(newTasks);
         }

         table = taskStore.getTables().save(table).get();
         return table.getAllTaskIds();
      });
   }

   public TaskAccessResult<Table> modifyTable(TableDelta delta) {
      return TaskAccessResult.transaction(taskStore, () -> {
         Table table = taskStore.getTables().getById(tableId).get();

         for (ItemId<Task> id : table.getAllTaskIds().asList()) {
            Task task = taskStore.getTasks().getById(id).get();
            TaskDelta taskDelta = delta.asTaskDelta(task.getProperties());
            task = task.withModification(taskDelta);
            taskStore.getTasks().save(task).checkError();
         }

         for (ItemId<Generator> id : table.getAllGeneratorIds().asList()) {
            Generator generator = taskStore.getGenerators().getById(id).get();
            generator = generator.adjustToSchema(delta.getSchema());
            taskStore.getGenerators().save(generator).checkError();
         }

         Table modifiedTable = table.withModification(delta);
         return taskStore.getTables().save(modifiedTable).get();
      });
   }

   private TaskAccessResult<Seq<ItemId<Task>>> runGenerator(
      ItemId<Generator> generatorId,
      Instant timestamp
   ) {
      return TaskAccessResult.of(() -> {
         Generator generator = taskStore.getGenerators().getById(generatorId).get();
         Tuple2<Generator, Vector<Task>> tuple = generator.withTasksUntil(timestamp);

         generator = taskStore.getGenerators().save(tuple._1).get();
         Vector<Either<TaskAccessException, Task>> eithers = tuple
            ._2
            .map(taskStore.getTasks()::save)
            .map(TaskAccessResult::asEither);
         Seq<Task> tasks = TaskAccessResult.getEither(Either.sequenceRight(eithers));

         return tasks.map(Task::getId);
      });
   }
}
