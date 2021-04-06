package io.github.theimbichner.taskmanager.task;

import io.vavr.Tuple2;
import io.vavr.collection.Vector;
import io.vavr.control.Either;

import io.github.theimbichner.taskmanager.io.TaskAccessException;
import io.github.theimbichner.taskmanager.io.TaskStore;

// TODO don't save items when unnecessary

public class Orchestration {
   private final TaskStore taskStore;

   public Orchestration(TaskStore taskStore) {
      this.taskStore = taskStore;
   }

   public Either<TaskAccessException, Task> createTask(ItemId<Table> tableId) {
      return taskStore.getTables().getById(tableId).asEither().flatMap(table -> {
         Task task = Task.newTask(table);
         Table updatedTable = table.withTasks(Vector.of(task.getId()));

         return taskStore.getTables().save(updatedTable).asEither()
            .<Task>flatMap(x -> taskStore.getTasks().save(task).asEither())
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
      return taskStore.getTasks().getById(taskId).asEither().flatMap(task -> {
         ItemId<Generator> generatorId = task.getGeneratorId();
         if (generatorId == null) {
            String msg = "Cannot modify series on non-series task";
            throw new IllegalStateException(msg);
         }

         GeneratorMutator generatorMutator = new GeneratorMutator(taskStore, generatorId);

         return removePriorTasksFromGenerator(generatorId, taskId)
            .flatMap(x -> generatorMutator.modifyGenerator(delta).asEither())
            .flatMap(x -> taskStore.getTasks().getById(taskId).asEither())
            .peekLeft(x -> taskStore.cancelTransaction())
            .flatMap(this::commit);
      });
   }

   public Either<TaskAccessException, Task> modifyAndSeverTask(
      ItemId<Task> taskId,
      TaskDelta delta
   ) {
      return taskStore.getTasks().getById(taskId).asEither().flatMap(task -> {
         // TODO should the task still sever if the delta is empty?
         if (delta.isEmpty()) {
            return Either.right(task);
         }

         Either<TaskAccessException, Task> severedTask = Either.right(task);
         if (task.getGeneratorId() != null) {
            severedTask = taskStore
               .getGenerators()
               .getById(task.getGeneratorId())
               .asEither()
               .flatMap(generator -> {
                  Generator modifiedGenerator = generator.withoutTask(taskId);
                  return taskStore.getGenerators().save(modifiedGenerator).asEither();
               })
               .flatMap(x -> {
                  Task modifiedTask = task.withoutGenerator();
                  return taskStore.getTasks().save(modifiedTask).asEither();
               });
         }

         return severedTask
            .map(t -> t.withModification(delta))
            .flatMap(t -> taskStore.getTasks().save(t).asEither())
            .peekLeft(x -> taskStore.cancelTransaction())
            .flatMap(this::commit);
      });
   }

   private Either<TaskAccessException, Generator> removePriorTasksFromGenerator(
      ItemId<Generator> generatorId,
      ItemId<Task> taskId
   ) {
      return taskStore.getGenerators().getById(generatorId).asEither().flatMap(generator -> {
         Tuple2<Generator, Vector<ItemId<Task>>> tuple = generator.withoutTasksBefore(taskId);
         Either<TaskAccessException, Task> result = taskStore
            .getGenerators()
            .save(tuple._1)
            .asEither()
            .map(x -> null);

         for (ItemId<Task> id : tuple._2) {
            result = result
               .flatMap(x -> taskStore.getTasks().getById(id).asEither())
               .map(Task::withoutGenerator)
               .flatMap(t -> taskStore.getTasks().save(t).asEither());
         }

         return result.map(x -> tuple._1);
      });
   }

   private <T> Either<TaskAccessException, T> commit(T t) {
      return taskStore.commit().asEither().map(x -> t);
   }
}
