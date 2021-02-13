package io.github.theimbichner.taskmanager.task;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.vavr.api.VavrAssertions.*;

public class OrchestrationTests {
   static DataProvider data;
   static PropertyMap generationFieldMap;

   @BeforeAll
   static void beforeAll() {
      data = new DataProvider();
      generationFieldMap = PropertyMap.empty()
         .put(data.getGenerationField(), Property.empty());
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

   /*
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
   */

   /*
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
   */

   /*
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
   */

   // TODO test that modifySeries returned value is the same as saved value
   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifySeries(Generator generator) {
      String generationField = data.getGenerationField();

      Instant instant = Instant.now().plusSeconds(600);
      Orchestration.getTasksFromTable(data.getTable(), instant).get();

      generator = data.getTaskStore().getGenerators().getById(generator.getId()).get();
      List<String> tasks = generator.getTaskIds().asList();
      int index = tasks.size() / 2;
      Task targetTask = data.getTaskStore().getTasks().getById(tasks.get(index)).get();

      Orchestration.modifySeries(targetTask, data.getFullGeneratorDelta()).get();

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
