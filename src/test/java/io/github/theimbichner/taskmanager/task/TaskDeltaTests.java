package io.github.theimbichner.taskmanager.task;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;

import static org.assertj.core.api.Assertions.*;

public class TaskDeltaTests {
   static PropertyMap properties;
   static PropertyMap empty;
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
      empty = PropertyMap.empty();
      name = "epsilon";
      markup = "zeta";
      duration = 123456L;
   }

   @Test
   void testEmpty() {
      TaskDelta delta = new TaskDelta(empty, null, null, null);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).isEmpty();
      assertThat(delta.getDuration()).isEmpty();

      assertThat(delta.isEmpty()).isTrue();
   }

   @Test
   void testProperties() {
      TaskDelta delta = new TaskDelta(properties, null, null, null);
      assertThat(delta.getProperties().asMap()).isEqualTo(properties.asMap());
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).isEmpty();
      assertThat(delta.getDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testName() {
      TaskDelta delta = new TaskDelta(empty, name, null, null);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).hasValue(name);
      assertThat(delta.getMarkup()).isEmpty();
      assertThat(delta.getDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testMarkup() {
      TaskDelta delta = new TaskDelta(empty, null, markup, null);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).hasValue(markup);
      assertThat(delta.getDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testDuration() {
      TaskDelta delta = new TaskDelta(empty, null, null, duration);
      assertThat(delta.getProperties().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).isEmpty();
      assertThat(delta.getDuration()).hasValue(duration);

      assertThat(delta.isEmpty()).isFalse();
   }
}
