package io.github.theimbichner.taskmanager.task.property;

import java.util.Map;

import io.vavr.collection.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class PropertyMapTests {
   private PropertyMap propertyMap;

   @BeforeEach
   void beforeEach() {
      propertyMap = PropertyMap.fromJava(Map.of(
         "alpha", Property.of(1L),
         "beta", Property.of(2L)));
   }

   @Test
   void testMerge() {
      PropertyMap delta = PropertyMap.fromJava(Map.of(
         "alpha", Property.of("update"),
         "beta", Property.DELETE,
         "gamma", Property.of(3L),
         "delta", Property.DELETE));
      HashMap<String, Property> expected = HashMap.of(
         "alpha", Property.of("update"),
         "gamma", Property.of(3L));
      propertyMap = propertyMap.merge(delta);
      assertThat(propertyMap.asMap()).isEqualTo(expected);
   }

   @Test
   void testPut() {
      propertyMap = propertyMap.put("gamma", Property.of(3L));
      HashMap<String, Property> expected = HashMap.of(
         "alpha", Property.of(1L),
         "beta", Property.of(2L),
         "gamma", Property.of(3L));
      assertThat(propertyMap.asMap()).isEqualTo(expected);
   }

   @Test
   void testPutOverwrite() {
      propertyMap = propertyMap.put("alpha", Property.of(3L));
      HashMap<String, Property> expected = HashMap.of(
         "alpha", Property.of(3L),
         "beta", Property.of(2L));
      assertThat(propertyMap.asMap()).isEqualTo(expected);
   }

   @Test
   void testToFromJson() {
      // TODO add tests after fully implementing properties
   }
}
