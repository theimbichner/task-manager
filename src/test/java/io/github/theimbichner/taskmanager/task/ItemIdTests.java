package io.github.theimbichner.taskmanager.task;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.*;

public class ItemIdTests {
   @Test
   void testToString() {
      assertThat(ItemId.of("alpha").toString()).isEqualTo("alpha");
   }

   @ParameterizedTest
   @MethodSource
   void testEquals(Object left, Object right, boolean equal) {
      if (equal) {
         assertThat(left).isEqualTo(right);
         assertThat(left.hashCode()).isEqualTo(right.hashCode());
      }
      else {
         assertThat(left).isNotEqualTo(right);
      }
   }

   private static Stream<Arguments> testEquals() {
      return Stream.of(
         Arguments.of(ItemId.of("alpha"), ItemId.of("alpha"), true),
         Arguments.of(ItemId.of("alpha"), ItemId.of("beta"), false),
         Arguments.of(ItemId.of("alpha"), null, false),
         Arguments.of(ItemId.of("alpha"), "alpha", false),
         Arguments.of("alpha", ItemId.of("alpha"), false),
         Arguments.of(ItemId.of("alpha"), ItemId.randomId(), false),
         Arguments.of(ItemId.randomId(), ItemId.randomId(), false),
         Arguments.of(ItemId.<Task>of("alpha"), ItemId.<Table>of("alpha"), true));
   }

   @Test
   void testOfStringInvalid() {
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> ItemId.of(null));
   }
}
