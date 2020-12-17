package io.github.theimbichner.task;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.github.theimbichner.task.schema.Property;
import io.github.theimbichner.task.schema.TypeDescriptor;

public class TableDelta {
   private final Map<String, TypeDescriptor> properties;
   private final String name;

   public TableDelta(Map<String, TypeDescriptor> properties, String name) {
      this.properties = Map.copyOf(properties);
      this.name = name;
   }

   public Map<String, TypeDescriptor> getProperties() {
      return properties;
   }

   public Optional<String> getName() {
      return Optional.ofNullable(name);
   }

   private Map<String, Property> getTaskProperties() {
      Map<String, Property> result = new HashMap<>();
      for (String s : properties.keySet()) {
         result.put(s, properties.get(s) == null ? null : properties.get(s).getDefaultValue());
      }

      return result;
   }

   public TaskDelta asTaskDelta() {
      return new TaskDelta(getTaskProperties(), null, null, null);
   }

   public GeneratorDelta asGeneratorDelta() {
      return new GeneratorDelta(getTaskProperties(), null, null, null, null);
   }
}
