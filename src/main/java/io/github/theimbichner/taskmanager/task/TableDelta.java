package io.github.theimbichner.taskmanager.task;

import java.util.Optional;

import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.task.property.Schema;

public class TableDelta {
   private final Schema schema;
   private final String name;

   public TableDelta(Schema schema, String name) {
      this.schema = schema;
      this.name = name;
   }

   public Schema getSchema() {
      return schema;
   }

   public Optional<String> getName() {
      return Optional.ofNullable(name);
   }

   public TaskDelta asTaskDelta(Task task) {
      PropertyMap properties = schema.asPropertiesDelta(task.getProperties());
      return new TaskDelta(properties, null, null, null);
   }

   public GeneratorDelta asGeneratorDelta(Generator generator) {
      PropertyMap templateProperties = schema.asPropertiesDelta(generator.getTemplateProperties());
      return new GeneratorDelta(templateProperties, null, null, null, null);
   }
}
