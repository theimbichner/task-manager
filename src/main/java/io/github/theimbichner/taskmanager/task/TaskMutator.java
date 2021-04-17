package io.github.theimbichner.taskmanager.task;

import io.vavr.Tuple2;
import io.vavr.collection.Vector;

import io.github.theimbichner.taskmanager.io.TaskAccessResult;
import io.github.theimbichner.taskmanager.io.TaskStore;

// TODO don't save items when unnecessary

public class TaskMutator {
   private final TaskStore taskStore;
   private final ItemId<Task> taskId;

   public TaskMutator(TaskStore taskStore, ItemId<Task> taskId) {
      this.taskStore = taskStore;
      this.taskId = taskId;
   }

   public static TaskAccessResult<Task> createTask(TaskStore taskStore, ItemId<Table> tableId) {
      return TaskAccessResult.transaction(taskStore, () -> {
         Table table = taskStore.getTables().getById(tableId).get();
         Task task = Task.newTask(table);
         Table updatedTable = table.withTasks(Vector.of(task.getId()));

         taskStore.getTables().save(updatedTable).checkError();
         return taskStore.getTasks().save(task).get();
      });
   }

   /* TODO should the call to unlinkGenerator on earlier tasks in the series
    * cause the modification timestamp to update on those tasks?
    */
   public TaskAccessResult<Task> modifySeries(GeneratorDelta delta) {
      return TaskAccessResult.transaction(taskStore, () -> {
         Task task = taskStore.getTasks().getById(taskId).get();
         ItemId<Generator> generatorId = task.getGeneratorId();
         if (generatorId == null) {
            String msg = "Cannot modify series on non-series task";
            throw new IllegalStateException(msg);
         }

         removePriorTasksFromGenerator(generatorId, taskId).checkError();

         GeneratorMutator generatorMutator = new GeneratorMutator(taskStore, generatorId);
         generatorMutator.modifyGenerator(delta).checkError();

         return taskStore.getTasks().getById(taskId).get();
      });
   }

   public TaskAccessResult<Task> modifyAndSeverTask(TaskDelta delta) {
      return TaskAccessResult.transaction(taskStore, () -> {
         Task task = taskStore.getTasks().getById(taskId).get();

         // TODO should the task still sever if the delta is empty?
         if (delta.isEmpty()) {
            return task;
         }

         if (task.getGeneratorId() != null) {
            Generator generator = taskStore.getGenerators().getById(task.getGeneratorId()).get();
            taskStore.getGenerators().save(generator.withoutTask(taskId)).checkError();
            task = taskStore.getTasks().save(task.withoutGenerator()).get();
         }

         Task modifiedTask = task.withModification(delta);
         return taskStore.getTasks().save(modifiedTask).get();
      });
   }

   private TaskAccessResult<Generator> removePriorTasksFromGenerator(
      ItemId<Generator> generatorId,
      ItemId<Task> targetTaskId
   ) {
      return TaskAccessResult.of(() -> {
         Generator generator = taskStore.getGenerators().getById(generatorId).get();
         Tuple2<Generator, Vector<ItemId<Task>>> tuple = generator.withoutTasksBefore(targetTaskId);

         for (ItemId<Task> id : tuple._2) {
            Task task = taskStore.getTasks().getById(id).get();
            Task modifiedTask = task.withoutGenerator();
            taskStore.getTasks().save(modifiedTask).checkError();
         }

         return taskStore.getGenerators().save(tuple._1).get();
      });
   }
}
