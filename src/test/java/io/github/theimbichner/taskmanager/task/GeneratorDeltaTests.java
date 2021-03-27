package io.github.theimbichner.taskmanager.task;

import java.util.NoSuchElementException;

import io.vavr.collection.HashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.time.DateTime;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.vavr.api.VavrAssertions.*;

public class GeneratorDeltaTests {
   static PropertyMap properties;
   static String name;
   static String templateName;
   static String templateMarkup;
   static Long templateDuration;

   static DateTime dateTime;
   static PropertyMap taskProperties;

   @BeforeAll
   static void beforeAll() {
      properties = PropertyMap.of(HashMap.of(
         "alpha", Property.ofNumber("1"),
         "beta", Property.ofNumber("2"),
         "gamma", Property.ofNumber("3"),
         "delta", Property.ofNumber("4")));
      name = "epsilon";
      templateName = "zeta";
      templateMarkup = "eta";
      templateDuration = 123456L;

      dateTime = new DateTime();
      taskProperties = PropertyMap.of(HashMap.of(
         "epsilon", Property.of(dateTime)));
   }

   @Test
   void testEmpty() {
      GeneratorDelta delta = new GeneratorDelta(
         PropertyMap.empty(),
         null,
         null,
         null,
         null);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isTrue();
   }

   @Test
   void testProperties() {
      GeneratorDelta delta = new GeneratorDelta(
         properties,
         null,
         null,
         null,
         null);
      assertThat(delta.getProperties().asMap()).isEqualTo(properties.asMap());
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testName() {
      GeneratorDelta delta = new GeneratorDelta(
         PropertyMap.empty(),
         name,
         null,
         null,
         null);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).contains(name);
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testTemplateName() {
      GeneratorDelta delta = new GeneratorDelta(
         PropertyMap.empty(),
         null,
         templateName,
         null,
         null);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).contains(templateName);
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testTemplateMarkup() {
      GeneratorDelta delta = new GeneratorDelta(
         PropertyMap.empty(),
         null,
         null,
         templateMarkup,
         null);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).contains(templateMarkup);
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testTemplateDuration() {
      GeneratorDelta delta = new GeneratorDelta(
         PropertyMap.empty(),
         null,
         null,
         null,
         templateDuration);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).contains(templateDuration);

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testAsTaskDelta() {
      PropertyMap expectedDeltaProperties = properties
        .put("epsilon", Property.of(dateTime.withDuration(templateDuration)));

      GeneratorDelta delta = new GeneratorDelta(
         properties,
         name,
         templateName,
         templateMarkup,
         templateDuration);
      TaskDelta taskDelta = delta.asTaskDelta("epsilon", taskProperties);

      assertThat(taskDelta.getProperties().asMap())
         .isEqualTo(expectedDeltaProperties.asMap());
      assertThat(taskDelta.getName()).contains(templateName);
      assertThat(taskDelta.getMarkup()).contains(templateMarkup);
   }

   @Test
   void testAsTaskDeltaNoProperties() {
      PropertyMap expectedDeltaProperties = PropertyMap.empty()
        .put("epsilon", Property.of(dateTime.withDuration(templateDuration)));

      GeneratorDelta delta = new GeneratorDelta(
         null,
         name,
         templateName,
         templateMarkup,
         templateDuration);
      TaskDelta taskDelta = delta.asTaskDelta("epsilon", taskProperties);

      assertThat(taskDelta.getProperties().asMap())
         .isEqualTo(expectedDeltaProperties.asMap());
      assertThat(taskDelta.getName()).contains(templateName);
      assertThat(taskDelta.getMarkup()).contains(templateMarkup);
   }

   @Test
   void testAsTaskDeltaNoDuration() {
      PropertyMap expectedDeltaProperties = properties;

      GeneratorDelta delta = new GeneratorDelta(
         properties,
         name,
         templateName,
         templateMarkup,
         null);
      TaskDelta taskDelta = delta.asTaskDelta("epsilon", taskProperties);

      assertThat(taskDelta.getProperties().asMap())
         .isEqualTo(expectedDeltaProperties.asMap());
      assertThat(taskDelta.getName()).contains(templateName);
      assertThat(taskDelta.getMarkup()).contains(templateMarkup);
   }

   @Test
   void testAsTaskDeltaNoPropertiesOrDuration() {
      GeneratorDelta delta = new GeneratorDelta(
         PropertyMap.empty(),
         null,
         null,
         null,
         null);
      TaskDelta taskDelta = delta.asTaskDelta("epsilon", taskProperties);

      assertThat(taskDelta.getProperties().asMap()).isEmpty();
      assertThat(taskDelta.getName()).isEmpty();
      assertThat(taskDelta.getMarkup()).isEmpty();
      assertThat(taskDelta.isEmpty()).isTrue();
   }

   @Test
   void testAsTaskDeltaInvalidTaskProperties() {
      GeneratorDelta delta = new GeneratorDelta(
         properties,
         name,
         templateName,
         templateMarkup,
         templateDuration);
      assertThatExceptionOfType(NoSuchElementException.class)
         .isThrownBy(() -> delta.asTaskDelta("alpha", taskProperties));
   }
}
