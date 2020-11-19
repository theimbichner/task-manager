package io.github.theimbichner.task;

import java.util.Map;
import java.util.Optional;

import io.github.theimbichner.task.schema.Property;

public class GeneratorDelta {
   private final Map<String, Property> properties;
   private final Optional<String> name;
   private final Optional<String> templateName;
   private final Optional<Optional<String>> templateMarkup;
   private final Optional<Long> templateDuration;

   public GeneratorDelta(
      Map<String, Property> properties,
      String name,
      String templateName,
      Optional<String> templateMarkup,
      Long templateDuration
   ) {
      this.properties = Map.copyOf(properties);
      this.name = Optional.ofNullable(name);
      this.templateName = Optional.ofNullable(templateName);
      this.templateMarkup = Optional.ofNullable(templateMarkup);
      this.templateDuration = Optional.ofNullable(templateDuration);
   }

   public Map<String, Property> getProperties() {
      return properties;
   }

   public Optional<String> getName() {
      return name;
   }

   public Optional<String> getTemplateName() {
      return templateName;
   }

   public Optional<Optional<String>> getTemplateMarkup() {
      return templateMarkup;
   }

   public Optional<Long> getTemplateDuration() {
      return templateDuration;
   }

   public boolean isEmpty() {
      return name.isEmpty()
         && templateName.isEmpty()
         && templateMarkup.isEmpty()
         && templateDuration.isEmpty()
         && properties.isEmpty();
   }

   public TaskDelta asTaskDelta() {
      return new TaskDelta(
         properties,
         templateName.orElse(null),
         templateMarkup.orElse(null),
         templateDuration.orElse(null));
   }
}
