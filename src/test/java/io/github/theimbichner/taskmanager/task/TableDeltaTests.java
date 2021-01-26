package io.github.theimbichner.taskmanager.task;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.task.property.Schema;
import io.github.theimbichner.taskmanager.task.property.TypeDescriptor;

import static org.assertj.core.api.Assertions.*;

public class TableDeltaTests {
   static Schema schema;
   static String name;

   static Schema deltaSchema;
   static PropertyMap baseProperties;
   static PropertyMap deltaProperties;

   @BeforeAll
   static void beforeAll() {
      schema = Schema.empty()
         .withColumn("alpha", TypeDescriptor.fromTypeName("EnumList"))
         .withColumn("beta", TypeDescriptor.fromTypeName("String"))
         .withColumn("gamma", TypeDescriptor.fromTypeName("Boolean"));
      name = "delta";

      deltaSchema = Schema.empty()
         .withColumn("delta", TypeDescriptor.fromTypeName("Integer"))
         .withColumnRenamed("beta", "epsilon")
         .withoutColumn("gamma");
      baseProperties = PropertyMap.fromJava(Map.of(
         "alpha", Property.of(SetList.empty()),
         "beta", Property.of("abcde"),
         "gamma", Property.of(true)));
      deltaProperties = PropertyMap.fromJava(Map.of(
         "beta", Property.DELETE,
         "gamma", Property.DELETE,
         "delta", Property.empty(),
         "epsilon", Property.of("abcde")));
   }

   @Test
   void testFull() {
      TableDelta delta = new TableDelta(schema, name);
      assertThat(delta.getSchema().asMap()).isEqualTo(schema.asMap());
      assertThat(delta.getName()).hasValue(name);
      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testEmptySchema() {
      TableDelta delta = new TableDelta(Schema.empty(), name);
      assertThat(delta.getSchema().asMap()).isEmpty();
      assertThat(delta.getName()).hasValue(name);
      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testNoName() {
      TableDelta delta = new TableDelta(schema, null);
      assertThat(delta.getSchema().asMap()).isEqualTo(schema.asMap());
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.isEmpty()).isFalse();
   }

   @Test
   void testEmpty() {
      TableDelta delta = new TableDelta(Schema.empty(), null);
      assertThat(delta.getSchema().asMap()).isEmpty();
      assertThat(delta.getName()).isEmpty();
      assertThat(delta.isEmpty()).isTrue();
   }

   @Test
   void testAsTaskDelta() {
      TableDelta tableDelta = new TableDelta(deltaSchema, name);
      TaskDelta taskDelta = tableDelta.asTaskDelta(baseProperties);
      assertThat(taskDelta.getProperties().asMap()).isEqualTo(deltaProperties.asMap());
      assertThat(taskDelta.getName()).isEmpty();
      assertThat(taskDelta.getMarkup()).isEmpty();
      assertThat(taskDelta.getDuration()).isEmpty();
   }

   @Test
   void testAsTaskDeltaNoProperties() {
      TableDelta tableDelta = new TableDelta(Schema.empty(), name);
      TaskDelta taskDelta = tableDelta.asTaskDelta(baseProperties);
      assertThat(taskDelta.getProperties().asMap()).isEmpty();
   }
}
