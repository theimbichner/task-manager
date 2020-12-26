package io.github.theimbichner.task;

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
               .flatMap(t -> t.modify(taskDelta, false)))
            .collect(Collectors.toList()))
         .flatMap(x -> {
            Generator modified = generator.withModification(delta);
            return taskStore.getGenerators().save(modified);
         });
   }

   public static Either<TaskAccessException, Generator> removeTasksFromGeneratorBefore(
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
            .peek(task -> task.unlinkGenerator());
      }

      return result.map(x -> split._1);
   }
}
