package io.github.theimbichner.task;

import java.util.Map;
import java.util.Optional;

import io.github.theimbichner.task.schema.Property;

public class TaskDelta {
   private final Map<String, Property> properties;
   private final Optional<String> name;
   private final Optional<Optional<String>> markup;
   private final Optional<Long> duration;

   public TaskDelta(
      Map<String, Property> properties,
      String name,
      Optional<String> markup,
      Long duration
   ) {
      this.properties = Map.copyOf(properties);
      this.name = Optional.ofNullable(name);
      this.markup = Optional.ofNullable(markup);
      this.duration = Optional.ofNullable(duration);
   }

   public Map<String, Property> getProperties() {
      return properties;
   }

   public Optional<String> getName() {
      return name;
   }

   public Optional<Optional<String>> getMarkup() {
      return markup;
   }

   public Optional<Long> getDuration() {
      return duration;
   }

   public boolean isEmpty() {
      return name.isEmpty() && markup.isEmpty() && duration.isEmpty() && properties.isEmpty();
   }
}
