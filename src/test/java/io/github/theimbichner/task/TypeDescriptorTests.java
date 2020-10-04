package io.github.theimbichner.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.*;

public class TypeDescriptorTests {
   private static Stream<Arguments> provideAllTypeNames() {
      return Stream.of(
         Arguments.of("Number", null),
         Arguments.of("Integer", null),
         Arguments.of("String", ""),
         Arguments.of("Boolean", false),
         Arguments.of("DateTime", null),
         Arguments.of("Enum", null),
         Arguments.of("EnumList", new ArrayList<String>()));
   }

   @ParameterizedTest
   @MethodSource("provideAllTypeNames")
   void testDefaultValue(String typeName, Object expectedDefault) {
      TypeDescriptor type = TypeDescriptor.fromTypeName(typeName);

      assertThat(type.getTypeName()).isEqualTo(typeName);
      assertThat(type.getNewDefaultValueInstance()).isEqualTo(expectedDefault);
   }

   @ParameterizedTest
   @MethodSource("provideAllTypeNames")
   void testToFromData(String typeName, Object expectedDefault) {
      TypeDescriptor type = TypeDescriptor.fromTypeName(typeName);
      Map<String, Object> data = type.toData();
      TypeDescriptor newType = TypeDescriptor.fromData(data);

      assertThat(newType.getTypeName()).isEqualTo(typeName);
      assertThat(newType.getNewDefaultValueInstance())
         .isEqualTo(expectedDefault);
      assertThat(newType.getClass()).isEqualTo(type.getClass());
   }

   private static Stream<String> provideEnumTypeNames() {
      return Stream.of("Enum", "EnumList");
   }

   @ParameterizedTest
   @MethodSource("provideEnumTypeNames")
   void testEnumToFromData(String typeName) {
      TypeDescriptor type = TypeDescriptor.fromTypeName(typeName);
      TypeDescriptor.Enumeration enumeration = (TypeDescriptor.Enumeration) type;

      enumeration.withEnumValues(Arrays.asList("alpha", "beta", "gamma"))
         .withoutEnumValues(Arrays.asList("beta", "delta"));

      List<String> enumValues = enumeration.getEnumValues();
      Map<String, Object> data = enumeration.toData();

      enumeration.withEnumValues(Arrays.asList("epsilon", "zeta"))
         .withoutEnumValues(Arrays.asList("alpha", "gamma"));

      TypeDescriptor newType = TypeDescriptor.fromData(data);
      TypeDescriptor.Enumeration newEnum = (TypeDescriptor.Enumeration) newType;

      assertThat(newEnum.getEnumValues()).isEqualTo(enumValues);
   }

   @ParameterizedTest
   @MethodSource("provideEnumTypeNames")
   void testEnumValues(String typeName) {
      TypeDescriptor type = TypeDescriptor.fromTypeName(typeName);
      TypeDescriptor.Enumeration enumeration = (TypeDescriptor.Enumeration) type;

      enumeration.withEnumValues(Arrays.asList("alpha", "beta", "gamma"));
      assertThat(enumeration.getEnumValues())
         .containsExactly("alpha", "beta", "gamma");

      enumeration.withoutEnumValues(Arrays.asList("beta", "delta"));
      assertThat(enumeration.getEnumValues()).containsExactly("alpha", "gamma");

      enumeration.withEnumValues(Arrays.asList("epsilon", "zeta"));
      assertThat(enumeration.getEnumValues())
         .containsExactly("alpha", "gamma", "epsilon", "zeta");

      enumeration.withoutEnumValues(Arrays.asList("alpha", "gamma"));
      assertThat(enumeration.getEnumValues())
         .containsExactly("epsilon", "zeta");
   }

   @Test
   void testGetByNameInvalid() {
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> TypeDescriptor.fromTypeName("Invalid"));
   }
}
