package io.github.theimbichner.taskmanager.task;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;

import static org.assertj.core.api.Assertions.*;

public class GeneratorDeltaTests {
   static PropertyMap properties;
   static PropertyMap empty;
   static String name;
   static String templateName;
   static String templateMarkup;
   static Long templateDuration;

   @BeforeAll
   static void beforeAll() {
      properties = PropertyMap.fromJava(Map.of(
         "alpha", Property.of(1L),
         "beta", Property.of(2L),
         "gamma", Property.of(3L),
         "delta", Property.of(4L)));
      empty = PropertyMap.empty();
      name = "epsilon";
      templateName = "zeta";
      templateMarkup = "eta";
      templateDuration = 123456L;
   }

   @Test
   void testEmpty() {
      GeneratorDelta delta = new GeneratorDelta(empty, null, null, null, null);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isTrue();
   }

   @Test
   void testProperties() {
      GeneratorDelta delta = new GeneratorDelta(properties, null, null, null, null);
      assertThat(delta.getProperties().asMap()).isEqualTo(properties.asMap());
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testName() {
      GeneratorDelta delta = new GeneratorDelta(empty, name, null, null, null);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).hasValue(name);
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testTemplateName() {
      GeneratorDelta delta = new GeneratorDelta(empty, null, templateName, null, null);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).hasValue(templateName);
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testTemplateMarkup() {
      GeneratorDelta delta = new GeneratorDelta(empty, null, null, templateMarkup, null);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).hasValue(templateMarkup);
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testTemplateDuration() {
      GeneratorDelta delta = new GeneratorDelta(empty, null, null, null, templateDuration);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).hasValue(templateDuration);

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testAsTaskDelta() {
      GeneratorDelta delta = new GeneratorDelta(
         properties,
         name,
         templateName,
         templateMarkup,
         templateDuration);
      TaskDelta taskDelta = delta.asTaskDelta();
      assertThat(taskDelta.getProperties().asMap()).isEqualTo(properties.asMap());
      assertThat(taskDelta.getName()).hasValue(templateName);
      assertThat(taskDelta.getMarkup()).hasValue(templateMarkup);
      assertThat(taskDelta.getDuration()).hasValue(templateDuration);
   }
}
