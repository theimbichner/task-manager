package io.github.theimbichner.taskmanager.task;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.task.property.Schema;
import io.github.theimbichner.taskmanager.task.property.TypeDescriptor;
import io.github.theimbichner.taskmanager.time.DatePattern;
import io.github.theimbichner.taskmanager.time.DateTime;
import io.github.theimbichner.taskmanager.time.UniformDatePattern;

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

   private static DatePattern getDatePattern(int step) {
      return new UniformDatePattern(Instant.now().plusSeconds(1), Duration.ofSeconds(step));
   }

   @Test
   void testGetTasksFromTable() {
      TaskStore taskStore = data.getTaskStore();

      Table table = Table.createTable();
      table.setTaskStore(taskStore);

      Instant start = Instant.now();
      Instant end = start.plusSeconds(600);

      int numTasks = 0;
      List<DatePattern> datePatterns = List.of(getDatePattern(7), getDatePattern(13));
      for (DatePattern pattern : datePatterns) {
         Generator generator = Generator.createGenerator(table, "", pattern);
         taskStore.getGenerators().save(generator).get();
         table = table.withGenerator(generator.getId());

         numTasks += pattern.getDates(start, end).size();
      }

      assertThat(Orchestration.getTasksFromTable(table, start).get().asList()).isEmpty();
      table = taskStore.getTables().getById(table.getId()).get();
      assertThat(Orchestration.getTasksFromTable(table, end).get().asList()).hasSize(numTasks);
      table = taskStore.getTables().getById(table.getId()).get();
      assertThat(Orchestration.getTasksFromTable(table, start).get().asList()).hasSize(numTasks);
   }

   @Test
   void testModifyTable() {
      DateTime dateTime = new DateTime();

      Table table = Table.createTable();
      table.setTaskStore(data.getTaskStore());

      Schema baseSchema = Schema.empty()
         .withColumn("alpha", TypeDescriptor.fromTypeName("String"))
         .withColumn("beta", TypeDescriptor.fromTypeName("DateTime"))
         .withColumn("gamma", TypeDescriptor.fromTypeName("Boolean"));
      table = table.withModification(new TableDelta(baseSchema, null));
      data.getTaskStore().getTables().save(table).get();

      Task task = Task.createTask(table);
      table = data.getTaskStore().getTables().getById(table.getId()).get();

      PropertyMap taskProperties = PropertyMap.fromJava(Map.of(
         "beta", Property.of(dateTime)));
      task = task.withModification(new TaskDelta(taskProperties, null, null, null)).get();
      data.getTaskStore().getTasks().save(task).get();

      Generator generator = Generator.createGenerator(table, "beta", getDatePattern(4));
      data.getTaskStore().getTables().save(table.withGenerator(generator.getId())).get();
      table = data.getTaskStore().getTables().getById(table.getId()).get();

      PropertyMap generatorProperties = PropertyMap.fromJava(Map.of(
         "alpha", Property.of("abcde"),
         "gamma", Property.of(true)));
      generator = generator.withModification(new GeneratorDelta(
         generatorProperties,
         null,
         null,
         null,
         data.getDuration()));
      data.getTaskStore().getGenerators().save(generator).get();

      Orchestration.getTasksFromTable(table, Instant.now().plusSeconds(600)).get();

      Schema deltaSchema = Schema.empty()
         .withColumn("delta", TypeDescriptor.fromTypeName("EnumList"))
         .withoutColumn("alpha")
         .withColumnRenamed("beta", "epsilon");
      Instant before = Instant.now();
      Orchestration.modifyTable(table, new TableDelta(deltaSchema, "new name"));
      table = data.getTaskStore().getTables().getById(table.getId()).get();

      assertThat(table.getDateLastModified().getStart()).isAfterOrEqualTo(before);
      assertThat(table.getName()).isEqualTo("new name");
      assertThat(table.getSchema().asMap().mapValues(x -> x.getTypeName())).isEqualTo(HashMap.of(
         "gamma", "Boolean",
         "delta", "EnumList",
         "epsilon", "DateTime"));

      task = data.getTaskStore().getTasks().getById(task.getId()).get();
      assertThat(task.getProperties().asMap()).isEqualTo(HashMap.of(
         "gamma", Property.of(false),
         "delta", Property.of(SetList.empty()),
         "epsilon", Property.of(dateTime)));

      generator = data.getTaskStore().getGenerators().getById(generator.getId()).get();
      assertThat(generator.getTemplateProperties().asMap()).isEqualTo(HashMap.of(
         "gamma", Property.of(true),
         "delta", Property.of(SetList.empty()),
         "epsilon", Property.empty()));
      assertThat(generator.getGenerationField()).isEqualTo("epsilon");

      for (String s : table.getAllTaskIds().asList()) {
         if (s.equals(task.getId())) {
            continue;
         }

         Task generatorTask = data.getTaskStore().getTasks().getById(s).get();
         Property generationProperty = generatorTask.getProperties().asMap().get("epsilon").get();
         assertThat(generatorTask.getProperties().asMap()).isEqualTo(HashMap.of(
            "gamma", Property.of(true),
            "delta", Property.of(SetList.empty()),
            "epsilon", generationProperty));

         DateTime generationDateTime = (DateTime) generationProperty.get();
         assertThat(generationDateTime.getEnd())
            .isEqualTo(generationDateTime.getStart().plusSeconds(data.getDuration()));
      }
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
      assertThat(task.getProperties().asMap().get("alpha")).contains(Property.empty());
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
      assertThat(generator.getTemplateProperties().asMap())
         .isEqualTo(data.getProperties().merge(generationFieldMap).asMap());
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
      assertThat(generator.getTemplateProperties().asMap())
         .isEqualTo(data.getProperties().merge(generationFieldMap).asMap());
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

      assertThat(generator.getTemplateProperties().asMap()).isEqualTo(HashMap.of(
         "", Property.empty(),
         "alpha", Property.empty(),
         "gamma", Property.of(new DateTime(Instant.ofEpochSecond(12345)))));
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
         assertThat(dates).contains(date);
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
         assertThat(dates).contains(date);
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
