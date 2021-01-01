package io.github.theimbichner.taskmanager.task.property;

import java.math.BigDecimal;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.time.DateTime;

public abstract class Property {
   public static final Property DELETE = new Property() {
      @Override
      public Object get() {
         return this;
      }

      @Override
      public JSONObject toJson() {
         return null;
      }

      @Override
      public int hashCode() {
         return 0;
      }
   };

   public abstract JSONObject toJson();
   public abstract Object get();

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof Property)) {
         return false;
      }

      Property other = (Property) obj;

      return Objects.equals(get(), other.get());
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(get());
   }

   public static Property fromJson(JSONObject json) {
      switch (json.optString("type", "")) {
      case "DateTime":
         DateTime dateTime = DateTime.fromJson(json.getJSONObject("value"));
         return Property.of(dateTime);

      case "EnumList":
         SetList<String> list = SetList.empty();
         JSONArray jsonArray = json.getJSONArray("value");
         for (int i = 0; i < jsonArray.length(); i++) {
            list = list.add(jsonArray.getString(i));
         }
         return Property.of(list);

      case "String":
         return Property.of(json.getString("value"));

      case "Boolean":
         return Property.of(json.getBoolean("value"));

      case "Number":
         return Property.of(json.getBigDecimal("value"));

      case "Empty":
         return Property.empty();

      default:
         throw new IllegalArgumentException("Could not recognize property json.");
      }
   }

   public static Property of(DateTime dateTime) {
      return new JsonAdapterProperty<>(dateTime, "DateTime", DateTime::toJson);
   }

   public static Property of(SetList<String> list) {
      return new JsonAdapterProperty<>(list, "EnumList", s -> new JSONArray(s.asList()));
   }

   public static Property of(String string) {
      return new JsonAdapterProperty<>(string, "String", x -> x);
   }

   public static Property of(Boolean bool) {
      return new JsonAdapterProperty<>(bool, "Boolean", x -> x);
   }

   public static Property of(Double d) {
      return new NumberProperty(BigDecimal.valueOf(d));
   }

   public static Property of(Long l) {
      return new NumberProperty(BigDecimal.valueOf(l));
   }

   public static Property of(BigDecimal bigDecimal) {
      return new NumberProperty(bigDecimal);
   }

   public static Property empty() {
      return new JsonAdapterProperty<>(null, "Empty", x -> JSONObject.NULL);
   }
}
