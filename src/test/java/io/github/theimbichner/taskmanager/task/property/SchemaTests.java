package io.github.theimbichner.taskmanager.task.property;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import io.vavr.collection.HashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.time.DateTime;

import static org.assertj.core.api.Assertions.*;

public class SchemaTests {
   private static DateTime dateTime;
   private static SetList<String> setList;
   private static Schema baseSchema;
   private static PropertyMap baseProperties;

   @BeforeAll
   static void beforeAll() {
      dateTime = new DateTime();
      setList = SetList.<String>empty().add("one").add("two");

      baseSchema = Schema.empty()
         .withColumn("alpha", TypeDescriptor.fromTypeName("String"))
         .withColumn("beta", TypeDescriptor.fromTypeName("DateTime"))
         .withColumn(
            "gamma",
            ((EnumerationTypeDescriptor) TypeDescriptor.fromTypeName("EnumList"))
               .withEnumValues("one", "two", "three"));
      baseProperties = PropertyMap.fromJava(Map.of(
         "alpha", Property.of("abcde"),
         "beta", Property.of(dateTime),
         "gamma", Property.of(setList)));
   }

   @Test
   void testAsMap() {
      assertThat(baseSchema.asMap()).isEqualTo(HashMap.of(
         "alpha", TypeDescriptor.fromTypeName("String"),
         "beta", TypeDescriptor.fromTypeName("DateTime"),
         "gamma", TypeDescriptor.fromTypeName("EnumList")));
   }

   @Test
   void testToFromJson() {
      Schema schema = Schema.fromJson(baseSchema.toJson());
      HashMap<String, String> map = schema.asMap().mapValues(x -> x.getTypeName());
      assertThat(map).isEqualTo(HashMap.of(
         "alpha", "String",
         "beta", "DateTime",
         "gamma", "EnumList"));
      TypeDescriptor type = schema.asMap().get("gamma").get();
      assertThat(((EnumerationTypeDescriptor) type).getEnumValues())
         .containsExactly("one", "two", "three");
   }

   @Test
   void testAsMapEmpty() {
      assertThat(Schema.empty().asMap()).isEmpty();
   }

   @Test
   void testAsPropertiesDelta() {
      assertThat(baseSchema.asPropertiesDelta(baseProperties).asMap()).isEqualTo(HashMap.of(
         "alpha", Property.of(""),
         "beta", Property.empty(),
         "gamma", Property.of(SetList.empty())));
   }

   @ParameterizedTest
   @MethodSource
   void testMerge(
      Schema delta,
      HashMap<String, String> expectedSchema,
      HashMap<String, Property> expectedProperties
   ) {
      Schema merged = baseSchema.merge(delta);
      PropertyMap propertiesDelta = delta.asPropertiesDelta(baseProperties);
      PropertyMap mergedProperties = baseProperties.merge(propertiesDelta);

      assertThat(merged.asMap().mapValues(x -> x.getTypeName())).isEqualTo(expectedSchema);
      assertThat(mergedProperties.asMap()).isEqualTo(expectedProperties);
   }

   private static Stream<Arguments> testMerge() {
      return Stream.of(
         Arguments.of(
            Schema.empty(),
            HashMap.of(
               "alpha", "String",
               "beta", "DateTime",
               "gamma", "EnumList"),
            HashMap.of(
               "alpha", Property.of("abcde"),
               "beta", Property.of(dateTime),
               "gamme", Property.of(setList))),
         Arguments.of(
            Schema.empty().withColumn("alpha", TypeDescriptor.fromTypeName("Integer")),
            HashMap.of(
               "alpha", "Integer",
               "beta", "DateTime",
               "gamma", "EnumList"),
            HashMap.of(
               "alpha", Property.empty(),
               "beta", Property.of(dateTime),
               "gamma", Property.of(setList))),
         Arguments.of(
            Schema.empty().withColumn("delta", TypeDescriptor.fromTypeName("Integer")),
            HashMap.of(
               "alpha", "String",
               "beta", "DateTime",
               "gamma", "EnumList",
               "delta", "Integer"),
            HashMap.of(
               "alpha", Property.of("abcde"),
               "beta", Property.of(dateTime),
               "gamma", Property.of(setList),
               "delta", Property.empty())),
         Arguments.of(
            Schema.empty().withoutColumn("alpha"),
            HashMap.of(
               "beta", "DateTime",
               "gamma", "EnumList"),
            HashMap.of(
               "beta", Property.of(dateTime),
               "gamma", Property.of(setList))),
         Arguments.of(
            Schema.empty().withoutColumn("delta"),
            HashMap.of(
               "alpha", "String",
               "beta", "DateTime",
               "gamma", "EnumList"),
            HashMap.of(
               "alpha", Property.of("abcde"),
               "beta", Property.of(dateTime),
               "gamma", Property.of(setList))),
         Arguments.of(
            Schema.empty().withColumnRenamed("alpha", "delta"),
            HashMap.of(
               "beta", "DateTime",
               "gamma", "EnumList",
               "delta", "String"),
            HashMap.of(
               "beta", Property.of(dateTime),
               "gamma", Property.of(setList),
               "delta", Property.of("abcde"))),
         Arguments.of(
            Schema.empty().withColumnRenamed("alpha", "beta"),
            HashMap.of(
               "beta", "String",
               "gamma", "EnumList"),
            HashMap.of(
               "beta", Property.of("abcde"),
               "gamma", Property.of(setList))),
         Arguments.of(
            Schema.empty().withColumnRenamed("alpha", "delta").withoutColumn("delta"),
            HashMap.of(
               "beta", "DateTime",
               "gamma", "EnumList"),
            HashMap.of(
               "beta", Property.of(dateTime),
               "gamma", Property.of(setList))),
         Arguments.of(
            Schema.empty().withColumnRenamed("alpha", "beta").withoutColumn("beta"),
            HashMap.of("gamma", "EnumList"),
            HashMap.of("gamma", Property.of(setList))),
         Arguments.of(
            Schema.empty()
               .withColumnRenamed("alpha", "delta")
               .withColumn("alpha", TypeDescriptor.fromTypeName("Integer")),
            HashMap.of(
               "alpha", "Integer",
               "beta", "DateTime",
               "gamma", "EnumList",
               "delta", "String"),
            HashMap.of(
               "alpha", Property.empty(),
               "beta", Property.of(dateTime),
               "gamma", Property.of(setList),
               "delta", Property.of("abcde"))),
         Arguments.of(
            Schema.empty()
               .withColumnRenamed("alpha", "delta")
               .withColumn("delta", TypeDescriptor.fromTypeName("Integer")),
            HashMap.of(
               "beta", "DateTime",
               "gamma", "EnumList",
               "delta", "Integer"),
            HashMap.of(
               "beta", Property.of(dateTime),
               "gamma", Property.of(setList),
               "delta", Property.empty())),
         Arguments.of(
            Schema.empty()
               .withColumnRenamed("alpha", "delta")
               .withColumnRenamed("delta", "epsilon"),
            HashMap.of(
               "beta", "DateTime",
               "gamma", "EnumList",
               "epsilon", "String"),
            HashMap.of(
               "beta", Property.of(dateTime),
               "gamma", Property.of(setList),
               "epsilon", Property.of("abcde"))),
         Arguments.of(
            Schema.empty()
               .withColumnRenamed("alpha", "delta")
               .withoutColumn("delta")
               .withColumnRenamed("gamma", "delta"),
            HashMap.of(
               "beta", "DateTime",
               "delta", "EnumList"),
            HashMap.of(
               "beta", Property.of(dateTime),
               "delta", Property.of(setList))),
         Arguments.of(
            Schema.empty()
               .withColumn("delta", TypeDescriptor.fromTypeName("Integer"))
               .withColumnRenamed("delta", "epsilon"),
            HashMap.of(
               "alpha", "String",
               "beta", "DateTime",
               "gamma", "EnumList",
               "epsilon", "Integer"),
            HashMap.of(
               "alpha", Property.of("abcde"),
               "beta", Property.of(dateTime),
               "gamma", Property.of(setList),
               "epsilon", Property.empty())),
         Arguments.of(
            Schema.empty()
               .withColumn("beta", TypeDescriptor.fromTypeName("Integer"))
               .withColumnRenamed("beta", "epsilon"),
            HashMap.of(
               "alpha", "String",
               "gamma", "EnumList",
               "delta", "Integer"),
            HashMap.of(
               "alpha", Property.of("abcde"),
               "gamma", Property.of(setList),
               "delta", Property.empty())),
         Arguments.of(
            Schema.empty()
               .withColumn("delta", TypeDescriptor.fromTypeName("Integer"))
               .withColumnRenamed("delta", "epsilon")
               .withoutColumn("epsilon"),
            HashMap.of(
               "alpha", "String",
               "beta", "DateTime",
               "gamma", "EnumList"),
            HashMap.of(
               "alpha", Property.of("abcde"),
               "beta", Property.of(dateTime),
               "gamma", Property.of(setList))),
         Arguments.of(
            Schema.empty()
               .withColumn("delta", TypeDescriptor.fromTypeName("Integer"))
               .withColumnRenamed("delta", "epsilon")
               .withColumnRenamed("epsilon", "zeta"),
            HashMap.of(
               "alpha", "String",
               "beta", "DateTime",
               "gamma", "EnumList",
               "zeta", "Integer"),
            HashMap.of(
               "alpha", Property.of("abcde"),
               "beta", Property.of(dateTime),
               "gamma", Property.of(setList),
               "zeta", Property.empty())));
   }

   @Test
   void testMergeInvalidRenameNonexistent() {
      Schema delta = Schema.empty().withColumnRenamed("delta", "beta");
      assertThatExceptionOfType(NoSuchElementException.class)
         .isThrownBy(() -> baseSchema.merge(delta));
      assertThatExceptionOfType(NoSuchElementException.class)
         .isThrownBy(() -> delta.asPropertiesDelta(baseProperties));
   }

   @Test
   void testInvalidDeleteRename() {
      Schema delta = Schema.empty().withoutColumn("alpha");
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> delta.withColumnRenamed("alpha", "delta"));
   }

   @Test
   void testInvalidRenameRename() {
      Schema delta = Schema.empty().withColumnRenamed("alpha", "delta");
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> delta.withColumnRenamed("alpha", "epsilon"));
   }

   @Test
   void testAsMapInvalidDelete() {
      Schema delta = Schema.empty().withoutColumn("alpha");
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> delta.asMap());
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> delta.toJson());
   }

   @Test
   void testAsMapInvalidRename() {
      Schema delta = Schema.empty()
         .withColumnRenamed("alpha", "delta")
         .withColumn("alpha", TypeDescriptor.fromTypeName("Integer"));
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> delta.asMap());
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> delta.toJson());
   }
}
