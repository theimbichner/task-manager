package io.github.theimbichner.task.time;

import java.time.Instant;

import org.json.JSONObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

   @Test
   void testToFromJson() {
      DateTime time = new DateTime(startInstant, endInstant);
      JSONObject json = time.toJson();
      DateTime newTime = DateTime.fromJson(json);

      assertThat(newTime.getStart()).isEqualTo(time.getStart());
      assertThat(newTime.getEnd()).isEqualTo(time.getEnd());
   }
}
