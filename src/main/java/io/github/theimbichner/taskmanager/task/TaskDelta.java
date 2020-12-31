package io.github.theimbichner.taskmanager.task;

import java.util.Optional;

import io.github.theimbichner.taskmanager.task.property.PropertyMap;

public class TaskDelta {
   private final PropertyMap properties;
   private final String name;
   private final String markup;
   private final Long duration;

   public TaskDelta(
      PropertyMap properties,
      String name,
      String markup,
      Long duration
   ) {
      this.properties = properties;
      this.name = name;
      this.markup = markup;
      this.duration = duration;
   }

   public PropertyMap getProperties() {
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
      return name == null && markup == null && duration == null && properties.asMap().isEmpty();
   }
}
