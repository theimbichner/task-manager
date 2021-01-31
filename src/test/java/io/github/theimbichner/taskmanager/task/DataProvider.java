package io.github.theimbichner.taskmanager.task;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import io.github.theimbichner.taskmanager.io.InMemoryDataStore;
import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.time.DatePattern;
import io.github.theimbichner.taskmanager.time.DateTime;
import io.github.theimbichner.taskmanager.time.UniformDatePattern;

public class DataProvider {
   private final TaskStore taskStore;
   private final String tableId;
   private final String generationField;
   private final DatePattern datePattern;

   private final String deltaName;
   private final String deltaTemplateName;
   private final String deltaTemplateMarkup;
   private final long deltaTemplateDuration;

   public DataProvider() {
      taskStore = InMemoryDataStore.createTaskStore();

      tableId = Orchestration.createTable(taskStore).get().getId();

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
      return taskStore.getTables().getById(tableId).get();
   }

   public String getGenerationField() {
      return generationField;
   }

   public DatePattern getGenerationDatePattern() {
      return datePattern;
   }

   public PropertyMap getProperties() {
      return PropertyMap.fromJava(Map.of(
         "alpha", Property.ofNumber("1"),
         "beta", Property.of(""),
         "gamma", Property.of(new DateTime(Instant.ofEpochSecond(12345)))));
   }

   public PropertyMap getUpdateProperties() {
      return PropertyMap.fromJava(Map.of(
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
      return Orchestration.createTask(getTable()).get();
   }

   public Task createModifiedTask() {
      Task task = createDefaultTask();
      return Orchestration.modifyAndSeverTask(task, getTaskDelta()).get();
   }

   public Task createDefaultTaskWithGenerator() {
      Generator generator = createModifiedGenerator();
      Orchestration.getTasksFromTable(getTable(), Instant.now().plusSeconds(600)).get();
      generator = taskStore.getGenerators().getById(generator.getId()).get();

      String taskId = generator.getTaskIds().asList().get(0);
      return taskStore.getTasks().getById(taskId).get();
   }

   public Task createModifiedTaskWithGenerator() {
      Generator generator = createDefaultGenerator();
      Orchestration.getTasksFromTable(getTable(), Instant.now().plusSeconds(600)).get();
      generator = taskStore.getGenerators().getById(generator.getId()).get();

      String taskId = generator.getTaskIds().asList().get(0);
      Task task = taskStore.getTasks().getById(taskId).get();

      return Orchestration.modifySeries(task, getFullGeneratorDelta()).get();
   }

   public TaskDelta getTaskDelta() {
      return new TaskDelta(
         getProperties(),
         deltaTemplateName,
         deltaTemplateMarkup,
         null);
   }

   public TaskDelta getFullTaskDelta() {
      return new TaskDelta(
         getProperties(),
         deltaTemplateName,
         deltaTemplateMarkup,
         deltaTemplateDuration);
   }

   public Generator createDefaultGenerator() {
      return Orchestration.createGenerator(getTable(), generationField, datePattern).get();
   }

   public Generator createModifiedGenerator() {
      Generator generator = createDefaultGenerator();
      GeneratorDelta delta = getFullGeneratorDelta();
      return Orchestration.modifyGenerator(generator, delta).get();
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
