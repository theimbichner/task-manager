package io.github.theimbichner.task;

import java.util.Map;
import java.util.Optional;

import io.github.theimbichner.task.schema.Property;

public class GeneratorDelta {
   private final Map<String, Property> properties;
   private final String name;
   private final String templateName;
   private final String templateMarkup;
   private final Long templateDuration;

   public GeneratorDelta(
      Map<String, Property> properties,
      String name,
      String templateName,
      String templateMarkup,
      Long templateDuration
   ) {
      this.properties = Map.copyOf(properties);
      this.name = name;
      this.templateName = templateName;
      this.templateMarkup = templateMarkup;
      this.templateDuration = templateDuration;
   }

   public Map<String, Property> getProperties() {
      return properties;
   }

   public Optional<String> getName() {
      return Optional.ofNullable(name);
   }

   public Optional<String> getTemplateName() {
      return Optional.ofNullable(templateName);
   }

   public Optional<String> getTemplateMarkup() {
      return Optional.ofNullable(templateMarkup);
   }

   public Optional<Long> getTemplateDuration() {
      return Optional.ofNullable(templateDuration);
   }

   public boolean isEmpty() {
      return name == null
         && templateName == null
         && templateMarkup == null
         && templateDuration == null
         && properties.isEmpty();
   }

   public TaskDelta asTaskDelta() {
      return new TaskDelta(properties, templateName, templateMarkup, templateDuration);
   }
}
