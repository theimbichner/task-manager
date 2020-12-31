package io.github.theimbichner.taskmanager.task.property;

import java.util.stream.Stream;
import java.time.Instant;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.taskmanager.time.DateTime;

import static org.assertj.core.api.Assertions.*;

public class PropertyTests {
   @ParameterizedTest
   @MethodSource
   void testEquality(Object lhs, Object rhs, boolean result) {
      if (result) {
         assertThat(lhs).isEqualTo(rhs);
      }
      else {
         assertThat(lhs).isNotEqualTo(rhs);
      }
   }

   private static Stream<Arguments> testEquality() {
      Instant instant = Instant.now();
      DateTime dateTime = new DateTime(instant);
      DateTime dateTimeCopy = new DateTime(instant);
      DateTime extended = new DateTime(instant.minusSeconds(300), instant.plusSeconds(300));
      DateTime extendedForward = new DateTime(instant, instant.plusSeconds(600));
      DateTime extendedBackward = new DateTime(instant.minusSeconds(600), instant);

      return Stream.of(
         Arguments.of(Property.of("alpha"), Property.of("alpha"), true),
         Arguments.of(Property.of("alpha"), Property.of("beta"), false),

         Arguments.of(Property.of(null), Property.of(null), true),
         Arguments.of(Property.of("alpha"), Property.of(null), false),
         Arguments.of(Property.of(null), Property.of("alpha"), false),

         Arguments.of(Property.of("alpha"), "alpha", false),
         Arguments.of(Property.of("alpha"), null, false),

         Arguments.of(Property.of(dateTime), Property.of(dateTime), true),
         Arguments.of(Property.of(dateTime), Property.of("alpha"), false),
         Arguments.of(Property.of("alpha"), Property.of(dateTime), false),

         Arguments.of(Property.of(dateTime), Property.of(extended), false),
         Arguments.of(Property.of(dateTime), Property.of(extendedForward), false),
         Arguments.of(Property.of(dateTime), Property.of(extendedBackward), false),
         Arguments.of(Property.of(dateTime), Property.of(dateTimeCopy), true));
   }
}
