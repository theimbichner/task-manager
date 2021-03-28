package io.github.theimbichner.taskmanager.task;

import io.vavr.control.Option;

import io.github.theimbichner.taskmanager.task.property.PropertyMap;

public class TaskDelta {
   private final PropertyMap properties;
   private final String name;
   private final String markup;

   public TaskDelta(
      PropertyMap properties,
      String name,
      String markup
   ) {
      this.properties = properties;
      this.name = name;
      this.markup = markup;
   }

   public PropertyMap getProperties() {
      return properties;
   }

   public Option<String> getName() {
      return Option.of(name);
   }

   public Option<String> getMarkup() {
      return Option.of(markup);
   }

   public boolean isEmpty() {
      return name == null && markup == null && properties.asMap().isEmpty();
   }
}
