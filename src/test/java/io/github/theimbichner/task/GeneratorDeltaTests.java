package io.github.theimbichner.task;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class GeneratorDeltaTests {
   static Map<String, Object> properties;
   static String name;
   static String templateName;
   static String templateMarkup;
   static Integer templateDuration;

   @BeforeAll
   static void beforeAll() {
      properties = Map.of("alpha", 1, "beta", 2, "gamma", 3, "delta", 4);
      name = "epsilon";
      templateName = "zeta";
      templateMarkup = "eta";
      templateDuration = 123456;
   }

   @Test
   void testName() {
      GeneratorDelta delta = new GeneratorDelta(properties, name, null, null, null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEqualTo(Optional.of(name));
      assertThat(delta.getTemplateName()).isEqualTo(Optional.empty());
      assertThat(delta.getTemplateMarkup()).isEqualTo(Optional.empty());
      assertThat(delta.getTemplateDuration()).isEqualTo(Optional.empty());
   }

   @Test
   void testTemplateName() {
      GeneratorDelta delta = new GeneratorDelta(properties, null, templateName, null, null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEqualTo(Optional.empty());
      assertThat(delta.getTemplateName()).isEqualTo(Optional.of(templateName));
      assertThat(delta.getTemplateMarkup()).isEqualTo(Optional.empty());
      assertThat(delta.getTemplateDuration()).isEqualTo(Optional.empty());
   }

   @Test
   void testTemplateMarkup() {
      GeneratorDelta delta = new GeneratorDelta(properties, null, null, Optional.of(templateMarkup), null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEqualTo(Optional.empty());
      assertThat(delta.getTemplateName()).isEqualTo(Optional.empty());
      assertThat(delta.getTemplateMarkup()).isEqualTo(Optional.of(Optional.of(templateMarkup)));
      assertThat(delta.getTemplateDuration()).isEqualTo(Optional.empty());
   }

   @Test
   void testTemplateMarkupEmpty() {
      GeneratorDelta delta = new GeneratorDelta(properties, null, null, Optional.empty(), null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEqualTo(Optional.empty());
      assertThat(delta.getTemplateName()).isEqualTo(Optional.empty());
      assertThat(delta.getTemplateMarkup()).isEqualTo(Optional.of(Optional.empty()));
      assertThat(delta.getTemplateDuration()).isEqualTo(Optional.empty());
   }

   @Test
   void testTemplateDuration() {
      GeneratorDelta delta = new GeneratorDelta(properties, null, null, null, templateDuration);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEqualTo(Optional.empty());
      assertThat(delta.getTemplateName()).isEqualTo(Optional.empty());
      assertThat(delta.getTemplateMarkup()).isEqualTo(Optional.empty());
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
      assertThat(taskDelta.getName()).isEqualTo(Optional.of(templateName));
      assertThat(taskDelta.getMarkup()).isEqualTo(Optional.of(Optional.of(templateMarkup)));
      assertThat(taskDelta.getDuration()).isEqualTo(Optional.of(templateDuration));
   }
}
