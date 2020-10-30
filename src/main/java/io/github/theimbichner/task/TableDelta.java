package io.github.theimbichner.task;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TableDelta {
   private final Map<String, TypeDescriptor> properties;
   private final Optional<String> name;

   public TableDelta(Map<String, TypeDescriptor> properties, String name) {
      this.properties = Map.copyOf(properties);
      this.name = Optional.ofNullable(name);
   }

   public Map<String, TypeDescriptor> getProperties() {
      return properties;
   }

   public Optional<String> getName() {
      return name;
   }

   private Map<String, Object> getTaskProperties() {
      Map<String, Object> result = new HashMap<>();
      for (String s : properties.keySet()) {
         result.put(s, Optional.ofNullable(properties.get(s))
            .map(TypeDescriptor::getNewDefaultValueInstance)
            .orElse(null));
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
