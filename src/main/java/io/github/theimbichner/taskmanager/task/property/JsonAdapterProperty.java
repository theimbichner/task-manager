package io.github.theimbichner.taskmanager.task.property;

import java.util.function.Function;

import org.json.JSONObject;

public class JsonAdapterProperty<T> extends Property {
   private final T data;
   private final String typeName;
   private final Function<T, Object> toJson;

   JsonAdapterProperty(T data, String typeName, Function<T, Object> toJson) {
      this.data = data;
      this.typeName = typeName;
      this.toJson = toJson;
   }

   @Override
   public T get() {
      return data;
   }

   @Override
   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("type", typeName);
      json.put("value", toJson.apply(data));

      return json;
   }
}
