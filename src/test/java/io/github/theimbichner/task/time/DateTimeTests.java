package io.github.theimbichner.task.time;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class DateTimeTests {
   static long startTime;
   static long endTime;
   static long delta;
   static Instant startInstant;
   static Instant endInstant;

   @BeforeAll
   static void beforeAll() {
      startTime = 123456;
      endTime = 654321;
      delta = 1000;
      startInstant = Instant.ofEpochSecond(startTime);
      endInstant = Instant.ofEpochSecond(endTime);
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
      DateTime time = new DateTime(startTime);
      assertThat(time.getStart()).isEqualTo(startInstant);
      assertThat(time.getEnd()).isEqualTo(startInstant);
   }

   @Test
   void testStartEndConstructor() {
      DateTime time = new DateTime(startTime, endTime);
      assertThat(time.getStart()).isEqualTo(startInstant);
      assertThat(time.getEnd()).isEqualTo(endInstant);
   }

   @Test
   void testWithDuration() {
      DateTime time = new DateTime(startTime, endTime);
      DateTime newTime = time.withDuration(delta);

      assertThat(newTime.getStart()).isEqualTo(startInstant);
      assertThat(newTime.getEnd().getEpochSecond()).isEqualTo(startTime + delta);

      assertThat(time.getStart()).isEqualTo(startInstant);
      assertThat(time.getEnd()).isEqualTo(endInstant);
   }

   @Test
   void testToFromData() {
      DateTime time = new DateTime(startTime, endTime);
      Map<String, Object> data = time.toData();
      DateTime newTime = DateTime.fromData(data);

      assertThat(newTime.getStart()).isEqualTo(time.getStart());
      assertThat(newTime.getEnd()).isEqualTo(time.getEnd());
   }
}
