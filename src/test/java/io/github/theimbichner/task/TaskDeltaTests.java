package io.github.theimbichner.task;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.task.schema.Property;

import static org.assertj.core.api.Assertions.*;

public class TaskDeltaTests {
   static Map<String, Property> properties;
   static String name;
   static String markup;
   static Long duration;

   @BeforeAll
   static void beforeAll() {
      properties = Map.of(
         "alpha", Property.of(1),
         "beta", Property.of(2),
         "gamma", Property.of(3),
         "delta", Property.of(4));
      name = "epsilon";
      markup = "zeta";
      duration = 123456L;
   }

   @Test
   void testEmpty() {
      TaskDelta delta = new TaskDelta(Map.of(), null, null, null);
      assertThat(delta.getProperties()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).isEmpty();
      assertThat(delta.getDuration()).isEmpty();

      assertThat(delta.isEmpty()).isTrue();
   }

   @Test
   void testProperties() {
      TaskDelta delta = new TaskDelta(properties, null, null, null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).isEmpty();
      assertThat(delta.getDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testName() {
      TaskDelta delta = new TaskDelta(Map.of(), name, null, null);
      assertThat(delta.getProperties()).isEmpty();
      assertThat(delta.getName()).hasValue(name);
      assertThat(delta.getMarkup()).isEmpty();
      assertThat(delta.getDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testMarkup() {
      TaskDelta delta = new TaskDelta(Map.of(), null, Optional.of(markup), null);
      assertThat(delta.getProperties()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).hasValue(Optional.of(markup));
      assertThat(delta.getDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testMarkupEmpty() {
      TaskDelta delta = new TaskDelta(Map.of(), null, Optional.empty(), null);
      assertThat(delta.getProperties()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).hasValue(Optional.empty());
      assertThat(delta.getDuration()).isEmpty();

      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testDuration() {
      TaskDelta delta = new TaskDelta(Map.of(), null, null, duration);
      assertThat(delta.getProperties()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).isEmpty();
      assertThat(delta.getDuration()).hasValue(duration);

      assertThat(delta.isEmpty()).isFalse();
   }
}
