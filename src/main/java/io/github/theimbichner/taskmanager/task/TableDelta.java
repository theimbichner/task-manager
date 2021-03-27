package io.github.theimbichner.taskmanager.task;

import io.vavr.control.Option;

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

   public Option<String> getName() {
      return Option.of(name);
   }

   public boolean isEmpty() {
      return name == null && schema.isEmpty();
   }

   public TaskDelta asTaskDelta(PropertyMap baseProperties) {
      PropertyMap deltaProperties = schema.asPropertiesDelta(baseProperties);
      return new TaskDelta(deltaProperties, null, null);
   }
}
