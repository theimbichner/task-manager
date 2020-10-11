package io.github.theimbichner.task;

import java.util.Map;
import java.util.Optional;

public class TaskDelta {
   private final Map<String, Object> properties;
   private final Optional<String> name;
   private final Optional<Optional<String>> markup;
   private final Optional<Integer> duration;

   public TaskDelta(
      Map<String, Object> properties,
      String name,
      Optional<String> markup,
      Integer duration
   ) {
      this.properties = Map.copyOf(properties);
      this.name = Optional.ofNullable(name);
      this.markup = Optional.ofNullable(markup);
      this.duration = Optional.ofNullable(duration);
   }

   public Map<String, Object> getProperties() {
      return properties;
   }

   public Optional<String> getName() {
      return name;
   }

   public Optional<Optional<String>> getMarkup() {
      return markup;
   }

   public Optional<Integer> getDuration() {
      return duration;
   }
}
