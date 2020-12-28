package io.github.theimbichner.task;

import java.util.Optional;

import io.github.theimbichner.task.schema.PropertyMap;

public class GeneratorDelta {
   private final PropertyMap properties;
   private final String name;
   private final String templateName;
   private final String templateMarkup;
   private final Long templateDuration;

   public GeneratorDelta(
      PropertyMap properties,
      String name,
      String templateName,
      String templateMarkup,
      Long templateDuration
   ) {
      this.properties = properties;
      this.name = name;
      this.templateName = templateName;
      this.templateMarkup = templateMarkup;
      this.templateDuration = templateDuration;
   }

   public PropertyMap getProperties() {
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
         && properties.asMap().isEmpty();
   }

   public TaskDelta asTaskDelta() {
      return new TaskDelta(properties, templateName, templateMarkup, templateDuration);
   }
}
