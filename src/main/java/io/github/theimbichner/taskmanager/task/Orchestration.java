package io.github.theimbichner.taskmanager.task;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import io.vavr.Tuple2;
import io.vavr.control.Either;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.io.Storable;
import io.github.theimbichner.taskmanager.io.TaskAccessException;
import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.time.DatePattern;

// TODO verify that orchestration correctly saves updates in every method

// TODO don't save items when unnecessary

public class Orchestration {
   private Orchestration() {}

   public static Either<TaskAccessException, Table> createTable(TaskStore taskStore) {
      Table table = Table.newTable();
      table.setTaskStore(taskStore);
      return taskStore.getTables().save(table);
   }

   public static Either<TaskAccessException, Task> createTask(Table table) {
      TaskStore taskStore = table.getTaskStore();

      Task task = Task.newTask(table);
      Table updatedTable = table.withTasks(List.of(task.getId()));

      return taskStore.getTables().save(updatedTable)
         .flatMap(x -> taskStore.getTasks().save(task));
   }

   public static Either<TaskAccessException, Generator> createGenerator(
      Table table,
      String field,
      DatePattern pattern
   ) {
      TaskStore taskStore = table.getTaskStore();

      Generator generator = Generator.newGenerator(table, field, pattern);
      Table updatedTable = table.withGenerator(generator.getId());

      return taskStore.getTables().save(updatedTable)
         .flatMap(x -> taskStore.getGenerators().save(generator));
   }

   public static Either<TaskAccessException, SetList<String>> getTasksFromTable(
      Table table,
      Instant timestamp
   ) {
      TaskStore taskStore = table.getTaskStore();

      Either<TaskAccessException, Table> result = Either.right(table);
      for (String id : table.getAllGeneratorIds().asList()) {
         result = result.flatMap(resultTable -> taskStore
            .getGenerators().getById(id)
            .flatMap(g -> runGenerator(g, timestamp))
            .map(resultTable::withTasks));
      }
      return result
         .flatMap(taskStore.getTables()::save)
         .map(Table::getAllTaskIds);
   }

   public static Either<TaskAccessException, Table> modifyTable(
      Table table,
      TableDelta delta
   ) {
      TaskStore taskStore = table.getTaskStore();
      Either<TaskAccessException, Storable> result = Either.right(null);

      for (String s : table.getAllTaskIds().asList()) {
         result = result
            .flatMap(x -> taskStore.getTasks().getById(s))
            .flatMap(task -> task.withModification(delta.asTaskDelta(task.getProperties())))
            .flatMap(task -> taskStore.getTasks().save(task));
      }

      for (String s : table.getAllGeneratorIds().asList()) {
         result = result
            .flatMap(x -> taskStore.getGenerators().getById(s))
            .map(g -> g.adjustToSchema(delta.getSchema()))
            .flatMap(generator -> taskStore.getGenerators().save(generator));
      }

      return result.flatMap(x -> taskStore.getTables().save(table.withModification(delta)));
   }

   public static Either<TaskAccessException, Generator> modifyGenerator(
      Generator generator,
      GeneratorDelta delta
   ) {
      TaskStore taskStore = generator.getTaskStore();
      TaskDelta taskDelta = delta.asTaskDelta();

      return Either
         .sequenceRight(generator.getTaskIds().asList().stream()
            .map(id -> taskStore
               .getTasks().getById(id)
               .flatMap(t -> modifyTask(t, taskDelta)))
            .collect(Collectors.toList()))
         .flatMap(x -> {
            Generator modified = generator.withModification(delta);
            return taskStore.getGenerators().save(modified);
         });
   }

   /* TODO should the call to unlinkGenerator on earlier tasks in the series
    * cause the modification timestamp to update on those tasks?
    */
   public static Either<TaskAccessException, Task> modifySeries(Task task, GeneratorDelta delta) {
      TaskStore taskStore = task.getTaskStore();

      return task.getGenerator()
         .map(g -> g.getOrElse(() -> {
            String msg = "Cannot modify series on non series task";
            throw new IllegalStateException(msg);
         }))
         .flatMap(g -> removeTasksFromGeneratorBefore(g, task.getId()))
         .flatMap(g -> modifyGenerator(g, delta))
         .flatMap(g -> taskStore.getTasks().getById(task.getId()));
   }

   public static Either<TaskAccessException, Task> modifyAndSeverTask(Task task, TaskDelta delta) {
      // TODO should the task still sever if the delta is empty?
      if (delta.isEmpty()) {
         return Either.right(task);
      }

      TaskStore taskStore = task.getTaskStore();

      return task.getGenerator()
         .flatMap(generator -> {
            if (generator.isEmpty()) {
               return Either.right(task);
            }
            return taskStore.getGenerators()
               .save(generator.get().withoutTask(task.getId()))
               .map(x -> task.withoutGenerator())
               .flatMap(taskStore.getTasks()::save);
         })
         .flatMap(t -> t.withModification(delta))
         .flatMap(taskStore.getTasks()::save);
   }

   private static Either<TaskAccessException, Task> modifyTask(Task task, TaskDelta delta) {
      TaskStore taskStore = task.getTaskStore();
      return task.withModification(delta)
         .flatMap(t -> taskStore.getTasks().save(t));
   }

   private static Either<TaskAccessException, Generator> removeTasksFromGeneratorBefore(
      Generator generator,
      String taskId
   ) {
      TaskStore taskStore = generator.getTaskStore();

      Tuple2<Generator, List<String>> split = generator.withoutTasksBefore(taskId);
      Either<TaskAccessException, Task> result = taskStore
         .getGenerators()
         .save(split._1)
         .map(x -> null);

      for (String s : split._2) {
         result = result
            .flatMap(x -> taskStore.getTasks().getById(s))
            .map(Task::withoutGenerator)
            .flatMap(taskStore.getTasks()::save);
      }

      return result.map(x -> split._1);
   }

   private static Either<TaskAccessException, List<String>> runGenerator(
      Generator generator,
      Instant timestamp
   ) {
      TaskStore taskStore = generator.getTaskStore();
      // TODO why is this named split
      Tuple2<Generator, List<Task>> split = generator.withTasksUntil(timestamp);

      return taskStore.getGenerators()
         .save(split._1)
         .flatMap(x -> Either.sequenceRight(split._2
            .stream()
            .map(taskStore.getTasks()::save)
            .collect(Collectors.toList())))
         .map(tasks -> tasks.map(Task::getId).asJava());
   }
}
