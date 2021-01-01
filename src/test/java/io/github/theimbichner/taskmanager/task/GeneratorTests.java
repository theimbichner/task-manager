package io.github.theimbichner.taskmanager.task;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import io.vavr.Tuple2;
import io.vavr.collection.HashSet;

import org.json.JSONArray;
import org.json.JSONObject;

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

public class GeneratorTests {
   static DataProvider data;

   @BeforeAll
   static void beforeAll() {
      data = new DataProvider();
   }

   @Test
   void testNewGenerator() {
      Instant before = Instant.now();
      Generator generator = data.createDefaultGenerator();
      Instant after = Instant.now();

      assertThat(generator.getName()).isEqualTo("");
      assertThat(generator.getTemplateName()).isEqualTo("");
      assertThat(generator.getTemplateMarkup()).isEqualTo("");
      assertThat(generator.getTemplateDuration()).isEqualTo(0);
      assertThat(generator.getTemplateTableId()).isEqualTo(data.getTable().getId());
      assertThat(generator.getTemplateProperties().asMap()).isEmpty();
      assertThat(generator.getGenerationField()).isEqualTo(data.getGenerationField());
      assertThat(generator.getTaskIds().asList()).isEmpty();

      assertThat(generator.getDateCreated().getStart())
         .isAfterOrEqualTo(before)
         .isBeforeOrEqualTo(after)
         .isEqualTo(generator.getDateCreated().getEnd());
      assertThat(generator.getDateLastModified().getStart())
         .isEqualTo(generator.getDateCreated().getStart())
         .isEqualTo(generator.getDateLastModified().getStart());

      assertThat(generator.getGenerationDatePattern())
         .isEqualTo(data.getGenerationDatePattern());
   }

   private static Stream<Generator> provideGenerators() {
      return Stream.of(
         data.createDefaultGenerator(),
         data.createModifiedGenerator());
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
      assertThat(newGenerator.getGenerationField())
         .isEqualTo(generator.getGenerationField());
      assertThat(newGenerator.getTaskIds().asList())
         .isEqualTo(generator.getTaskIds().asList());

      assertThat(newGenerator.getTemplateProperties().asMap())
         .isEqualTo(generator.getTemplateProperties().asMap());

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
   void testToFromJsonWithTasks() {
      Generator generator = data.createModifiedGenerator();

      Instant timestamp = Instant.now().plusSeconds(600);
      Tuple2<Generator, List<Task>> tuple = generator.withTasksUntil(timestamp);
      generator = tuple._1;
      List<Task> tasks = tuple._2;

      JSONObject json = generator.toJson();
      JSONArray jsonTasks = json.getJSONArray("tasks");
      for (int i = 0; i < tasks.size(); i++) {
         assertThat(jsonTasks.getString(i)).isEqualTo(tasks.get(i).getId());
      }

      generator = Generator.fromJson(json);
      json = generator.toJson();
      jsonTasks = json.getJSONArray("tasks");
      for (int i = 0; i < tasks.size(); i++) {
         assertThat(jsonTasks.getString(i)).isEqualTo(tasks.get(i).getId());
      }
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifyFull(Generator generator) {
      Instant beforeModify = Instant.now();
      generator = generator.withModification(data.getFullGeneratorDelta());

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
   void testModifyEmpty(Generator generator) {
      GeneratorDelta delta = new GeneratorDelta(PropertyMap.empty(), null, null, null, null);
      assertThat(generator.withModification(delta)).isSameAs(generator);
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifyPartial(Generator generator) {
      String oldName = generator.getName();
      String oldTemplateName = generator.getTemplateName();
      String oldTemplateMarkup = generator.getTemplateMarkup();
      long oldTemplateDuration = generator.getTemplateDuration();

      GeneratorDelta delta = new GeneratorDelta(data.getProperties(), null, null, null, null);
      Instant beforeModify = Instant.now();
      generator = generator.withModification(delta);

      assertThat(generator.getDateLastModified().getStart())
         .isAfterOrEqualTo(beforeModify)
         .isEqualTo(generator.getDateLastModified().getEnd());

      assertThat(generator.getName()).isEqualTo(oldName);
      assertThat(generator.getTemplateName()).isEqualTo(oldTemplateName);
      assertThat(generator.getTemplateMarkup()).isEqualTo(oldTemplateMarkup);
      assertThat(generator.getTemplateDuration()).isEqualTo(oldTemplateDuration);
   }

   @Test
   void testModifyUpdateProperties() {
      GeneratorDelta delta = new GeneratorDelta(
         data.getUpdateProperties(),
         null,
         null,
         null,
         null);
      Generator generator = data.createModifiedGenerator().withModification(delta);

      assertThat(generator.getTemplateProperties().asMap().keySet())
         .isEqualTo(HashSet.of("alpha", "gamma"));
      assertThat(generator.getTemplateProperties().asMap().get("alpha"))
         .contains(Property.empty());
   }

   @Test
   void testModifyInvalid() {
      Generator generator = data.createModifiedGenerator();
      GeneratorDelta delta = new GeneratorDelta(
         PropertyMap.empty().put(
            data.getGenerationField(), Property.of(new DateTime())),
         null,
         null,
         null,
         null);
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> generator.withModification(delta));
   }

   // TODO add tests where generateTasks timestamp lies exactly on timestamp returned by getDates
   @Test
   void testGenerateTasks() {
      Generator generator = data.createDefaultGenerator();
      DatePattern datePattern = data.getGenerationDatePattern();
      String generationField = data.getGenerationField();
      Instant start = generator.getDateCreated().getStart();
      Instant firstInstant = Instant.now().plusSeconds(100);
      Instant secondInstant = Instant.now().plusSeconds(350);

      List<Instant> dates = datePattern.getDates(start, firstInstant);
      Tuple2<Generator, List<Task>> firstResult = generator.withTasksUntil(firstInstant);
      generator = firstResult._1;
      assertThat(firstResult._2).hasSameSizeAs(dates);
      for (Task task : firstResult._2) {
         Property dateProperty = task.getProperties().asMap().get(generationField).get();
         Instant date = ((DateTime) dateProperty.get()).getStart();
         assertThat(task.getGeneratorId()).isEqualTo(generator.getId());
         assertThat(dates).contains(date);
      }
      List<String> firstIds = firstResult._2.stream().map(Task::getId).collect(Collectors.toList());
      assertThat(generator.getTaskIds().asList()).containsAll(firstIds);

      dates = datePattern.getDates(firstInstant, secondInstant);
      Tuple2<Generator, List<Task>> secondResult = generator.withTasksUntil(secondInstant);
      generator = secondResult._1;
      assertThat(secondResult._2).hasSameSizeAs(dates);
      for (Task task : secondResult._2) {
         Property dateProperty = task.getProperties().asMap().get(generationField).get();
         Instant date = ((DateTime) dateProperty.get()).getStart();
         assertThat(task.getGeneratorId()).isEqualTo(generator.getId());
         assertThat(dates).contains(date);
      }
      List<String> secondIds = secondResult._2.stream().map(Task::getId).collect(Collectors.toList());
      assertThat(generator.getTaskIds().asList()).containsAll(firstIds);
      assertThat(generator.getTaskIds().asList()).containsAll(secondIds);

      Tuple2<Generator, List<Task>> thirdResult = generator.withTasksUntil(firstInstant);
      assertThat(thirdResult._1).isSameAs(generator);
      assertThat(thirdResult._2).isEmpty();
   }

   @Test
   void testUnlinkTasksBefore() {
      Generator generator = data.createDefaultGenerator();
      Instant firstInstant = Instant.now().plusSeconds(100);
      Instant secondInstant = Instant.now().plusSeconds(350);

      Tuple2<Generator, List<Task>> tuple = generator.withTasksUntil(firstInstant);
      generator = tuple._1;
      List<String> firstIds = tuple._2.stream().map(Task::getId).collect(Collectors.toList());
      tuple = generator.withTasksUntil(secondInstant);
      generator = tuple._1;
      List<String> secondIds = tuple._2.stream().map(Task::getId).collect(Collectors.toList());

      String id = secondIds.get(0);
      Tuple2<Generator, List<String>> result = generator.withoutTasksBefore(id);
      assertThat(result._1.getTaskIds().asList()).isEqualTo(secondIds);
      assertThat(result._2).isEqualTo(firstIds);
   }

   @Test
   void testUnlinkTasksBeforeInvalid() {
      Generator generator = data.createDefaultGenerator();
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> generator.withoutTasksBefore("alpha"));
   }
}
