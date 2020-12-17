package io.github.theimbichner.task;

import java.util.Map;
import java.util.Optional;

import io.github.theimbichner.task.schema.Property;

public class TaskDelta {
   private final Map<String, Property> properties;
   private final String name;
   private final String markup;
   private final Long duration;

   public TaskDelta(
      Map<String, Property> properties,
      String name,
      String markup,
      Long duration
   ) {
      this.properties = Map.copyOf(properties);
      this.name = name;
      this.markup = markup;
      this.duration = duration;
   }

   public Map<String, Property> getProperties() {
      return properties;
   }

   public Optional<String> getName() {
      return Optional.ofNullable(name);
   }

   public Optional<String> getMarkup() {
      return Optional.ofNullable(markup);
   }

   public Optional<Long> getDuration() {
      return Optional.ofNullable(duration);
   }

   public boolean isEmpty() {
      return name == null && markup == null && duration == null && properties.isEmpty();
   }
}
