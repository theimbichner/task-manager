package io.github.theimbichner.task.schema;

import java.util.Objects;

import org.json.JSONObject;

public interface Property {
   class SimpleExposingProperty implements Property {
      private final Object obj;

      private SimpleExposingProperty(Object obj) {
         this.obj = obj;
      }

      @Override
      public Object get() {
         return obj;
      }

      @Override
      public JSONObject toJson() {
         return new JSONObject();
      }

      @Override
      public boolean equals(Object obj) {
         if (obj instanceof Property) {
            return Objects.equals(get(), ((Property) obj).get());
         }
         return false;
      }
   }

   Property DELETE = new Property() {
      @Override
      public Object get() {
         return this;
      }

      @Override
      public JSONObject toJson() {
         return null;
      }
   };

   JSONObject toJson();
   Object get();

   static Property fromJson(JSONObject json) {
      return Property.of(null);
   }

   static Property of(Object obj) {
      return new SimpleExposingProperty(obj);
   }
}
