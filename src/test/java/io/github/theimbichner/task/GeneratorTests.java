package io.github.theimbichner.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.time.Duration;
import java.time.Instant;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.task.io.InMemoryDataStore;
import io.github.theimbichner.task.io.TaskAccessException;
import io.github.theimbichner.task.io.TaskStore;
import io.github.theimbichner.task.schema.Property;
import io.github.theimbichner.task.time.DatePattern;
import io.github.theimbichner.task.time.DateTime;
import io.github.theimbichner.task.time.UniformDatePattern;

import static org.assertj.core.api.Assertions.*;

public class GeneratorTests {
   static TaskStore taskStore;

   static Table table;
   static String generationField;
   static DatePattern datePattern;

   static Map<String, Property> deltaProperties;
   static String deltaName;
   static String deltaTemplateName;
   static Optional<String> deltaTemplateMarkup;
   static long deltaTemplateDuration;

   private static Generator createDefaultGenerator() {
      return Generator.createGenerator(table, generationField, datePattern);
   }

   private static Generator createModifiedGenerator() throws TaskAccessException {
      Generator generator = createDefaultGenerator();
      GeneratorDelta delta = new GeneratorDelta(
         deltaProperties,
         deltaName,
         deltaTemplateName,
         deltaTemplateMarkup,
         deltaTemplateDuration);
      generator.modify(delta);
      return generator;
   }

   @BeforeAll
   static void beforeAll() {
      taskStore = InMemoryDataStore.createTaskStore();
      table = Table.createTable();
      table.registerTaskStore(taskStore);

      generationField = "";
      datePattern = new UniformDatePattern(
         Instant.ofEpochSecond(5),
         Duration.ofSeconds(7));

      deltaProperties = Map.of(
         "alpha", Property.of(1),
         "beta", Property.of(""),
         "gamma", Property.of(new DateTime()));
      deltaName = "delta";
      deltaTemplateName = "epsilon";
      deltaTemplateMarkup = Optional.of("zeta");
      deltaTemplateDuration = 600;
   }

   @Test
   void testNewGenerator() {
      Instant before = Instant.now();
      Generator generator = createDefaultGenerator();
      Instant after = Instant.now();

      assertThat(generator.getName()).isEqualTo("");
      assertThat(generator.getTemplateName()).isEqualTo("");
      assertThat(generator.getTemplateMarkup()).isNull();
      assertThat(generator.getTemplateDuration()).isEqualTo(0);
      assertThat(generator.getTemplateTableId()).isEqualTo(table.getId());
      assertThat(generator.getTemplatePropertyNames()).isEqualTo(Set.of());
      assertThat(generator.getGenerationField()).isEqualTo(generationField);

      assertThat(generator.getDateCreated().getStart())
         .isAfterOrEqualTo(before)
         .isBeforeOrEqualTo(after)
         .isEqualTo(generator.getDateCreated().getEnd());
      assertThat(generator.getDateLastModified().getStart())
         .isEqualTo(generator.getDateCreated().getStart())
         .isEqualTo(generator.getDateLastModified().getStart());

      assertThat(generator.getGenerationDatePattern()).isEqualTo(datePattern);
   }

   private static Stream<Generator> provideGenerators() throws TaskAccessException {
      return Stream.of(
         createDefaultGenerator(),
         createModifiedGenerator());
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testToFromJson(Generator generator) {
      Generator newGenerator = Generator.fromJson(generator.toJson());

      assertThat(newGenerator.getId())
         .isEqualTo(generator.getId());
      assertThat(newGenerator.getName())
         .isEqualTo(generator.getName());
      assertThat(newGenerator.getTemplateName())
         .isEqualTo(generator.getTemplateName());
      assertThat(newGenerator.getTemplateMarkup())
         .isEqualTo(generator.getTemplateMarkup());
      assertThat(newGenerator.getTemplateDuration())
         .isEqualTo(generator.getTemplateDuration());
      assertThat(newGenerator.getTemplateTableId())
         .isEqualTo(generator.getTemplateTableId());
      assertThat(newGenerator.getTemplatePropertyNames())
         .isEqualTo(generator.getTemplatePropertyNames());
      assertThat(newGenerator.getGenerationField())
         .isEqualTo(generator.getGenerationField());

      // TODO reenable tests after implementing properties
      // for (String s : newGenerator.getTemplatePropertyNames()) {
      //    assertThat(newGenerator.getTemplateProperty(s))
      //       .isEqualTo(generator.getTemplateProperty(s));
      // }

      assertThat(newGenerator.getDateCreated().getStart())
         .isEqualTo(generator.getDateCreated().getStart());
      assertThat(newGenerator.getDateCreated().getEnd())
         .isEqualTo(generator.getDateCreated().getEnd());
      assertThat(newGenerator.getDateLastModified().getStart())
         .isEqualTo(generator.getDateLastModified().getStart());
      assertThat(newGenerator.getDateLastModified().getEnd())
         .isEqualTo(generator.getDateLastModified().getEnd());

      Instant start = Instant.ofEpochSecond(0);
      Instant end = Instant.ofEpochSecond(1000);

      assertThat(newGenerator.getGenerationDatePattern().getDates(start, end))
         .isEqualTo(generator.getGenerationDatePattern().getDates(start, end));
   }

   @Test
   void testToFromJsonWithTasks() throws TaskAccessException {
      Generator generator = createModifiedGenerator();
      Instant timestamp = Instant.now().plusSeconds(600);
      List<String> tasks = generator.generateTasks(timestamp);

      JSONObject json = generator.toJson();
      JSONArray jsonTasks = json.getJSONArray("tasks");
      for (int i = 0; i < tasks.size(); i++) {
         assertThat(jsonTasks.getString(i)).isEqualTo(tasks.get(i));
      }

      generator = Generator.fromJson(json);
      json = generator.toJson();
      jsonTasks = json.getJSONArray("tasks");
      for (int i = 0; i < tasks.size(); i++) {
         assertThat(jsonTasks.getString(i)).isEqualTo(tasks.get(i));
      }
   }

   // delete markup

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifyFull() throws TaskAccessException {
      Generator generator = createDefaultGenerator();
      GeneratorDelta delta = new GeneratorDelta(
         deltaProperties,
         deltaName,
         deltaTemplateName,
         deltaTemplateMarkup,
         deltaTemplateDuration);
      Instant beforeModify = Instant.now();
      generator.modify(delta);

      assertThat(generator.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(generator.getDateLastModified().getEnd());

      assertThat(generator.getName()).isEqualTo(deltaName);
      assertThat(generator.getTemplateName()).isEqualTo(deltaTemplateName);
      assertThat(generator.getTemplateMarkup()).isEqualTo(deltaTemplateMarkup.get());
      assertThat(generator.getTemplateDuration()).isEqualTo(deltaTemplateDuration);

      assertThat(generator.getTemplatePropertyNames()).isEqualTo(deltaProperties.keySet());
      for (String s : generator.getTemplatePropertyNames()) {
         assertThat(generator.getTemplateProperty(s)).isEqualTo(deltaProperties.get(s));
      }
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifyEmpty(Generator generator) throws TaskAccessException {
      DateTime oldDateLastModified = generator.getDateLastModified();
      String oldName = generator.getName();
      String oldTemplateName = generator.getTemplateName();
      String oldTemplateMarkup = generator.getTemplateMarkup();
      long oldTemplateDuration = generator.getTemplateDuration();
      Set<String> oldTemplatePropertyNames = generator.getTemplatePropertyNames();

      GeneratorDelta delta = new GeneratorDelta(Map.of(), null, null, null, null);
      generator.modify(delta);

      assertThat(generator.getDateLastModified().getStart())
         .isEqualTo(oldDateLastModified.getStart());
      assertThat(generator.getDateLastModified().getEnd())
         .isEqualTo(oldDateLastModified.getEnd());

      assertThat(generator.getName()).isEqualTo(oldName);
      assertThat(generator.getTemplateName()).isEqualTo(oldTemplateName);
      assertThat(generator.getTemplateMarkup()).isEqualTo(oldTemplateMarkup);
      assertThat(generator.getTemplateDuration()).isEqualTo(oldTemplateDuration);

      assertThat(generator.getTemplatePropertyNames())
         .isEqualTo(oldTemplatePropertyNames);
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifyPartial(Generator generator) throws TaskAccessException {
      String oldName = generator.getName();
      String oldTemplateName = generator.getTemplateName();
      String oldTemplateMarkup = generator.getTemplateMarkup();
      long oldTemplateDuration = generator.getTemplateDuration();

      GeneratorDelta delta = new GeneratorDelta(deltaProperties, null, null, null, null);
      Instant beforeModify = Instant.now();
      generator.modify(delta);

      assertThat(generator.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(generator.getDateLastModified().getEnd());

      assertThat(generator.getName()).isEqualTo(oldName);
      assertThat(generator.getTemplateName()).isEqualTo(oldTemplateName);
      assertThat(generator.getTemplateMarkup()).isEqualTo(oldTemplateMarkup);
      assertThat(generator.getTemplateDuration()).isEqualTo(oldTemplateDuration);
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifyDeleteMarkup(Generator generator) throws TaskAccessException {
      GeneratorDelta delta = new GeneratorDelta(Map.of(), null, null, Optional.empty(), null);
      generator.modify(delta);
      assertThat(generator.getTemplateMarkup()).isNull();
   }

   @Test
   void testModifyUpdateProperties() throws TaskAccessException {
      Generator generator = createModifiedGenerator();

      Map<String, Property> properties = new HashMap<>();
      // modify
      properties.put("alpha", Property.of(null));
      // delete
      properties.put("beta", Property.DELETE);
      // delete nonexistent
      properties.put("delta", Property.DELETE);

      generator.modify(new GeneratorDelta(properties, null, null, null, null));
      assertThat(generator.getTemplatePropertyNames()).isEqualTo(Set.of("alpha", "gamma"));
      assertThat(generator.getTemplateProperty("alpha")).isEqualTo(Property.of(null));
   }

   @Test
   void testModifyWithTasks() throws TaskAccessException {
      Generator generator = createDefaultGenerator();
      Instant timestamp = Instant.now().plusSeconds(600);
      List<String> tasks = generator.generateTasks(timestamp);
      taskStore.getGenerators().save(generator);

      GeneratorDelta delta = new GeneratorDelta(deltaProperties, deltaName, deltaTemplateName, deltaTemplateMarkup, deltaTemplateDuration);
      generator.modify(delta);

      for (String s : tasks) {
         Task task = taskStore.getTasks().getById(s);
         assertThat(task.getName()).isEqualTo(deltaTemplateName);
         assertThat(task.getMarkup()).isEqualTo(deltaTemplateMarkup.get());
         for (String key : deltaProperties.keySet()) {
            assertThat(task.getProperty(key)).isEqualTo(deltaProperties.get(key));
         }
         DateTime date = (DateTime) task.getProperty(generationField).get();
         assertThat(date.getEnd())
            .isEqualTo(date.getStart().plusSeconds(deltaTemplateDuration));
      }
   }

   @Test
   void testModifyInvalid() throws TaskAccessException {
      Generator generator = createModifiedGenerator();
      Map<String, Property> newProperties = Map.of(generationField, Property.of(new DateTime()));
      GeneratorDelta delta = new GeneratorDelta(newProperties, null, null, null, null);
      assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
         generator.modify(delta);
      });
   }

   @Test
   void testGenerateTasks() throws TaskAccessException {
      Generator generator = createDefaultGenerator();
      Instant start = generator.getDateCreated().getStart();
      Instant firstInstant = Instant.now().plusSeconds(100);
      Instant secondInstant = Instant.now().plusSeconds(350);

      List<Instant> dates = datePattern.getDates(start, firstInstant);
      List<String> firstResult = generator.generateTasks(firstInstant);
      assertThat(firstResult).hasSameSizeAs(dates);
      for (String s : firstResult) {
         Task task = taskStore.getTasks().getById(s);
         assertThat(task.getGeneratorId()).isEqualTo(generator.getId());
      }

      dates = datePattern.getDates(firstInstant, secondInstant);
      List<String> secondResult = generator.generateTasks(secondInstant);
      assertThat(secondResult).hasSameSizeAs(dates);
      for (String s : secondResult) {
         Task task = taskStore.getTasks().getById(s);
         assertThat(task.getGeneratorId()).isEqualTo(generator.getId());
      }

      List<String> thirdResult = generator.generateTasks(firstInstant);
      assertThat(thirdResult).isEmpty();
   }

   @Test
   void testUnlinkTasksBefore() throws TaskAccessException {
      Generator generator = createDefaultGenerator();
      Instant firstInstant = Instant.now().plusSeconds(100);
      Instant secondInstant = Instant.now().plusSeconds(350);

      List<String> firstResult = generator.generateTasks(firstInstant);
      List<String> secondResult = generator.generateTasks(secondInstant);

      generator.unlinkTasksBefore(secondResult.get(0));
      for (String s : firstResult) {
         Task task = taskStore.getTasks().getById(s);
         assertThat(task.getGeneratorId()).isNull();
      }
   }

   @Test
   void testUnlinkTasksBeforeInvalid() {
      Generator generator = createDefaultGenerator();
      assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
         generator.unlinkTasksBefore("alpha");
      });
   }
}
