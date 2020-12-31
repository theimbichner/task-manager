package io.github.theimbichner.taskmanager.task.property;

import java.util.LinkedHashSet;
import java.util.stream.Stream;

import org.json.JSONObject;

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
         Arguments.of("EnumList", new LinkedHashSet<String>()));
   }

   @ParameterizedTest
   @MethodSource("provideAllTypeNames")
   void testDefaultValue(String typeName, Object expectedDefault) {
      TypeDescriptor type = TypeDescriptor.fromTypeName(typeName);

      assertThat(type.getTypeName()).isEqualTo(typeName);
      assertThat(type.getDefaultValue().get()).isEqualTo(expectedDefault);
   }

   @ParameterizedTest
   @MethodSource("provideAllTypeNames")
   void testToFromJson(String typeName, Object expectedDefault) {
      TypeDescriptor type = TypeDescriptor.fromTypeName(typeName);
      JSONObject json = type.toJson();
      TypeDescriptor newType = TypeDescriptor.fromJson(json);

      assertThat(newType.getTypeName()).isEqualTo(typeName);
      assertThat(newType.getDefaultValue().get()).isEqualTo(expectedDefault);
      assertThat(newType.getClass()).isEqualTo(type.getClass());
   }

   private static Stream<String> provideEnumTypeNames() {
      return Stream.of("Enum", "EnumList");
   }

   @ParameterizedTest
   @MethodSource("provideEnumTypeNames")
   void testEnumToFromJson(String typeName) {
      TypeDescriptor type = TypeDescriptor.fromTypeName(typeName);
      EnumerationTypeDescriptor enumType = (EnumerationTypeDescriptor) type;

      enumType = enumType
         .withEnumValues("alpha", "beta", "gamma")
         .withoutEnumValues("beta", "delta")
         .withEnumValues("gamma");

      JSONObject json = enumType.toJson();
      TypeDescriptor newType = TypeDescriptor.fromJson(json);
      EnumerationTypeDescriptor newEnum = (EnumerationTypeDescriptor) newType;

      assertThat(newEnum.getEnumValues()).isEqualTo(enumType.getEnumValues());
   }

   @ParameterizedTest
   @MethodSource("provideEnumTypeNames")
   void testEnumValues(String typeName) {
      TypeDescriptor type = TypeDescriptor.fromTypeName(typeName);
      EnumerationTypeDescriptor enumType = (EnumerationTypeDescriptor) type;
      EnumerationTypeDescriptor newEnum;

      assertThat(enumType.getEnumValues()).isEmpty();

      newEnum = enumType.withEnumValues("alpha", "beta", "gamma");

      assertThat(enumType.getEnumValues()).isEmpty();
      assertThat(newEnum.getEnumValues())
         .containsExactlyInAnyOrder("alpha", "beta", "gamma");

      enumType = newEnum;
      newEnum = enumType.withoutEnumValues("beta", "delta");

      assertThat(enumType.getEnumValues())
         .containsExactlyInAnyOrder("alpha", "beta", "gamma");
      assertThat(newEnum.getEnumValues())
         .containsExactlyInAnyOrder("alpha", "gamma");

      enumType = newEnum;
      newEnum = enumType.withEnumValues("gamma");

      assertThat(enumType.getEnumValues())
         .containsExactlyInAnyOrder("alpha", "gamma");
      assertThat(newEnum.getEnumValues())
         .containsExactlyInAnyOrder("alpha", "gamma");

      enumType = newEnum;
      newEnum = enumType.withEnumValues("epsilon", "zeta");

      assertThat(enumType.getEnumValues())
         .containsExactlyInAnyOrder("alpha", "gamma");
      assertThat(newEnum.getEnumValues())
         .containsExactlyInAnyOrder("alpha", "gamma", "epsilon", "zeta");

      enumType = newEnum;
      newEnum = enumType.withoutEnumValues("alpha", "gamma");

      assertThat(enumType.getEnumValues())
         .containsExactlyInAnyOrder("alpha", "gamma", "epsilon", "zeta");
      assertThat(newEnum.getEnumValues())
         .containsExactlyInAnyOrder("epsilon", "zeta");
   }

   @Test
   void testGetByNameInvalid() {
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> TypeDescriptor.fromTypeName("Invalid"));
   }
}
