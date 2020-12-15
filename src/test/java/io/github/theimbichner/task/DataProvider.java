package io.github.theimbichner.task;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import io.github.theimbichner.task.io.InMemoryDataStore;
import io.github.theimbichner.task.io.TaskAccessException;
import io.github.theimbichner.task.io.TaskStore;
import io.github.theimbichner.task.schema.Property;
import io.github.theimbichner.task.time.DatePattern;
import io.github.theimbichner.task.time.DateTime;
import io.github.theimbichner.task.time.UniformDatePattern;

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
      table.registerTaskStore(taskStore);

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

   public Map<String, Property> getProperties() {
      return Map.of(
         "alpha", Property.of(1),
         "beta", Property.of(""),
         "gamma", Property.of(new DateTime(Instant.ofEpochSecond(12345))));
   }

   public Map<String, Property> getUpdateProperties() {
      return Map.of(
         // modify
         "alpha", Property.of(null),
         // delete
         "beta", Property.DELETE,
         // delete nonexistent
         "delta", Property.DELETE);
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
      return Task.createTask(table);
   }

   public Task createModifiedTask() throws TaskAccessException {
      Task task = createDefaultTask();
      task.modify(getTaskDelta());
      return task;
   }

   public Task createDefaultTaskWithGenerator() throws TaskAccessException {
      Generator generator = createModifiedGenerator();
      return Task.newSeriesTask(generator, Instant.now());
   }

   public Task createModifiedTaskWithGenerator() throws TaskAccessException {
      Generator generator = createDefaultGenerator();
      Task task = Task.newSeriesTask(generator, Instant.now());
      task.modify(getFullTaskDelta(), false);
      return task;
   }

   public TaskDelta getTaskDelta() {
      return new TaskDelta(
         getProperties(),
         deltaTemplateName,
         Optional.of(deltaTemplateMarkup),
         null);
   }

   public TaskDelta getFullTaskDelta() {
      return new TaskDelta(
         getProperties(),
         deltaTemplateName,
         Optional.of(deltaTemplateMarkup),
         deltaTemplateDuration);
   }

   public Generator createDefaultGenerator() throws TaskAccessException {
      Generator generator = Generator.createGenerator(table, generationField, datePattern);
      taskStore.getGenerators().save(generator);
      return generator;
   }

   public Generator createModifiedGenerator() throws TaskAccessException {
      Generator generator = createDefaultGenerator();
      generator.modify(getFullGeneratorDelta());
      taskStore.getGenerators().save(generator);
      return generator;
   }

   public GeneratorDelta getFullGeneratorDelta() {
      return new GeneratorDelta(
         getProperties(),
         deltaName,
         deltaTemplateName,
         Optional.of(deltaTemplateMarkup),
         deltaTemplateDuration);
   }
}
