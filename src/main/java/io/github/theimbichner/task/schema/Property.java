package io.github.theimbichner.task.schema;

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
         return null;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj instanceof Property) {
            return this.get().equals(((Property) obj).get());
         }
         return false;
      }
   }

   JSONObject toJson();
   Object get();

   public static Property fromJson(JSONObject json) {
      return null;
   }

   public static Property of(Object obj) {
      return new SimpleExposingProperty(obj);
   }
}
