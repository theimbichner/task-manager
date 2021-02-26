package io.github.theimbichner.taskmanager.task;

import java.util.stream.Stream;
import java.time.Instant;

import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.time.DatePattern;
import io.github.theimbichner.taskmanager.time.DateTime;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.vavr.api.VavrAssertions.*;

public class TaskTests {
   static DataProvider data;

   @BeforeAll
   static void beforeAll() {
      data = new DataProvider();
   }

   @Test
   void testNewTask() {
      Instant before = Instant.now();
      Task task = Task.newTask(data.getTable());
      Instant after = Instant.now();

      assertThat(task.getName()).isEqualTo("");

      assertThat(task.getDateCreated().getStart())
         .isAfterOrEqualTo(before)
         .isBeforeOrEqualTo(after)
         .isEqualTo(task.getDateCreated().getEnd());
      assertThat(task.getDateLastModified().getStart())
         .isEqualTo(task.getDateCreated().getStart())
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(task.getMarkup()).isEqualTo("");
      assertThat(task.getGeneratorId()).isNull();
      assertThat(task.getProperties().asMap()).isEmpty();
   }

   private static Stream<Task> provideTasks() {
      return Stream.of(
         data.createDefaultTask(),
         data.createModifiedTask(),
         data.createDefaultTaskWithGenerator(),
         data.createModifiedTaskWithGenerator());
   }

   private static Stream<Task> provideGeneratorTasks() {
      return Stream.of(
         data.createDefaultTaskWithGenerator(),
         data.createModifiedTaskWithGenerator());
   }

   private static Stream<Generator> provideGenerators() {
      return Stream.of(
         data.createDefaultGenerator(),
         data.createModifiedGenerator());
   }

   @ParameterizedTest
   @MethodSource("provideTasks")
   void testModify(Task task) {
      Instant beforeModify = Instant.now();
      task = task.withModification(data.getTaskDelta());

      assertThat(task.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(task.getName()).isEqualTo(data.getTemplateName());
      assertThat(task.getMarkup()).isEqualTo(data.getMarkup());
      assertThat(task.getProperties().asMap())
         .containsAllEntriesOf(data.getProperties().asMap());
   }

   @ParameterizedTest
   @MethodSource("provideTasks")
   void testModifyEmpty(Task task) {
      TaskDelta delta = new TaskDelta(PropertyMap.empty(), null, null);
      assertThat(task.withModification(delta))
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(task);
   }

   @ParameterizedTest
   @MethodSource("provideTasks")
   void testModifyPartial(Task task) {
      String oldName = task.getName();
      String oldMarkup = task.getMarkup();

      Instant beforeModify = Instant.now();
      TaskDelta delta = new TaskDelta(data.getProperties(), null, null);
      task = task.withModification(delta);

      assertThat(task.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(task.getName()).isEqualTo(oldName);
      assertThat(task.getMarkup()).isEqualTo(oldMarkup);
   }

   @Test
   void testModifyUpdateProperties() {
      Task task = data.createModifiedTask();
      TaskDelta delta = new TaskDelta(data.getUpdateProperties(), null, null);
      task = task.withModification(delta);

      assertThat(task.getProperties().asMap().keySet()).isEqualTo(HashSet.of("alpha", "gamma"));
      assertThat(task.getProperties().asMap().get("alpha")).contains(Property.empty());
   }

   @ParameterizedTest
   @MethodSource("provideTasks")
   void testToFromJson(Task task) {
      Task newTask = Task.fromJson(task.toJson());

      assertThat(newTask.getId()).isEqualTo(task.getId());
      assertThat(newTask.getName()).isEqualTo(task.getName());

      assertThat(newTask.getDateCreated().getStart())
         .isEqualTo(task.getDateCreated().getStart());
      assertThat(newTask.getDateCreated().getEnd())
         .isEqualTo(task.getDateCreated().getEnd());
      assertThat(newTask.getDateLastModified().getStart())
         .isEqualTo(task.getDateLastModified().getStart());
      assertThat(newTask.getDateLastModified().getEnd())
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(newTask.getMarkup()).isEqualTo(task.getMarkup());
      assertThat(newTask.getGeneratorId()).isEqualTo(task.getGeneratorId());
      assertThat(newTask.getProperties().asMap()).isEqualTo(task.getProperties().asMap());
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testNewSeriesTask(Generator generator) {
      Instant instant = Instant.now();
      Task task = Task.newSeriesTask(generator, instant);
      String generationField = generator.getGenerationField();

      assertThat(task.getName()).isEqualTo(generator.getTemplateName());
      assertThat(task.getMarkup()).isEqualTo(generator.getTemplateMarkup());
      assertThat(task.getGeneratorId()).isEqualTo(generator.getId());

      DateTime date = new DateTime(instant).withDuration(generator.getTemplateDuration());
      HashMap<String, Property> expectedProperties = generator.getTemplateProperties().asMap();
      expectedProperties = expectedProperties.put(generationField, Property.of(date));

      assertThat(task.getProperties().asMap()).isEqualTo(expectedProperties);
   }

   @Test
   void testWithSeriesModification() {
      Instant instant = Instant.now();
      DatePattern pattern = data.getGenerationDatePattern();

      Table table = Table.newTable();
      Generator generator = Generator.newGenerator(table, "alpha", pattern);
      Task task = Task.newSeriesTask(generator, instant);
      GeneratorDelta delta = new GeneratorDelta(
         PropertyMap.empty()
            .put("alpha", Property.ofNumber("12345"))
            .put("beta", Property.of("abcde")),
         "generator name",
         "task name",
         "markup",
         10L);

      Task result = task.withSeriesModification(delta, generator);

      assertThat(result.getProperties().asMap()).isEqualTo(HashMap.of(
         "alpha", Property.of(new DateTime(instant, instant.plusSeconds(10))),
         "beta", Property.of("abcde")));
      assertThat(result.getName()).isEqualTo("task name");
      assertThat(result.getMarkup()).isEqualTo("markup");
   }

   @Test
   void testWithSeriesModificationInvalid() {
      DatePattern pattern = data.getGenerationDatePattern();
      GeneratorDelta delta = new GeneratorDelta(
         PropertyMap.empty(),
         null,
         null,
         null,
         null);

      Table table = Table.newTable();
      Task task = Task.newTask(table);
      Generator generator = Generator.newGenerator(table, "alpha", pattern);

      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> task.withSeriesModification(delta, generator));
   }
}
