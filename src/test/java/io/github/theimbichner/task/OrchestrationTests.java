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
import io.github.theimbichner.task.time.DateTime;

import static org.assertj.core.api.Assertions.*;

public class OrchestrationTests {
   static DataProvider data;

   @BeforeAll
   static void beforeAll() {
      data = new DataProvider();
   }

   private static Stream<Generator> provideGenerators() {
      return Stream.of(
         data.createDefaultGenerator(),
         data.createModifiedGenerator());
   }

   @ParameterizedTest
   @MethodSource("provideGenerators")
   void testModifyFull(Generator generator) {
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
   void testModifyEmpty(Generator generator) {
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
   void testModifyPartial(Generator generator) {
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
   void testModifyUpdateProperties() {
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
   void testModifyWithTasks() {
      Generator generator = data.createDefaultGenerator();
      Instant timestamp = Instant.now().plusSeconds(600);
      List<String> tasks = generator.generateTasks(timestamp).get();
      data.getTaskStore().getGenerators().save(generator).get();

      Orchestration.modifyGenerator(generator, data.getFullGeneratorDelta()).get();
      generator = data.getTaskStore().getGenerators().getById(generator.getId()).get();

      String generationField = data.getGenerationField();

      for (String s : tasks) {
         Task task = data.getTaskStore().getTasks().getById(s).get();
         assertThat(task.getName()).isEqualTo(data.getTemplateName());
         assertThat(task.getMarkup()).isEqualTo(data.getMarkup());
         assertThat(task.getProperties().asMap()).containsAll(data.getProperties().asMap());
         DateTime date = (DateTime) task.getProperties().asMap().get(generationField).get().get();
         assertThat(date.getEnd())
            .isEqualTo(date.getStart().plusSeconds(data.getDuration()));
      }
   }

   @Test
   void testRemoveTasksFromGeneratorBefore() {
      Generator generator = data.createDefaultGenerator();
      Instant firstInstant = Instant.now().plusSeconds(100);
      Instant secondInstant = Instant.now().plusSeconds(350);

      List<String> firstResult = generator.generateTasks(firstInstant).get();
      List<String> secondResult = generator.generateTasks(secondInstant).get();

      Orchestration.removeTasksFromGeneratorBefore(generator, secondResult.get(0)).get();
      generator = generator.getTaskStore().getGenerators().getById(generator.getId()).get();
      for (String s : firstResult) {
         Task task = data.getTaskStore().getTasks().getById(s).get();
         assertThat(task.getGeneratorId()).isNull();
      }
      assertThat(generator.getTaskIds().asList()).isEqualTo(secondResult);
   }
}
