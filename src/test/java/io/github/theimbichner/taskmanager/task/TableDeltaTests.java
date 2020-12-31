package io.github.theimbichner.taskmanager.task;

import java.util.Map;
import java.util.Set;

import io.vavr.collection.HashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.TypeDescriptor;

import static org.assertj.core.api.Assertions.*;

public class TableDeltaTests {
   static Map<String, TypeDescriptor> properties;
   static HashMap<String, Property> taskProperties;
   static String name;

   @BeforeAll
   static void beforeAll() {
      properties = Map.of(
         "alpha", TypeDescriptor.fromTypeName("EnumList"),
         "beta", TypeDescriptor.fromTypeName("String"),
         "gamma", TypeDescriptor.fromTypeName("Boolean"));
      taskProperties = HashMap.of(
         "alpha", Property.of(Set.of()),
         "beta", Property.of(""),
         "gamma", Property.of(false));
      name = "delta";
   }

   @Test
   void testName() {
      TableDelta delta = new TableDelta(properties, name);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).hasValue(name);
   }

   @Test
   void testNoName() {
      TableDelta delta = new TableDelta(properties, null);
      assertThat(delta.getProperties()).isEqualTo(properties);
      assertThat(delta.getName()).isEmpty();
   }

   @Test
   void testAsTaskDelta() {
      TableDelta tableDelta = new TableDelta(properties, name);
      TaskDelta taskDelta = tableDelta.asTaskDelta();
      assertThat(taskDelta.getProperties().asMap()).isEqualTo(taskProperties);
      assertThat(taskDelta.getName()).isEmpty();
      assertThat(taskDelta.getMarkup()).isEmpty();
      assertThat(taskDelta.getDuration()).isEmpty();
   }

   @Test
   void testAsTaskDeltaNoProperties() {
      TableDelta tableDelta = new TableDelta(Map.of(), name);
      TaskDelta taskDelta = tableDelta.asTaskDelta();
      assertThat(taskDelta.getProperties().asMap()).isEmpty();
   }

   @Test
   void testAsGeneratorDelta() {
      TableDelta tableDelta = new TableDelta(properties, name);
      GeneratorDelta generatorDelta = tableDelta.asGeneratorDelta();
      assertThat(generatorDelta.getProperties().asMap()).isEqualTo(taskProperties);
      assertThat(generatorDelta.getName()).isEmpty();
      assertThat(generatorDelta.getTemplateName()).isEmpty();
      assertThat(generatorDelta.getTemplateMarkup()).isEmpty();
      assertThat(generatorDelta.getTemplateDuration()).isEmpty();
   }

   @Test
   void testAsGeneratorDeltaNoProperties() {
      TableDelta tableDelta = new TableDelta(Map.of(), name);
      GeneratorDelta generatorDelta = tableDelta.asGeneratorDelta();
      assertThat(generatorDelta.getProperties().asMap()).isEmpty();
   }
}
