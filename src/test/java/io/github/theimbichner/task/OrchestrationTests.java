package io.github.theimbichner.task;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import io.vavr.collection.HashSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.task.schema.Property;
import io.github.theimbichner.task.schema.PropertyMap;
import io.github.theimbichner.task.time.DatePattern;
import io.github.theimbichner.task.time.DateTime;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.vavr.api.VavrAssertions.*;

public class OrchestrationTests {
   static DataProvider data;

   @BeforeAll
   static void beforeAll() {
      data = new DataProvider();
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
   void testModifyTask(Task task) {
      Instant beforeModify = Instant.now();
      Orchestration.modifyTask(task, data.getTaskDelta()).get();
      task = data.getTaskStore().getTasks().getById(task.getId()).get();

      assertThat(task.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(task.getName()).isEqualTo(data.getTemplateName());
      assertThat(task.getMarkup()).isEqualTo(data.getMarkup());
      assertThat(task.getProperties().asMap())
         .containsAllEntriesOf(data.getProperties().asMap());
   }

   @ParameterizedTest
   @MethodSource("provideGeneratorTasks")
   void testModifyAndSeverTask(Task task) {
      Instant beforeModify = Instant.now();
      Orchestration.modifyAndSeverTask(task, data.getTaskDelta());
      task = data.getTaskStore().getTasks().getById(task.getId()).get();

      assertThat(task.getGeneratorId()).isNull();

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
   void testModifyTaskEmpty(Task task) {
      DateTime oldDateLastModified = task.getDateLastModified();
      String oldName = task.getName();
      String oldMarkup = task.getMarkup();
      PropertyMap oldProperties = task.getProperties();

      TaskDelta delta = new TaskDelta(PropertyMap.empty(), null, null, null);
      Orchestration.modifyTask(task, delta).get();
      task = data.getTaskStore().getTasks().getById(task.getId()).get();

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
   void testModifyAndSeverTaskEmpty(Task task) {
      DateTime oldDateLastModified = task.getDateLastModified();
      String oldName = task.getName();
      String oldMarkup = task.getMarkup();
      PropertyMap oldProperties = task.getProperties();

      TaskDelta delta = new TaskDelta(PropertyMap.empty(), null, null, null);
      Orchestration.modifyAndSeverTask(task, delta).get();
      task = data.getTaskStore().getTasks().getById(task.getId()).get();

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
   void testModifyAndSeverTaskPartial(Task task) {
      String oldName = task.getName();
      String oldMarkup = task.getMarkup();

      Instant beforeModify = Instant.now();
      TaskDelta delta = new TaskDelta(data.getProperties(), null, null, null);
      Orchestration.modifyAndSeverTask(task, delta);
      task = data.getTaskStore().getTasks().getById(task.getId()).get();

      assertThat(task.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(task.getName()).isEqualTo(oldName);
      assertThat(task.getMarkup()).isEqualTo(oldMarkup);
   }

   @Test
   void testModifyAndSeverTaskUpdateProperties() {
      Task task = data.createModifiedTask();
      TaskDelta delta = new TaskDelta(data.getUpdateProperties(), null, null, null);
      Orchestration.modifyAndSeverTask(task, delta).get();
      task = data.getTaskStore().getTasks().getById(task.getId()).get();

      assertThat(task.getProperties().asMap().keySet()).isEqualTo(HashSet.of("alpha", "gamma"));
      assertThat(task.getProperties().asMap().get("alpha")).contains(Property.of(null));
   }

   @ParameterizedTest
   @MethodSource("provideTasks")
   void testModifyAndSeverTaskInvalid(Task task) {
      TaskDelta delta = data.getFullTaskDelta();
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> Orchestration.modifyAndSeverTask(task, delta));
   }

   @ParameterizedTest
   @MethodSource("provideGeneratorTasks")
   void testModifyTaskUpdateDuration(Task task) {
      String generationField = data.getGenerationField();
      DateTime initial = (DateTime) task.getProperties().asMap().get(generationField).get().get();
      Instant expectedEnd = initial.getStart().plusSeconds(data.getDuration());

      Orchestration.modifyTask(task, data.getFullTaskDelta()).get();
      task = data.getTaskStore().getTasks().getById(task.getId()).get();

      DateTime dateTime = (DateTime) task.getProperties().asMap().get(generationField).get().get();
      assertThat(dateTime.getStart()).isEqualTo(initial.getStart());
      assertThat(dateTime.getEnd()).isEqualTo(expectedEnd);
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifyGeneratorFull(Generator generator) {
      Instant beforeModify = Instant.now();
      Orchestration.modifyGenerator(generator, data.getFullGeneratorDelta()).get();
      generator = data.getTaskStore().getGenerators().getById(generator.getId()).get();

      assertThat(generator.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(generator.getDateLastModified().getEnd());

      assertThat(generator.getName()).isEqualTo(data.getName());
      assertThat(generator.getTemplateName()).isEqualTo(data.getTemplateName());
      assertThat(generator.getTemplateMarkup()).isEqualTo(data.getMarkup());
      assertThat(generator.getTemplateDuration()).isEqualTo(data.getDuration());
      assertThat(generator.getTemplateProperties().asMap()).isEqualTo(data.getProperties().asMap());
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifyGeneratorEmpty(Generator generator) {
      DateTime oldDateLastModified = generator.getDateLastModified();
      String oldName = generator.getName();
      String oldTemplateName = generator.getTemplateName();
      String oldTemplateMarkup = generator.getTemplateMarkup();
      long oldTemplateDuration = generator.getTemplateDuration();
      PropertyMap oldTemplateProperties = generator.getTemplateProperties();

      GeneratorDelta delta = new GeneratorDelta(PropertyMap.empty(), null, null, null, null);
      Orchestration.modifyGenerator(generator, delta).get();
      generator = data.getTaskStore().getGenerators().getById(generator.getId()).get();

      assertThat(generator.getDateLastModified().getStart())
         .isEqualTo(oldDateLastModified.getStart());
      assertThat(generator.getDateLastModified().getEnd())
         .isEqualTo(oldDateLastModified.getEnd());

      assertThat(generator.getName()).isEqualTo(oldName);
      assertThat(generator.getTemplateName()).isEqualTo(oldTemplateName);
      assertThat(generator.getTemplateMarkup()).isEqualTo(oldTemplateMarkup);
      assertThat(generator.getTemplateDuration()).isEqualTo(oldTemplateDuration);

      assertThat(generator.getTemplateProperties().asMap())
         .isEqualTo(oldTemplateProperties.asMap());
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifyGeneratorPartial(Generator generator) {
      String oldName = generator.getName();
      String oldTemplateName = generator.getTemplateName();
      String oldTemplateMarkup = generator.getTemplateMarkup();
      long oldTemplateDuration = generator.getTemplateDuration();

      GeneratorDelta delta = new GeneratorDelta(data.getProperties(), null, null, null, null);
      Instant beforeModify = Instant.now();
      Orchestration.modifyGenerator(generator, delta).get();
      generator = data.getTaskStore().getGenerators().getById(generator.getId()).get();

      assertThat(generator.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(generator.getDateLastModified().getEnd());

      assertThat(generator.getName()).isEqualTo(oldName);
      assertThat(generator.getTemplateName()).isEqualTo(oldTemplateName);
      assertThat(generator.getTemplateMarkup()).isEqualTo(oldTemplateMarkup);
      assertThat(generator.getTemplateDuration()).isEqualTo(oldTemplateDuration);
   }

   @Test
   void testModifyGeneratorUpdateProperties() {
      Generator generator = data.createModifiedGenerator();
      GeneratorDelta delta = new GeneratorDelta(
         data.getUpdateProperties(),
         null,
         null,
         null,
         null);
      Orchestration.modifyGenerator(generator, delta).get();
      generator = data.getTaskStore().getGenerators().getById(generator.getId()).get();

      assertThat(generator.getTemplateProperties().asMap().keySet())
         .isEqualTo(HashSet.of("alpha", "gamma"));
      assertThat(generator.getTemplateProperties().asMap().get("alpha"))
         .contains(Property.of(null));
   }

   @Test
   void testModifyGeneratorWithTasks() {
      Generator generator = data.createDefaultGenerator();
      Instant timestamp = Instant.now().plusSeconds(600);
      String generationField = data.getGenerationField();

      List<String> tasks = Orchestration.runGenerator(generator, timestamp).get();
      generator = data.getTaskStore().getGenerators().getById(generator.getId()).get();

      Orchestration.modifyGenerator(generator, data.getFullGeneratorDelta()).get();
      generator = data.getTaskStore().getGenerators().getById(generator.getId()).get();
      assertThat(generator.getTaskIds().asList()).isEqualTo(tasks);

      for (String s : tasks) {
         Task task = data.getTaskStore().getTasks().getById(s).get();
         assertThat(task.getName()).isEqualTo(data.getTemplateName());
         assertThat(task.getMarkup()).isEqualTo(data.getMarkup());
         assertThat(task.getProperties().asMap())
            .containsAllEntriesOf(data.getProperties().asMap());
         DateTime date = (DateTime) task.getProperties().asMap().get(generationField).get().get();
         assertThat(date.getEnd())
            .isEqualTo(date.getStart().plusSeconds(data.getDuration()));
      }
   }

   // TODO add tests where generateTasks timestamp lies exactly on timestamp returned by getDates
   @Test
   void testRunGenerator() {
      Generator generator = data.createDefaultGenerator();
      DatePattern datePattern = data.getGenerationDatePattern();
      String generationField = data.getGenerationField();
      Instant start = generator.getDateCreated().getStart();
      Instant firstInstant = Instant.now().plusSeconds(100);
      Instant secondInstant = Instant.now().plusSeconds(350);

      List<Instant> dates = datePattern.getDates(start, firstInstant);
      List<String> firstResult = Orchestration.runGenerator(generator, firstInstant).get();
      generator = data.getTaskStore().getGenerators().getById(generator.getId()).get();
      assertThat(firstResult).hasSameSizeAs(dates);
      for (String s : firstResult) {
         Task task = data.getTaskStore().getTasks().getById(s).get();
         Property dateProperty = task.getProperties().asMap().get(generationField).get();
         Instant date = ((DateTime) dateProperty.get()).getStart();
         assertThat(task.getGeneratorId()).isEqualTo(generator.getId());
         assertThat(dates.contains(date));
      }
      assertThat(generator.getTaskIds().asList()).containsAll(firstResult);

      dates = datePattern.getDates(firstInstant, secondInstant);
      List<String> secondResult = Orchestration.runGenerator(generator, secondInstant).get();
      generator = data.getTaskStore().getGenerators().getById(generator.getId()).get();
      assertThat(secondResult).hasSameSizeAs(dates);
      for (String s : secondResult) {
         Task task = data.getTaskStore().getTasks().getById(s).get();
         Property dateProperty = task.getProperties().asMap().get(generationField).get();
         Instant date = ((DateTime) dateProperty.get()).getStart();
         assertThat(task.getGeneratorId()).isEqualTo(generator.getId());
         assertThat(dates.contains(date));
      }
      assertThat(generator.getTaskIds().asList()).containsAll(firstResult);
      assertThat(generator.getTaskIds().asList()).containsAll(secondResult);

      List<String> prevTasks = generator.getTaskIds().asList();
      List<String> thirdResult = Orchestration.runGenerator(generator, firstInstant).get();
      generator = data.getTaskStore().getGenerators().getById(generator.getId()).get();
      assertThat(thirdResult).isEmpty();
      assertThat(generator.getTaskIds().asList()).isEqualTo(prevTasks);
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifySeries(Generator generator) {
      Instant instant = Instant.now().plusSeconds(600);
      List<String> tasks = Orchestration.runGenerator(generator, instant).get();
      int index = tasks.size() / 2;
      Task targetTask = data.getTaskStore().getTasks().getById(tasks.get(index)).get();
      Orchestration.modifySeries(targetTask, data.getFullGeneratorDelta()).get();

      String generationField = data.getGenerationField();

      for (int i = 0; i < tasks.size(); i++) {
         Task task = data.getTaskStore().getTasks().getById(tasks.get(i)).get();
         if (i < index) {
            assertThat(task.getGeneratorId()).isNull();
         }
         else {
            assertThat(task.getGeneratorId()).isEqualTo(generator.getId());
            assertThat(task.getName()).isEqualTo(data.getTemplateName());
            assertThat(task.getMarkup()).isEqualTo(data.getMarkup());
            assertThat(task.getProperties().asMap().remove(generationField))
               .isEqualTo(data.getProperties().asMap());
            assertThat(task.getProperties().asMap().containsKey(generationField)).isTrue();
         }
      }

      generator = data.getTaskStore().getGenerators().getById(generator.getId()).get();
      assertThat(generator.getTaskIds().asList()).isEqualTo(tasks.subList(index, tasks.size()));
   }

   @Test
   void testModifySeriesInvalid() {
      Task task = data.createModifiedTask();
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> Orchestration.modifySeries(task, data.getFullGeneratorDelta()));
   }
}
