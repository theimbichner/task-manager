package io.github.theimbichner.taskmanager.task;

import io.github.theimbichner.taskmanager.io.TaskAccessResult;
import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.time.DatePattern;

public class GeneratorMutator {
   private final TaskStore taskStore;
   private final ItemId<Generator> generatorId;

   public GeneratorMutator(TaskStore taskStore, ItemId<Generator> generatorId) {
      this.taskStore = taskStore;
      this.generatorId = generatorId;
   }

   public static TaskAccessResult<Generator> createGenerator(
      TaskStore taskStore,
      ItemId<Table> tableId,
      String field,
      DatePattern pattern
   ) {
      return TaskAccessResult.transaction(taskStore, () -> {
         Table table = taskStore.getTables().getById(tableId).get();
         Generator generator = Generator.newGenerator(table, field, pattern);

         Table updatedTable = table.withGenerator(generator.getId());
         updatedTable = taskStore.getTables().save(updatedTable).get();

         return taskStore.getGenerators().save(generator).get();
      });
   }

   public TaskAccessResult<Generator> modifyGenerator(GeneratorDelta delta) {
      return TaskAccessResult.transaction(taskStore, () -> {
         Generator generator = taskStore.getGenerators().getById(generatorId).get();

         for (ItemId<Task> taskId : generator.getTaskIds().asList()) {
            Task task = taskStore.getTasks().getById(taskId).get();
            Task modifiedTask = task.withSeriesModification(delta, generator);
            taskStore.getTasks().save(modifiedTask).checkError();
         }

         Generator modifiedGenerator = generator.withModification(delta);
         return taskStore.getGenerators().save(modifiedGenerator).get();
      });
   }
}
