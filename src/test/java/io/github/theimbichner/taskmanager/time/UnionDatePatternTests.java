package io.github.theimbichner.taskmanager.time;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class UnionDatePatternTests {
   static UnionDatePattern pattern;
   static Instant start;
   static Instant end;
   static List<Integer> expected;

   @BeforeAll
   static void beforeAll() {
      pattern = new UnionDatePattern().withPatterns(
         new UniformDatePattern(Instant.ofEpochSecond(75), Duration.ofSeconds(7)),
         new UniformDatePattern(Instant.ofEpochSecond(93), Duration.ofSeconds(1)),
         new UniformDatePattern(Instant.ofEpochSecond(6), Duration.ofSeconds(24)),
         new UniformDatePattern(Instant.ofEpochSecond(89), Duration.ofSeconds(4)));
      start = Instant.ofEpochSecond(0);
      end = Instant.ofEpochSecond(100);

      expected = List.of(6, 30, 54, 75, 78, 82, 89, 93, 94, 95, 96, 97, 98, 99, 100);
   }

   static void testDates(DatePattern pattern, List<Integer> expected) {
      List<Instant> expectedDates = expected.stream()
         .map(Instant::ofEpochSecond)
         .collect(Collectors.toList());

      assertThat(pattern.getDates(start, end)).isEqualTo(expectedDates);
   }

   @Test
   void testGetDates() {
      testDates(pattern, expected);
   }

   @Test
   void testGetDatesNested() {
      testDates(new UnionDatePattern().withPatterns(pattern), expected);
   }

   @Test
   void testGetDatesEmpty() {
      testDates(new UnionDatePattern(), List.of());
   }

   @Test
   void testWithoutPatternAt() {
      List<Integer> expected = List.of(6, 30, 54, 75, 78, 82, 89, 93, 96, 97);
      testDates(pattern.withoutPatternAt(1), expected);
   }

   @Test
   void testGetNumberOfPatterns() {
      UnionDatePattern empty = new UnionDatePattern();
      UnionDatePattern wrapped = empty.withPatterns(pattern);

      assertThat(empty.getNumberOfPatterns()).isEqualTo(0);
      assertThat(wrapped.getNumberOfPatterns()).isEqualTo(1);
      assertThat(pattern.getNumberOfPatterns()).isEqualTo(4);
   }

   @Test
   void testGetPatternAt() {
      testDates(pattern.getPatternAt(0), List.of(75, 82, 89, 96));
      testDates(pattern.getPatternAt(1), List.of(93, 94, 95, 96, 97, 98, 99, 100));
      testDates(pattern.getPatternAt(2), List.of(6, 30, 54, 78));
      testDates(pattern.getPatternAt(3), List.of(89, 93, 97));
   }

   @Test
   void testWithPatternsImmutable() {
      UnionDatePattern newPattern = new UnionDatePattern();
      newPattern.withPatterns(pattern);
      testDates(newPattern, List.of());
   }

   @Test
   void testWithoutPatternAtImmutable() {
      pattern.withoutPatternAt(0);
      testDates(pattern, expected);
   }

   @Test
   void testToFromJson() {
      JSONObject json = pattern.toJson();
      DatePattern newPattern = DatePattern.fromJson(json);

      assertThat(newPattern).isInstanceOf(UnionDatePattern.class);
      assertThat(newPattern.getDates(start, end))
         .isEqualTo(pattern.getDates(start, end));
   }
}
