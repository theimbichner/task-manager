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

   public TaskDelta asTaskDelta(PropertyMap baseProperties) {
      PropertyMap deltaProperties = schema.asPropertiesDelta(baseProperties);
      return new TaskDelta(deltaProperties, null, null, null);
   }

   public GeneratorDelta asGeneratorDelta(PropertyMap baseProperties) {
      PropertyMap deltaProperties = schema.asPropertiesDelta(baseProperties);
      return new GeneratorDelta(deltaProperties, null, null, null, null);
   }
}
