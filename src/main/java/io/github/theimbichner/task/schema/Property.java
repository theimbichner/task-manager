package io.github.theimbichner.task.schema;

import java.util.Objects;

import org.json.JSONObject;

import io.github.theimbichner.task.time.DateTime;

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
         if (!(obj instanceof Property)) {
            return false;
         }

         Property other = (Property) obj;

         if (get() instanceof DateTime) {
            // TODO implement and test equals on DateTime
            if (!(other.get() instanceof DateTime)) {
               return false;
            }
            DateTime dateTime = (DateTime) get();
            DateTime otherDateTime = (DateTime) other.get();
            return dateTime.getStart().equals(otherDateTime.getStart())
               && dateTime.getEnd().equals(otherDateTime.getEnd());
         }
         return Objects.equals(get(), other.get());
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
