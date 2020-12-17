package io.github.theimbichner.task;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.task.schema.Property;

import static org.assertj.core.api.Assertions.*;

public class GeneratorDeltaTests {
   static Map<String, Property> properties;
   static String name;
   static String templateName;
   static String templateMarkup;
   static Long templateDuration;

   @BeforeAll
   static void beforeAll() {
      properties = Map.of(
         "alpha", Property.of(1),
         "beta", Property.of(2),
         "gamma", Property.of(3),
         "delta", Property.of(4));
      name = "epsilon";
      templateName = "zeta";
      templateMarkup = "eta";
      templateDuration = 123456L;
   }

   @Test
   void testEmpty() {
      GeneratorDelta delta = new GeneratorDelta(Map.of(), null, null, null, null);
      assertThat(delta.getProperties()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isTrue();
   }

   @Test
   void testProperties() {
      GeneratorDelta delta = new GeneratorDelta(properties, null, null, null, null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testName() {
      GeneratorDelta delta = new GeneratorDelta(Map.of(), name, null, null, null);
      assertThat(delta.getProperties()).isEmpty();
      assertThat(delta.getName()).hasValue(name);
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testTemplateName() {
      GeneratorDelta delta = new GeneratorDelta(Map.of(), null, templateName, null, null);
      assertThat(delta.getProperties()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).hasValue(templateName);
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testTemplateMarkup() {
      GeneratorDelta delta = new GeneratorDelta(Map.of(), null, null, templateMarkup, null);
      assertThat(delta.getProperties()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).hasValue(templateMarkup);
      assertThat(delta.getTemplateDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testTemplateDuration() {
      GeneratorDelta delta = new GeneratorDelta(Map.of(), null, null, null, templateDuration);
      assertThat(delta.getProperties()).isEmpty();
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
      assertThat(taskDelta.getProperties()).isEqualTo(properties);
      assertThat(taskDelta.getName()).hasValue(templateName);
      assertThat(taskDelta.getMarkup()).hasValue(templateMarkup);
      assertThat(taskDelta.getDuration()).hasValue(templateDuration);
   }
}
