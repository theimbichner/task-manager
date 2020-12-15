package io.github.theimbichner.task;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.task.io.TaskAccessException;
import io.github.theimbichner.task.schema.Property;
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

      assertThat(task.getMarkup()).isNull();
      assertThat(task.getGeneratorId()).isNull();
      assertThat(task.getPropertyNames()).isEqualTo(Set.of());
   }

   private static Stream<Task> provideTasks() throws TaskAccessException {
      return Stream.of(
         data.createDefaultTask(),
         data.createModifiedTask(),
         data.createDefaultTaskWithGenerator(),
         data.createModifiedTaskWithGenerator());
   }

   private static Stream<Task> provideGeneratorTasks() throws TaskAccessException {
      return Stream.of(
         data.createDefaultTaskWithGenerator(),
         data.createModifiedTaskWithGenerator());
   }

   private static Stream<Generator> provideGenerators() throws TaskAccessException {
      return Stream.of(
         data.createDefaultGenerator(),
         data.createModifiedGenerator());
   }

   @ParameterizedTest
   @MethodSource("provideTasks")
   void testModify(Task task) throws TaskAccessException {
      Instant beforeModify = Instant.now();
      task.modify(data.getTaskDelta(), false);

      assertThat(task.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(task.getName()).isEqualTo(data.getTemplateName());
      assertThat(task.getMarkup()).isEqualTo(data.getMarkup());
      for (String s : data.getProperties().keySet()) {
         assertThat(task.getProperty(s)).isEqualTo(data.getProperties().get(s));
      }
   }

   @ParameterizedTest
   @MethodSource("provideTasks")
   void testModifyEmpty(Task task) throws TaskAccessException {
      DateTime oldDateLastModified = task.getDateLastModified();
      String oldName = task.getName();
      String oldMarkup = task.getMarkup();
      Set<String> oldPropertyNames = task.getPropertyNames();

      task.modify(new TaskDelta(Map.of(), null, null, null), false);

      assertThat(task.getDateLastModified().getStart())
         .isEqualTo(oldDateLastModified.getStart());
      assertThat(task.getDateLastModified().getEnd())
         .isEqualTo(oldDateLastModified.getEnd());

      assertThat(task.getName()).isEqualTo(oldName);
      assertThat(task.getMarkup()).isEqualTo(oldMarkup);
      assertThat(task.getPropertyNames()).isEqualTo(oldPropertyNames);
   }

   @ParameterizedTest
   @MethodSource("provideTasks")
   void testModifyPartial(Task task) throws TaskAccessException {
      String oldName = task.getName();
      String oldMarkup = task.getMarkup();

      Instant beforeModify = Instant.now();
      task.modify(new TaskDelta(data.getProperties(), null, null, null));

      assertThat(task.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(task.getName()).isEqualTo(oldName);
      assertThat(task.getMarkup()).isEqualTo(oldMarkup);
   }

   @Test
   void testModifyUpdateProperties() throws TaskAccessException {
      Task task = data.createModifiedTask();
      Set<String> expectedPropertyNames = Set.of("alpha", "gamma");

      task.modify(new TaskDelta(data.getUpdateProperties(), null, null, null));
      assertThat(task.getPropertyNames()).isEqualTo(expectedPropertyNames);
      assertThat(task.getProperty("alpha")).isEqualTo(Property.of(null));
      assertThat(task.getProperty("beta")).isNull();
      assertThat(task.getProperty("delta")).isNull();
   }

   @Test
   void testModifyInvalid() throws TaskAccessException {
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
   void testModifyUpdateDuration(Task task) throws TaskAccessException {
      String generationField = data.getGenerationField();
      DateTime initialTime = (DateTime) task.getProperty(generationField).get();
      Instant expectedEnd = initialTime.getStart().plusSeconds(data.getDuration());

      task.modify(data.getFullTaskDelta(), false);
      DateTime dateTime = (DateTime) task.getProperty(generationField).get();

      assertThat(dateTime.getStart()).isEqualTo(initialTime.getStart());
      assertThat(dateTime.getEnd()).isEqualTo(expectedEnd);
   }

   @ParameterizedTest
   @MethodSource("provideGeneratorTasks")
   void testModifySeverGenerator(Task task) throws TaskAccessException {
      Instant beforeModify = Instant.now();
      task.modify(data.getTaskDelta());

      assertThat(task.getGeneratorId()).isNull();

      assertThat(task.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(task.getName()).isEqualTo(data.getTemplateName());
      assertThat(task.getMarkup()).isEqualTo(data.getMarkup());
      for (String s : data.getProperties().keySet()) {
         assertThat(task.getProperty(s)).isEqualTo(data.getProperties().get(s));
      }
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifySeries(Generator generator) throws TaskAccessException {
      List<String> tasks = generator.generateTasks(Instant.now().plusSeconds(600));
      int index = tasks.size() / 2;
      Task targetTask = data.getTaskStore().getTasks().getById(tasks.get(index));
      targetTask.modifySeries(data.getFullGeneratorDelta());

      Set<String> expectedProperties = new HashSet<>();
      expectedProperties.add(data.getGenerationField());
      expectedProperties.addAll(data.getProperties().keySet());

      for (int i = 0; i < tasks.size(); i++) {
         Task task = data.getTaskStore().getTasks().getById(tasks.get(i));
         if (i < index) {
            assertThat(task.getGeneratorId()).isNull();
         }
         else {
            assertThat(task.getName()).isEqualTo(data.getTemplateName());
            assertThat(task.getMarkup()).isEqualTo(data.getMarkup());
            assertThat(task.getPropertyNames()).isEqualTo(expectedProperties);
            for (String s : data.getProperties().keySet()) {
               assertThat(task.getProperty(s)).isEqualTo(data.getProperties().get(s));
            }
         }
      }
   }

   @Test
   void testModifySeriesInvalid() throws TaskAccessException {
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

      assertThat(newTask.getPropertyNames()).isEqualTo(task.getPropertyNames());
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

      Set<String> expectedPropertyNames = new HashSet<>();
      expectedPropertyNames.add(generationField);
      expectedPropertyNames.addAll(generator.getTemplatePropertyNames());
      assertThat(task.getPropertyNames()).isEqualTo(expectedPropertyNames);

      for (String s : generator.getTemplatePropertyNames()) {
         assertThat(task.getProperty(s)).isEqualTo(generator.getTemplateProperty(s));
      }

      DateTime dateTime = (DateTime) task.getProperty(generationField).get();
      assertThat(dateTime.getStart())
         .isEqualTo(instant);
      assertThat(dateTime.getEnd())
         .isEqualTo(instant.plusSeconds(generator.getTemplateDuration()));
   }
}
