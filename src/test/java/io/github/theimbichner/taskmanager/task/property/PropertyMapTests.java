package io.github.theimbichner.taskmanager.task.property;

import java.time.Instant;
import java.util.Map;

import io.vavr.collection.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.time.DateTime;

import static org.assertj.core.api.Assertions.*;

public class PropertyMapTests {
   private PropertyMap propertyMap;

   @BeforeEach
   void beforeEach() {
      propertyMap = PropertyMap.fromJava(Map.of(
         "alpha", Property.ofNumber("1"),
         "beta", Property.ofNumber("2")));
   }

   @Test
   void testMerge() {
      PropertyMap delta = PropertyMap.fromJava(Map.of(
         "alpha", Property.of("update"),
         "beta", Property.DELETE,
         "gamma", Property.ofNumber("3"),
         "delta", Property.DELETE));
      HashMap<String, Property> expected = HashMap.of(
         "alpha", Property.of("update"),
         "gamma", Property.ofNumber("3"));
      propertyMap = propertyMap.merge(delta);
      assertThat(propertyMap.asMap()).isEqualTo(expected);
   }

   @Test
   void testPut() {
      propertyMap = propertyMap.put("gamma", Property.ofNumber("3"));
      HashMap<String, Property> expected = HashMap.of(
         "alpha", Property.ofNumber("1"),
         "beta", Property.ofNumber("2"),
         "gamma", Property.ofNumber("3"));
      assertThat(propertyMap.asMap()).isEqualTo(expected);
   }

   @Test
   void testPutOverwrite() {
      propertyMap = propertyMap.put("alpha", Property.ofNumber("3"));
      HashMap<String, Property> expected = HashMap.of(
         "alpha", Property.ofNumber("3"),
         "beta", Property.ofNumber("2"));
      assertThat(propertyMap.asMap()).isEqualTo(expected);
   }

   @Test
   void testToFromJson() {
      PropertyMap propertyMap = PropertyMap.fromJava(Map.of(
         "alpha", Property.of(new DateTime(Instant.now())),
         "beta", Property.of(SetList.<String>empty().add("alpha").add("beta")),
         "gamma", Property.of("string"),
         "delta", Property.of(true),
         "epsilon", Property.ofNumber("2.25"),
         "zeta", Property.ofNumber("1"),
         "eta", Property.empty()));
      assertThat(PropertyMap.fromJson(propertyMap.toJson()).asMap())
         .isEqualTo(propertyMap.asMap());
   }
}
