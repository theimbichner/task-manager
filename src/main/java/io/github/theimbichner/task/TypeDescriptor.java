package io.github.theimbichner.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Notion data types not implemented:
 * Person
 * File
 * Url
 * Email
 * Phone
 * Relation/Rollup
 * Creator
 * LastModifier
 */
public interface TypeDescriptor {
   public static TypeDescriptor fromTypeName(String typeName) {
      switch (typeName) {
      case "Number":
      case "Integer":
      case "String":
      case "Boolean":
      case "DateTime":
         return new Simple(typeName);
      case "Enum":
         return new Enumeration(false);
      case "EnumList":
         return new Enumeration(true);
      default:
         throw new IllegalArgumentException();
      }
   }

   public static TypeDescriptor fromData(Map<String, Object> data) {
      if (data.get("typeName") != null) {
         return new Simple((String) data.get("typeName"));
      }
      return new Enumeration((Boolean) data.get("permitMultiple"))
         .withEnumValues((List<String>) data.get("enumValues"));
   }

   String getTypeName();
   Map<String, Object> toData();
   Object getNewDefaultValueInstance();

   public static class Simple implements TypeDescriptor {
      private final String typeName;

      private Simple(String typeName) {
         this.typeName = typeName;
      }

      @Override
      public String getTypeName() {
         return typeName;
      }

      @Override
      public Map<String, Object> toData() {
         Map<String, Object> result = new HashMap<>();
         result.put("typeName", typeName);
         return result;
      }

      @Override
      public Object getNewDefaultValueInstance() {
         switch (typeName) {
         case "String":
            return "";
         case "Boolean":
            return false;
         default:
            return null;
         }
      }
   }

   public static class Enumeration implements TypeDescriptor {
      private final boolean permitMultiple;
      private final List<String> enumValues;

      private Enumeration(boolean permitMultiple) {
         this.permitMultiple = permitMultiple;
         this.enumValues = new ArrayList<>();
      }

      public List<String> getEnumValues() {
         return new ArrayList<>(enumValues);
      }

      public Enumeration withEnumValues(Collection<String> enumValues) {
         this.enumValues.addAll(enumValues);
         return this;
      }

      public Enumeration withoutEnumValues(Collection<String> enumValues) {
         this.enumValues.removeAll(enumValues);
         return this;
      }

      @Override
      public String getTypeName() {
         if (permitMultiple) {
            return "EnumList";
         }
         return "Enum";
      }

      @Override
      public Map<String, Object> toData() {
         Map<String, Object> result = new HashMap<>();
         result.put("permitMultiple", permitMultiple);
         result.put("enumValues", getEnumValues());
         return result;
      }

      @Override
      public Object getNewDefaultValueInstance() {
         if (permitMultiple) {
            return new ArrayList<String>();
         }
         return null;
      }
   }
}
