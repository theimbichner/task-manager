package io.github.theimbichner.task;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class TaskDeltaTests {
   static Map<String, Object> properties;
   static String name;
   static String markup;
   static Integer duration;

   @BeforeAll
   static void beforeAll() {
      properties = Map.of("alpha", 1, "beta", 2, "gamma", 3, "delta", 4);
      name = "epsilon";
      markup = "zeta";
      duration = 123456;
   }

   @Test
   void testName() {
      TaskDelta delta = new TaskDelta(properties, name, null, null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).hasValue(name);
      assertThat(delta.getMarkup()).isEmpty();
      assertThat(delta.getDuration()).isEmpty();
   }

   @Test
   void testMarkup() {
      TaskDelta delta = new TaskDelta(properties, null, Optional.of(markup), null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).hasValue(Optional.of(markup));
      assertThat(delta.getDuration()).isEmpty();
   }

   @Test
   void testMarkupEmpty() {
      TaskDelta delta = new TaskDelta(properties, null, Optional.empty(), null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).hasValue(Optional.empty());
      assertThat(delta.getDuration()).isEmpty();
   }

   @Test
   void testDuration() {
      TaskDelta delta = new TaskDelta(properties, null, null, duration);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.getMarkup()).isEmpty();
      assertThat(delta.getDuration()).hasValue(duration);
   }
}
