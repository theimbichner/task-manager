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
   private final Table table;
   private final String generationField;
   private final DatePattern datePattern;

   private final String deltaName;
   private final String deltaTemplateName;
   private final String deltaTemplateMarkup;
   private final long deltaTemplateDuration;

   public DataProvider() {
      taskStore = InMemoryDataStore.createTaskStore();

      table = Table.createTable();
      table.setTaskStore(taskStore);

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
      return table;
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
      Task task = Task.createTask(table);
      return taskStore.getTasks().save(task).get();
   }

   public Task createModifiedTask() {
      Task task = createDefaultTask();
      return Orchestration.modifyAndSeverTask(task, getTaskDelta()).get();
   }

   public Task createDefaultTaskWithGenerator() {
      Generator generator = createModifiedGenerator();
      Task task = Task.newSeriesTask(generator, Instant.now());
      return taskStore.getTasks().save(task).get();
   }

   public Task createModifiedTaskWithGenerator() {
      Generator generator = createDefaultGenerator();
      Task task = Task.newSeriesTask(generator, Instant.now());
      return Orchestration.modifyTask(task, getFullTaskDelta()).get();
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
      Generator generator = Generator.createGenerator(table, generationField, datePattern);
      taskStore.getGenerators().save(generator).get();
      return generator;
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
