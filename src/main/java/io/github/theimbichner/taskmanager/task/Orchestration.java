package io.github.theimbichner.taskmanager.task;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import io.vavr.Tuple2;
import io.vavr.control.Either;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.io.TaskAccessException;
import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.time.DatePattern;

// TODO don't save items when unnecessary

public class Orchestration {
   private final TaskStore taskStore;

   public Orchestration(TaskStore taskStore) {
      this.taskStore = taskStore;
   }

   public Either<TaskAccessException, Table> createTable() {
      Table table = Table.newTable();
      return taskStore.getTables().save(table)
         .peekLeft(x -> taskStore.cancelTransaction())
         .flatMap(this::commit);
   }

   public Either<TaskAccessException, Task> createTask(ItemId<Table> tableId) {
      return taskStore.getTables().getById(tableId).flatMap(table -> {
         Task task = Task.newTask(table);
         Table updatedTable = table.withTasks(List.of(task.getId()));

         return taskStore.getTables().save(updatedTable)
            .flatMap(x -> taskStore.getTasks().save(task))
            .peekLeft(x -> taskStore.cancelTransaction())
            .flatMap(this::commit);
      });
   }

   public Either<TaskAccessException, Generator> createGenerator(
      ItemId<Table> tableId,
      String field,
      DatePattern pattern
   ) {
      return taskStore.getTables().getById(tableId).flatMap(table -> {
         Generator generator = Generator.newGenerator(table, field, pattern);
         Table updateTable = table.withGenerator(generator.getId());

         return taskStore.getTables().save(updateTable)
            .flatMap(x -> taskStore.getGenerators().save(generator))
            .peekLeft(x -> taskStore.cancelTransaction())
            .flatMap(this::commit);
      });
   }

   public Either<TaskAccessException, SetList<ItemId<Task>>> getTasksFromTable(
      ItemId<Table> tableId,
      Instant timestamp
   ) {
      return taskStore.getTables().getById(tableId).flatMap(table -> {
         Either<TaskAccessException, Table> result = Either.right(table);
         for (ItemId<Generator> id : table.getAllGeneratorIds().asList()) {
            result = result.flatMap(resultTable -> runGenerator(id, timestamp)
               .map(resultTable::withTasks));
         }
         return result
            .flatMap(taskStore.getTables()::save)
            .map(Table::getAllTaskIds)
            .peekLeft(x -> taskStore.cancelTransaction())
            .flatMap(this::commit);
      });
   }

   public Either<TaskAccessException, Table> modifyTable(
      ItemId<Table> tableId,
      TableDelta delta
   ) {
      return taskStore.getTables().getById(tableId).flatMap(table -> {
         Either<TaskAccessException, Object> result = Either.right(null);
         for (ItemId<Task> id : table.getAllTaskIds().asList()) {
            result = result
               .flatMap(x -> taskStore.getTasks().getById(id))
               .map(task -> {
                  TaskDelta taskDelta = delta.asTaskDelta(task.getProperties());
                  return task.withModification(taskDelta);
               })
               .flatMap(task -> taskStore.getTasks().save(task));
         }

         for (ItemId<Generator> id : table.getAllGeneratorIds().asList()) {
            result = result
               .flatMap(x -> taskStore.getGenerators().getById(id))
               .map(generator -> generator.adjustToSchema(delta.getSchema()))
               .flatMap(generator -> taskStore.getGenerators().save(generator));
         }

         Table modifiedTable = table.withModification(delta);
         return result
            .flatMap(x -> taskStore.getTables().save(modifiedTable))
            .peekLeft(x -> taskStore.cancelTransaction())
            .flatMap(this::commit);
      });
   }

   public Either<TaskAccessException, Generator> modifyGenerator(
      ItemId<Generator> generatorId,
      GeneratorDelta delta
   ) {
      return taskStore.getGenerators().getById(generatorId).flatMap(generator -> {
         List<Either<TaskAccessException, Task>> tasks = generator
            .getTaskIds()
            .asList()
            .stream()
            .map(taskId -> taskStore.getTasks().getById(taskId)
               .map(task -> task.withSeriesModification(delta, generator))
               .flatMap(taskStore.getTasks()::save))
            .collect(Collectors.toList());
         return Either.sequenceRight(tasks)
            .flatMap(x -> {
               Generator modifiedGenerator = generator.withModification(delta);
               return taskStore.getGenerators().save(modifiedGenerator);
            })
            .peekLeft(x -> taskStore.cancelTransaction())
            .flatMap(this::commit);
      });
   }

   /* TODO should the call to unlinkGenerator on earlier tasks in the series
    * cause the modification timestamp to update on those tasks?
    */
   public Either<TaskAccessException, Task> modifySeries(
      ItemId<Task> taskId,
      GeneratorDelta delta
   ) {
      return taskStore.getTasks().getById(taskId).flatMap(task -> {
         ItemId<Generator> generatorId = task.getGeneratorId();
         if (generatorId == null) {
            String msg = "Cannot modify series on non-series task";
            throw new IllegalStateException(msg);
         }

         removePriorTasksFromGenerator(generatorId, taskId);
         modifyGenerator(generatorId, delta);

         return taskStore.getTasks().getById(taskId)
            .peekLeft(x -> taskStore.cancelTransaction())
            .flatMap(this::commit);
      });
   }

   public Either<TaskAccessException, Task> modifyAndSeverTask(
      ItemId<Task> taskId,
      TaskDelta delta
   ) {
      return taskStore.getTasks().getById(taskId).flatMap(task -> {
         // TODO should the task still sever if the delta is empty?
         if (delta.isEmpty()) {
            return Either.right(task);
         }

         Either<TaskAccessException, Task> severedTask = Either.right(task);
         if (task.getGeneratorId() != null) {
            severedTask = taskStore
               .getGenerators()
               .getById(task.getGeneratorId())
               .flatMap(generator -> {
                  Generator modifiedGenerator = generator.withoutTask(taskId);
                  return taskStore.getGenerators().save(modifiedGenerator);
               })
               .flatMap(x -> {
                  Task modifiedTask = task.withoutGenerator();
                  return taskStore.getTasks().save(modifiedTask);
               });
         }

         return severedTask
            .map(t -> t.withModification(delta))
            .flatMap(taskStore.getTasks()::save)
            .peekLeft(x -> taskStore.cancelTransaction())
            .flatMap(this::commit);
      });
   }

   private Either<TaskAccessException, Generator> removePriorTasksFromGenerator(
      ItemId<Generator> generatorId,
      ItemId<Task> taskId
   ) {
      return taskStore.getGenerators().getById(generatorId).flatMap(generator -> {
         Tuple2<Generator, List<ItemId<Task>>> tuple = generator.withoutTasksBefore(taskId);
         Either<TaskAccessException, Task> result = taskStore
            .getGenerators()
            .save(tuple._1)
            .map(x -> null);

         for (ItemId<Task> id : tuple._2) {
            result = result
               .flatMap(x -> taskStore.getTasks().getById(id))
               .map(Task::withoutGenerator)
               .flatMap(taskStore.getTasks()::save);
         }

         return result.map(x -> tuple._1);
      });
   }

   private Either<TaskAccessException, List<ItemId<Task>>> runGenerator(
      ItemId<Generator> generatorId,
      Instant timestamp
   ) {
      return taskStore.getGenerators().getById(generatorId).flatMap(generator -> {
         Tuple2<Generator, List<Task>> tuple = generator.withTasksUntil(timestamp);

         return taskStore
            .getGenerators()
            .save(tuple._1)
            .flatMap(x -> Either.sequenceRight(tuple._2
               .stream()
               .map(taskStore.getTasks()::save)
               .collect(Collectors.toList())))
            .map(tasks -> tasks.map(Task::getId).asJava());
      });
   }

   private <T> Either<TaskAccessException, T> commit(T t) {
      return taskStore.commit().map(x -> t);
   }
}
