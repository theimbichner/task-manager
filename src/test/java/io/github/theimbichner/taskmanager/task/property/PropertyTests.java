package io.github.theimbichner.taskmanager.task.property;

import java.util.stream.Stream;
import java.time.Instant;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.time.DateTime;

import static org.assertj.core.api.Assertions.*;

public class PropertyTests {
   @ParameterizedTest
   @MethodSource
   void testEquality(Object lhs, Object rhs, boolean result) {
      if (result) {
         assertThat(lhs).isEqualTo(rhs);
         assertThat(lhs.hashCode()).isEqualTo(rhs.hashCode());
      }
      else {
         assertThat(lhs).isNotEqualTo(rhs);
      }
   }

   private static Stream<Arguments> testEquality() {
      Instant instant = Instant.now();
      DateTime dateTime = new DateTime(instant);
      DateTime dateTimeCopy = new DateTime(instant);

      SetList<String> list = SetList.<String>empty().add("alpha").add("beta");

      Property alphaProp = Property.of("alpha");
      Property emptyProp = Property.empty();

      return Stream.of(
         Arguments.of(alphaProp, "alpha", false),
         Arguments.of(alphaProp, null, false),

         Arguments.of(alphaProp, alphaProp, true),
         Arguments.of(alphaProp, Property.of("alpha"), true),
         Arguments.of(alphaProp, Property.of("beta"), false),

         Arguments.of(emptyProp, emptyProp, true),
         Arguments.of(emptyProp, Property.empty(), true),
         Arguments.of(alphaProp, emptyProp, false),
         Arguments.of(emptyProp, alphaProp, false),

         Arguments.of(Property.of(dateTime), Property.of(dateTime), true),
         Arguments.of(Property.of(dateTime), Property.of(dateTimeCopy), true),
         Arguments.of(Property.of(dateTime), Property.of(dateTime.withDuration(10)), false),
         Arguments.of(Property.of(dateTime), alphaProp, false),
         Arguments.of(alphaProp, Property.of(dateTime), false),

         Arguments.of(Property.of(SetList.empty()), Property.of(SetList.empty()), true),
         Arguments.of(Property.of(SetList.empty()), Property.of(list), false),
         Arguments.of(alphaProp, Property.of(list), false),
         Arguments.of(Property.of(list), alphaProp, false),

         Arguments.of(Property.DELETE, Property.DELETE, true),
         Arguments.of(Property.DELETE, alphaProp, false),
         Arguments.of(alphaProp, Property.DELETE, false),

         Arguments.of(Property.of(true), Property.of(true), true),
         Arguments.of(Property.of(true), Property.of(false), false),
         Arguments.of(Property.of(true), alphaProp, false),
         Arguments.of(alphaProp, Property.of(true), false),

         Arguments.of(Property.of(1L), Property.of(1L), true),
         Arguments.of(Property.of(2.25D), Property.of(2.25D), true),
         Arguments.of(Property.of(1.0D), Property.of(1L), false), // TODO fix
         Arguments.of(Property.of(1L), Property.of(1.0D), false), // TODO fix
         Arguments.of(Property.of(1L), Property.of(2L), false),
         Arguments.of(Property.of(1.5D), Property.of(2.25D), false),
         Arguments.of(Property.of(1L), Property.of(2.25D), false),
         Arguments.of(Property.of(2.25D), Property.of(1L), false));
   }
}
