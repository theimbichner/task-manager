package io.github.theimbichner.taskmanager.task.property;

import io.vavr.Tuple2;
import io.vavr.collection.HashMap;

import org.json.JSONObject;

public class Schema {
   private static interface Column {
      public static class Concrete implements Column {
         private final TypeDescriptor type;

         private Concrete(TypeDescriptor type) {
            this.type = type;
         }

         @Override
         public Property resolveProperty(PropertyMap properties) {
            return type.getDefaultValue();
         }

         @Override
         public TypeDescriptor asTypeDescriptor() {
            return type;
         }
      }

      public static class Renamed implements Column {
         private final String originalName;

         private Renamed(String originalName) {
            this.originalName = originalName;
         }

         @Override
         public Property resolveProperty(PropertyMap properties) {
            return properties.asMap().get(originalName).get();
         }

         @Override
         public Column resolveColumn(Schema schema) {
            return schema.columns.get(originalName).get();
         }
      }

      public static final Column DELETE = new Column() {
         @Override
         public Property resolveProperty(PropertyMap properties) {
            return Property.DELETE;
         }
      };

      public Property resolveProperty(PropertyMap properties);

      public default Column resolveColumn(Schema schema) {
         return this;
      }

      public default TypeDescriptor asTypeDescriptor() {
         throw new IllegalStateException("Cannot get type of non-concrete column.");
      }
   }

   private final HashMap<String, Column> columns;

   private Schema(HashMap<String, Column> columns) {
      this.columns = columns;
   }

   public static Schema empty() {
      return new Schema(HashMap.empty());
   }

   public Schema withColumn(String name, TypeDescriptor value) {
      return new Schema(columns.put(name, new Column.Concrete(value)));
   }

   public Schema withoutColumn(String name) {
      return new Schema(columns.put(name, Column.DELETE));
   }

   public Schema withColumnRenamed(String oldName, String newName) {
      Column newColumn = columns.get(oldName).getOrElse(new Column.Renamed(oldName));
      if (newColumn == Column.DELETE) {
         throw new IllegalStateException("Cannot rename deleted column.");
      }

      return new Schema(columns.put(newName, newColumn).put(oldName, Column.DELETE));
   }

   public Schema merge(Schema delta) {
      HashMap<String, Column> result = columns;

      for (Tuple2<String, Column> entry : delta.columns) {
         result = result.put(entry._1, entry._2.resolveColumn(this));
      }

      return new Schema(result.filter((k, v) -> v != Column.DELETE));
   }

   public PropertyMap asPropertiesDelta(PropertyMap base) {
      PropertyMap result = PropertyMap.empty();

      for (Tuple2<String, Column> entry : columns) {
         result = result.put(entry._1, entry._2.resolveProperty(base));
      }

      return result;
   }

   public boolean isEmpty() {
      return columns.isEmpty();
   }

   public HashMap<String, TypeDescriptor> asMap() {
      return columns.mapValues(Column::asTypeDescriptor);
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();

      for (Tuple2<String, TypeDescriptor> entry : asMap()) {
         json.put(entry._1, entry._2.toJson());
      }

      return json;
   }

   public static Schema fromJson(JSONObject json) {
      HashMap<String, Column> result = HashMap.empty();

      for (String s : json.keySet()) {
         TypeDescriptor type = TypeDescriptor.fromJson(json.getJSONObject(s));
         result = result.put(s, new Column.Concrete(type));
      }

      return new Schema(result);
   }
}
