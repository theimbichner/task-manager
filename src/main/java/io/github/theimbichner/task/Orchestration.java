package io.github.theimbichner.task;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import io.vavr.Tuple2;
import io.vavr.control.Either;

import io.github.theimbichner.task.io.TaskAccessException;
import io.github.theimbichner.task.io.TaskStore;

public class Orchestration {
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
               .flatMap(t -> Orchestration.modifyTask(t, taskDelta)))
            .collect(Collectors.toList()))
         .flatMap(x -> {
            Generator modified = generator.withModification(delta);
            return taskStore.getGenerators().save(modified);
         });
   }

   public static Either<TaskAccessException, Task> modifyTask(Task task, TaskDelta delta) {
      TaskStore taskStore = task.getTaskStore();
      return task.withModification(delta)
         .flatMap(t -> taskStore.getTasks().save(t));
   }

   public static Either<TaskAccessException, Task> modifyAndSeverTask(Task task, TaskDelta delta) {
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

   /* TODO should the call to unlinkGenerator on earlier tasks in the series
    * cause the modification timestamp to update on those tasks?
    */
   public static Either<TaskAccessException, Task> modifySeries(Task task, GeneratorDelta delta) {
      return task.getGenerator()
         .map(g -> g.getOrElse(() -> {
            String msg = "Cannot modify series on non series task";
            throw new IllegalStateException(msg);
         }))
         .flatMap(g -> removeTasksFromGeneratorBefore(g, task.getId()))
         .flatMap(g -> Orchestration.modifyGenerator(g, delta))
         .map(g -> task);
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
         .map(x -> (Task) null);

      for (String s : split._2) {
         result = result
            .flatMap(x -> taskStore.getTasks().getById(s))
            .map(Task::withoutGenerator)
            .flatMap(taskStore.getTasks()::save);
      }

      return result.map(x -> split._1);
   }

   public static Either<TaskAccessException, List<String>> runGenerator(
      Generator generator,
      Instant timestamp
   ) {
      TaskStore taskStore = generator.getTaskStore();
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
