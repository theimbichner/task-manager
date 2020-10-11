package io.github.theimbichner.task;

import java.util.Map;
import java.util.Optional;

public class GeneratorDelta {
   private final Map<String, Object> properties;
   private final Optional<String> name;
   private final Optional<String> templateName;
   private final Optional<Optional<String>> templateMarkup;
   private final Optional<Integer> templateDuration;

   public GeneratorDelta(
      Map<String, Object> properties,
      String name,
      String templateName,
      Optional<String> templateMarkup,
      Integer templateDuration
   ) {
      this.properties = Map.copyOf(properties);
      this.name = Optional.ofNullable(name);
      this.templateName = Optional.ofNullable(templateName);
      this.templateMarkup = Optional.ofNullable(templateMarkup);
      this.templateDuration = Optional.ofNullable(templateDuration);
   }

   public Map<String, Object> getProperties() {
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

   public Optional<Integer> getTemplateDuration() {
      return templateDuration;
   }

   public TaskDelta asTaskDelta() {
      return new TaskDelta(
         properties,
         templateName.orElse(null),
         templateMarkup.orElse(null),
         templateDuration.orElse(null));
   }
}
