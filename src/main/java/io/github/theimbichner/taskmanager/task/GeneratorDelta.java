package io.github.theimbichner.taskmanager.task;

import java.util.Optional;

import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.time.DateTime;

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

   public TaskDelta asTaskDelta(String generationField, PropertyMap taskProperties) {
      PropertyMap taskDeltaProperties = PropertyMap.empty();
      if (properties != null) {
         taskDeltaProperties = properties;
      }
      if (templateDuration != null) {
         DateTime initialDateTime = (DateTime) taskProperties
            .asMap()
            .get(generationField)
            .get()
            .get();
         DateTime newDateTime = initialDateTime.withDuration(templateDuration);
         taskDeltaProperties = taskDeltaProperties
            .put(generationField, Property.of(newDateTime));
      }
      return new TaskDelta(taskDeltaProperties, templateName, templateMarkup);
   }
}
