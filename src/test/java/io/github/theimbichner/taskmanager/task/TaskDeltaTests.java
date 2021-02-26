package io.github.theimbichner.taskmanager.task;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;

import static org.assertj.core.api.Assertions.*;

public class TaskDeltaTests {
   static PropertyMap properties;
   static String name;
   static String markup;
   static Long duration;

   @BeforeAll
   static void beforeAll() {
      properties = PropertyMap.fromJava(Map.of(
         "alpha", Property.ofNumber("1"),
         "beta", Property.ofNumber("2"),
         "gamma", Property.ofNumber("3"),
         "delta", Property.ofNumber("4")));
      name = "epsilon";
      markup = "zeta";
      duration = 123456L;
   }

   @Test
   void testEmpty() {
      TaskDelta delta = new TaskDelta(PropertyMap.empty(), null, null);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).isEmpty();

      assertThat(delta.isEmpty()).isTrue();
   }

   @Test
   void testProperties() {
      TaskDelta delta = new TaskDelta(properties, null, null);
      assertThat(delta.getProperties().asMap()).isEqualTo(properties.asMap());
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testName() {
      TaskDelta delta = new TaskDelta(PropertyMap.empty(), name, null);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).hasValue(name);
      assertThat(delta.getMarkup()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testMarkup() {
      TaskDelta delta = new TaskDelta(PropertyMap.empty(), null, markup);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).hasValue(markup);

      assertThat(delta.isEmpty()).isFalse();
   }
}
