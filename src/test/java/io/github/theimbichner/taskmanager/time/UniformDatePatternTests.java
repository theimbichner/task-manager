package io.github.theimbichner.taskmanager.time;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import io.vavr.collection.Vector;

import org.json.JSONObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.*;

public class UniformDatePatternTests {
   static Instant init;
   static Duration delta;
   static Instant start;
   static Instant end;
   static UniformDatePattern pattern;

   @BeforeAll
   static void beforeAll() {
      init = Instant.ofEpochSecond(60000);
      delta = Duration.ofSeconds(100);
      start = Instant.ofEpochSecond(70000);
      end = Instant.ofEpochSecond(80000);

      pattern = new UniformDatePattern(init, delta);
   }

   @ParameterizedTest
   @MethodSource
   void testGetDates(int init, int delta, int start, int end, Vector<Integer> expected) {
      Instant initTime = Instant.ofEpochMilli(init);
      Duration deltaTime = Duration.ofMillis(delta);
      UniformDatePattern pattern = new UniformDatePattern(initTime, deltaTime);

      Instant startTime = Instant.ofEpochMilli(start);
      Instant endTime = Instant.ofEpochMilli(end);
      Vector<Instant> dates = pattern.getDates(startTime, endTime);
      Vector<Instant> expectedDates = expected.map(Instant::ofEpochMilli);

      assertThat(dates).isEqualTo(expectedDates);
   }

   private static Stream<Arguments> testGetDates() {
      return Stream.of(
         // Result lies on start
         Arguments.of(1000, 400, 1400, 2800, Vector.of(1800, 2200, 2600)),
         // Result lies on end
         Arguments.of(1000, 300, 1700, 2500, Vector.of(1900, 2200, 2500)),
         // init < start (difference is a multiple of delta)
         Arguments.of(2100, 350, 2800, 4300, Vector.of(3150, 3500, 3850, 4200)),
         // init < start (difference not a multiple of delta)
         Arguments.of(9300, 500, 10400, 11500, Vector.of(10800, 11300)),
         // init == start
         Arguments.of(1024, 256, 1024, 2048, Vector.of(1280, 1536, 1792, 2048)),
         // start < init < end
         Arguments.of(1536, 256, 1024, 2048, Vector.of(1536, 1792, 2048)),
         // init == end
         Arguments.of(1111, 111, 555, 1111, Vector.of(1111)),
         // end < init
         Arguments.of(123456789, 2, 1000, 2000, Vector.empty()),
         // empty result (delta hops over the range)
         Arguments.of(2, 123456789, 1000, 2000, Vector.empty()),
         // one result (init)
         Arguments.of(10, 4, 8, 12, Vector.of(10)),
         // one result (not init)
         Arguments.of(10000, 4000, 13000, 15000, Vector.of(14000)));
   }

   @Test
   void testGetDatesInvalid() {
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> pattern.getDates(end, start));
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> pattern.getDates(start, start));
   }

   @Test
   void testToFromJson() {
      JSONObject json = pattern.toJson();
      DatePattern newPattern = DatePattern.fromJson(json);
      assertThat(newPattern).isInstanceOf(UniformDatePattern.class);
      assertThat(newPattern.getDates(start, end))
         .isEqualTo(pattern.getDates(start, end));
   }
}
