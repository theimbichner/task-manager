package io.github.theimbichner.task;

import java.util.Map;
import java.util.Optional;

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
   void testName() {
      GeneratorDelta delta = new GeneratorDelta(properties, name, null, null, null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).hasValue(name);
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();
   }

   @Test
   void testTemplateName() {
      GeneratorDelta delta = new GeneratorDelta(properties, null, templateName, null, null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).hasValue(templateName);
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEmpty();
   }

   @Test
   void testTemplateMarkup() {
      GeneratorDelta delta = new GeneratorDelta(properties, null, null, Optional.of(templateMarkup), null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).hasValue(Optional.of(templateMarkup));
      assertThat(delta.getTemplateDuration()).isEmpty();
   }

   @Test
   void testTemplateMarkupEmpty() {
      GeneratorDelta delta = new GeneratorDelta(properties, null, null, Optional.empty(), null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).hasValue(Optional.empty());
      assertThat(delta.getTemplateDuration()).isEmpty();
   }

   @Test
   void testTemplateDuration() {
      GeneratorDelta delta = new GeneratorDelta(properties, null, null, null, templateDuration);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getTemplateName()).isEmpty();
      assertThat(delta.getTemplateMarkup()).isEmpty();
      assertThat(delta.getTemplateDuration()).isEqualTo(Optional.of(templateDuration));
   }

   @Test
   void testAsTaskDelta() {
      GeneratorDelta delta = new GeneratorDelta(
         properties,
         name,
         templateName,
         Optional.of(templateMarkup),
         templateDuration);
      TaskDelta taskDelta = delta.asTaskDelta();
      assertThat(taskDelta.getProperties()).isEqualTo(properties);
      assertThat(taskDelta.getName()).hasValue(templateName);
      assertThat(taskDelta.getMarkup()).hasValue(Optional.of(templateMarkup));
      assertThat(taskDelta.getDuration()).hasValue(templateDuration);
   }
}
