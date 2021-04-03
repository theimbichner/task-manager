package io.github.theimbichner.taskmanager.task;

import java.time.Duration;
import java.time.Instant;

import io.vavr.collection.HashMap;

import io.github.theimbichner.taskmanager.io.InMemoryDataStore;
import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.time.DatePattern;
import io.github.theimbichner.taskmanager.time.DateTime;
import io.github.theimbichner.taskmanager.time.UniformDatePattern;

public class DataProvider {
   private final TaskStore taskStore;
   private final Orchestration orchestrator;
   private final ItemId<Table> tableId;
   private final TableMutator tableMutator;
   private final String generationField;
   private final DatePattern datePattern;

   private final String deltaName;
   private final String deltaTemplateName;
   private final String deltaTemplateMarkup;
   private final long deltaTemplateDuration;

   public DataProvider() {
      taskStore = InMemoryDataStore.createTaskStore();
      orchestrator = new Orchestration(taskStore);

      tableId = TableMutator.createTable(taskStore).asEither().get().getId();
      tableMutator = new TableMutator(taskStore, tableId);

      generationField = "";
      datePattern = new UniformDatePattern(
         Instant.ofEpochSecond(5),
         Duration.ofSeconds(7));

      deltaName = "delta";
      deltaTemplateName = "epsilon";
      deltaTemplateMarkup = "zeta";
      deltaTemplateDuration = 600;
   }

   public TaskStore getTaskStore() {
      return taskStore;
   }

   public Table getTable() {
      return taskStore.getTables().getById(tableId).asEither().get();
   }

   public String getGenerationField() {
      return generationField;
   }

   public DatePattern getGenerationDatePattern() {
      return datePattern;
   }

   public PropertyMap getProperties() {
      return PropertyMap.of(HashMap.of(
         "alpha", Property.ofNumber("1"),
         "beta", Property.of(""),
         "gamma", Property.of(new DateTime(Instant.ofEpochSecond(12345)))));
   }

   public PropertyMap getUpdateProperties() {
      return PropertyMap.of(HashMap.of(
         // modify
         "alpha", Property.empty(),
         // delete
         "beta", Property.DELETE,
         // delete nonexistent
         "delta", Property.DELETE));
   }

   public String getName() {
      return deltaName;
   }

   public String getTemplateName() {
      return deltaTemplateName;
   }

   public String getMarkup() {
      return deltaTemplateMarkup;
   }

   public long getDuration() {
      return deltaTemplateDuration;
   }

   public Task createDefaultTask() {
      return orchestrator.createTask(tableId).get();
   }

   public Task createModifiedTask() {
      ItemId<Task> taskId = createDefaultTask().getId();
      return orchestrator.modifyAndSeverTask(taskId, getTaskDelta()).get();
   }

   public Task createDefaultTaskWithGenerator() {
      Generator generator = createModifiedGenerator();
      tableMutator.getTasksFromTable(Instant.now().plusSeconds(600)).asEither().get();
      generator = taskStore.getGenerators().getById(generator.getId()).asEither().get();

      ItemId<Task> taskId = generator.getTaskIds().asList().get(0);
      return taskStore.getTasks().getById(taskId).asEither().get();
   }

   public Task createModifiedTaskWithGenerator() {
      Generator generator = createDefaultGenerator();
      tableMutator.getTasksFromTable(Instant.now().plusSeconds(600)).asEither().get();
      generator = taskStore.getGenerators().getById(generator.getId()).asEither().get();

      ItemId<Task> taskId = generator.getTaskIds().asList().get(0);
      return orchestrator.modifySeries(taskId, getFullGeneratorDelta()).get();
   }

   public TaskDelta getTaskDelta() {
      return new TaskDelta(
         getProperties(),
         deltaTemplateName,
         deltaTemplateMarkup);
   }

   public Generator createDefaultGenerator() {
      return GeneratorMutator
         .createGenerator(taskStore, tableId, generationField, datePattern)
         .asEither()
         .get();
   }

   public Generator createModifiedGenerator() {
      Generator generator = createDefaultGenerator();
      GeneratorDelta delta = getFullGeneratorDelta();
      GeneratorMutator mutator = new GeneratorMutator(taskStore, generator.getId());
      return mutator.modifyGenerator(delta).asEither().get();
   }

   public GeneratorDelta getFullGeneratorDelta() {
      return new GeneratorDelta(
         getProperties(),
         deltaName,
         deltaTemplateName,
         deltaTemplateMarkup,
         deltaTemplateDuration);
   }
}
