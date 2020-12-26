package io.github.theimbichner.task;

import java.util.stream.Collectors;

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
}
