package io.github.theimbichner.task;

import java.util.List;
import java.util.stream.Stream;
import java.time.Instant;

import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.task.schema.Property;
import io.github.theimbichner.task.schema.PropertyMap;
import io.github.theimbichner.task.time.DateTime;

import static org.assertj.core.api.Assertions.*;

public class TaskTests {
   static DataProvider data;

   @BeforeAll
   static void beforeAll() {
      data = new DataProvider();
   }

   @Test
   void testNewTask() {
      Instant before = Instant.now();
      Task task = data.createDefaultTask();
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
      task.modify(data.getTaskDelta(), false).get();

      assertThat(task.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(task.getName()).isEqualTo(data.getTemplateName());
      assertThat(task.getMarkup()).isEqualTo(data.getMarkup());
      assertThat(task.getProperties().asMap()).containsAll(data.getProperties().asMap());
   }

   @ParameterizedTest
   @MethodSource("provideTasks")
   void testModifyEmpty(Task task) {
      DateTime oldDateLastModified = task.getDateLastModified();
      String oldName = task.getName();
      String oldMarkup = task.getMarkup();
      PropertyMap oldProperties = task.getProperties();

      task.modify(new TaskDelta(PropertyMap.empty(), null, null, null), false).get();

      assertThat(task.getDateLastModified().getStart())
         .isEqualTo(oldDateLastModified.getStart());
      assertThat(task.getDateLastModified().getEnd())
         .isEqualTo(oldDateLastModified.getEnd());

      assertThat(task.getName()).isEqualTo(oldName);
      assertThat(task.getMarkup()).isEqualTo(oldMarkup);
      assertThat(task.getProperties().asMap()).isEqualTo(oldProperties.asMap());
   }

   @ParameterizedTest
   @MethodSource("provideTasks")
   void testModifyPartial(Task task) {
      String oldName = task.getName();
      String oldMarkup = task.getMarkup();

      Instant beforeModify = Instant.now();
      task.modify(new TaskDelta(data.getProperties(), null, null, null)).get();

      assertThat(task.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(task.getName()).isEqualTo(oldName);
      assertThat(task.getMarkup()).isEqualTo(oldMarkup);
   }

   @Test
   void testModifyUpdateProperties() {
      Task task = data.createModifiedTask();

      task.modify(new TaskDelta(data.getUpdateProperties(), null, null, null)).get();
      assertThat(task.getProperties().asMap().keySet()).isEqualTo(HashSet.of("alpha", "gamma"));
      assertThat(task.getProperties().asMap().get("alpha")).contains(Property.of(null));
   }

   @Test
   void testModifyInvalid() {
      Task task = data.createModifiedTask();
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> task.modify(data.getFullTaskDelta(), false));
   }

   @ParameterizedTest
   @MethodSource("provideTasks")
   void testModifyAndSeverInvalid(Task task) {
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> task.modify(data.getFullTaskDelta()));
   }

   @ParameterizedTest
   @MethodSource("provideGeneratorTasks")
   void testModifyUpdateDuration(Task task) {
      String generationField = data.getGenerationField();
      DateTime initial = (DateTime) task.getProperties().asMap().get(generationField).get().get();
      Instant expectedEnd = initial.getStart().plusSeconds(data.getDuration());

      task.modify(data.getFullTaskDelta(), false).get();
      DateTime dateTime = (DateTime) task.getProperties().asMap().get(generationField).get().get();

      assertThat(dateTime.getStart()).isEqualTo(initial.getStart());
      assertThat(dateTime.getEnd()).isEqualTo(expectedEnd);
   }

   @ParameterizedTest
   @MethodSource("provideGeneratorTasks")
   void testModifySeverGenerator(Task task) {
      Instant beforeModify = Instant.now();
      task.modify(data.getTaskDelta()).get();

      assertThat(task.getGeneratorId()).isNull();

      assertThat(task.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(task.getName()).isEqualTo(data.getTemplateName());
      assertThat(task.getMarkup()).isEqualTo(data.getMarkup());
      assertThat(task.getProperties().asMap()).containsAll(data.getProperties().asMap());
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifySeries(Generator generator) {
      Instant instant = Instant.now().plusSeconds(600);
      List<String> tasks = Orchestration.runGenerator(generator, instant).get();
      int index = tasks.size() / 2;
      Task targetTask = data.getTaskStore().getTasks().getById(tasks.get(index)).get();
      targetTask.modifySeries(data.getFullGeneratorDelta()).get();

      String generationField = data.getGenerationField();

      for (int i = 0; i < tasks.size(); i++) {
         Task task = data.getTaskStore().getTasks().getById(tasks.get(i)).get();
         if (i < index) {
            assertThat(task.getGeneratorId()).isNull();
         }
         else {
            assertThat(task.getName()).isEqualTo(data.getTemplateName());
            assertThat(task.getMarkup()).isEqualTo(data.getMarkup());
            assertThat(task.getProperties().asMap().remove(generationField))
               .isEqualTo(data.getProperties().asMap());
            assertThat(task.getProperties().asMap().containsKey(generationField)).isTrue();
         }
      }
   }

   @Test
   void testModifySeriesInvalid() {
      Task task = data.createModifiedTask();
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> task.modifySeries(data.getFullGeneratorDelta()));
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
      // TODO reenable tests after implementing properties
      // for (String s : newTask.getPropertyNames()) {
      //    assertThat(newTask.getProperty(s)).isEqualTo(task.getProperty(s));
      // }
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
}
