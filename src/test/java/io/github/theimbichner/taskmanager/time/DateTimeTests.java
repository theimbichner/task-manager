package io.github.theimbichner.taskmanager.time;

import java.time.Instant;
import java.util.stream.Stream;

import org.json.JSONObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.*;

public class DateTimeTests {
   static long delta;
   static Instant startInstant;
   static Instant endInstant;

   @BeforeAll
   static void beforeAll() {
      delta = 1000;
      startInstant = Instant.ofEpochSecond(123456, 987654);
      endInstant = Instant.ofEpochSecond(654321, 456789);
   }

   @Test
   void testConstructor() {
      Instant before = Instant.now();
      DateTime time = new DateTime();
      Instant after = Instant.now();

      assertThat(time.getStart()).isEqualTo(time.getEnd());
      assertThat(before).isBeforeOrEqualTo(time.getStart());
      assertThat(after).isAfterOrEqualTo(time.getStart());
   }

   @Test
   void testStartConstructor() {
      DateTime time = new DateTime(startInstant);
      assertThat(time.getStart()).isEqualTo(startInstant);
      assertThat(time.getEnd()).isEqualTo(startInstant);
   }

   @Test
   void testStartEndConstructor() {
      DateTime time = new DateTime(startInstant, endInstant);
      assertThat(time.getStart()).isEqualTo(startInstant);
      assertThat(time.getEnd()).isEqualTo(endInstant);
   }

   @Test
   void testWithDuration() {
      DateTime time = new DateTime(startInstant, endInstant);
      DateTime newTime = time.withDuration(delta);

      long second = startInstant.getEpochSecond() + delta;
      int nano = startInstant.getNano();
      Instant expectedEnd = Instant.ofEpochSecond(second, nano);

      assertThat(newTime.getStart()).isEqualTo(startInstant);
      assertThat(newTime.getEnd()).isEqualTo(expectedEnd);

      assertThat(time.getStart()).isEqualTo(startInstant);
      assertThat(time.getEnd()).isEqualTo(endInstant);
   }

   @ParameterizedTest
   @MethodSource
   void testEquals(Object left, Object right, boolean equals) {
      if (equals) {
         assertThat(left).isEqualTo(right);
         assertThat(left.hashCode()).isEqualTo(right.hashCode());
      }
      else {
         assertThat(left).isNotEqualTo(right);
      }
   }

   private static Stream<Arguments> testEquals() {
      Instant instant = Instant.now();
      DateTime dateTime = new DateTime(instant);
      DateTime dateTimeCopy = new DateTime(instant);

      DateTime extended = new DateTime(instant.minusSeconds(300), instant.plusSeconds(300));
      DateTime extendedCopy = new DateTime(instant.minusSeconds(300), instant.plusSeconds(300));
      DateTime extendedForward = new DateTime(instant, instant.plusSeconds(300));
      DateTime extendedBackward = new DateTime(instant.minusSeconds(300), instant);

      return Stream.of(
         Arguments.of(dateTime, dateTime, true),
         Arguments.of(dateTime, dateTimeCopy, true),
         Arguments.of(dateTime, null, false),
         Arguments.of(dateTime, "string", false),
         Arguments.of(dateTime, extended, false),
         Arguments.of(dateTime, extendedForward, false),
         Arguments.of(dateTime, extendedBackward, false),
         Arguments.of(extended, extended, true),
         Arguments.of(extended, extendedCopy, true),
         Arguments.of(extended, dateTime, false),
         Arguments.of(extended, extendedForward, false),
         Arguments.of(extended, extendedBackward, false));
   }

   @Test
   void testToFromJson() {
      DateTime time = new DateTime(startInstant, endInstant);
      JSONObject json = time.toJson();
      DateTime newTime = DateTime.fromJson(json);

      assertThat(newTime.getStart()).isEqualTo(time.getStart());
      assertThat(newTime.getEnd()).isEqualTo(time.getEnd());
   }
}
