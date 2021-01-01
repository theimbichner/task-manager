package io.github.theimbichner.taskmanager.task.property;

import java.math.BigDecimal;
import java.util.stream.Stream;
import java.time.Instant;

import org.json.JSONObject;

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
      Property longProp = Property.of(1L);
      Property doubleProp = Property.of(2.25D);

      return Stream.of(
         Arguments.of(alphaProp, "alpha", false),
         Arguments.of(alphaProp, null, false),
         Arguments.of(longProp, "alpha", false),
         Arguments.of(longProp, null, false),

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

         Arguments.of(longProp, longProp, true),
         Arguments.of(longProp, Property.of(1L), true),
         Arguments.of(longProp, Property.of(2L), false),
         Arguments.of(longProp, alphaProp, false),
         Arguments.of(alphaProp, longProp, false),

         Arguments.of(doubleProp, doubleProp, true),
         Arguments.of(doubleProp, Property.of(2.25D), true),
         Arguments.of(doubleProp, Property.of(1.5D), false),
         Arguments.of(doubleProp, alphaProp, false),
         Arguments.of(alphaProp, doubleProp, false),

         Arguments.of(Property.of(1.0D), longProp, true),
         Arguments.of(longProp, Property.of(1.0D), true),
         Arguments.of(longProp, doubleProp, false),
         Arguments.of(doubleProp, longProp, false));
   }

   @ParameterizedTest
   @MethodSource
   void testToFromJson(Property property) {
      JSONObject json = property.toJson();
      assertThat(Property.fromJson(json)).isEqualTo(property);
   }

   private static Stream<Property> testToFromJson() {
      return Stream.of(
         Property.of("alpha"),
         Property.empty(),
         Property.of(new DateTime(Instant.now())),
         Property.of(SetList.<String>empty().add("alpha").add("beta")),
         Property.of(false),
         Property.of(2.25D),
         Property.of(1L),
         Property.of(new BigDecimal("0.123456789012345678901234567890123456789")));
   }

   @ParameterizedTest
   @MethodSource
   void testToFromJsonInvalid(JSONObject json, Class<? extends Exception> cls) {
      assertThatExceptionOfType(cls)
         .isThrownBy(() -> Property.fromJson(json));
   }

   private static Stream<Arguments> testToFromJsonInvalid() {
      return Stream.of(
         Arguments.of(Property.DELETE.toJson(), NullPointerException.class),
         Arguments.of(new JSONObject(), IllegalArgumentException.class));
   }
}
