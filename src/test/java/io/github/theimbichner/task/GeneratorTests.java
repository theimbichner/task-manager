package io.github.theimbichner.task;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import io.vavr.collection.HashSet;

import org.json.JSONArray;
import org.json.JSONObject;

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
   void testToFromJsonWithTasks() {
      Generator generator = data.createModifiedGenerator();

      Instant timestamp = Instant.now().plusSeconds(600);
      List<String> tasks = generator.generateTasks(timestamp).get();

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
         .contains(Property.of(null));
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
      Instant start = generator.getDateCreated().getStart();
      Instant firstInstant = Instant.now().plusSeconds(100);
      Instant secondInstant = Instant.now().plusSeconds(350);

      List<Instant> dates = datePattern.getDates(start, firstInstant);
      List<String> firstResult = generator.generateTasks(firstInstant).get();
      assertThat(firstResult).hasSameSizeAs(dates);
      for (String s : firstResult) {
         Task task = data.getTaskStore().getTasks().getById(s).get();
         assertThat(task.getGeneratorId()).isEqualTo(generator.getId());
      }
      assertThat(generator.getTaskIds().asList()).containsAll(firstResult);

      dates = datePattern.getDates(firstInstant, secondInstant);
      List<String> secondResult = generator.generateTasks(secondInstant).get();
      assertThat(secondResult).hasSameSizeAs(dates);
      for (String s : secondResult) {
         Task task = data.getTaskStore().getTasks().getById(s).get();
         assertThat(task.getGeneratorId()).isEqualTo(generator.getId());
      }
      assertThat(generator.getTaskIds().asList()).containsAll(firstResult);
      assertThat(generator.getTaskIds().asList()).containsAll(secondResult);

      List<String> tasks = generator.getTaskIds().asList();
      List<String> thirdResult = generator.generateTasks(firstInstant).get();
      assertThat(thirdResult).isEmpty();
      assertThat(generator.getTaskIds().asList()).isEqualTo(tasks);
   }

   @Test
   void testUnlinkTasksBefore() {
      Generator generator = data.createDefaultGenerator();
      Instant firstInstant = Instant.now().plusSeconds(100);
      Instant secondInstant = Instant.now().plusSeconds(350);

      List<String> firstResult = generator.generateTasks(firstInstant).get();
      List<String> secondResult = generator.generateTasks(secondInstant).get();

      generator.unlinkTasksBefore(secondResult.get(0)).get();
      for (String s : firstResult) {
         Task task = data.getTaskStore().getTasks().getById(s).get();
         assertThat(task.getGeneratorId()).isNull();
      }
      assertThat(generator.getTaskIds().asList()).isEqualTo(secondResult);
   }

   @Test
   void testUnlinkTasksBeforeInvalid() {
      Generator generator = data.createDefaultGenerator();
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> generator.unlinkTasksBefore("alpha"));
   }
}
