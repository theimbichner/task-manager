package io.github.theimbichner.task;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
         return new Enumeration(false, new HashSet<>());
      case "EnumList":
         return new Enumeration(true, new HashSet<>());
      default:
         throw new IllegalArgumentException();
      }
   }

   public static TypeDescriptor fromData(Map<String, Object> data) {
      if (data.get("typeName") != null) {
         return new Simple((String) data.get("typeName"));
      }
      Boolean permitMultiple = (Boolean) data.get("permitMultiple");
      String[] enumValues = (String[]) data.get("enumValues");
      return new Enumeration(permitMultiple, Set.of(enumValues));
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
      private final Set<String> enumValues;

      private Enumeration(boolean permitMultiple, Set<String> enumValues) {
         this.permitMultiple = permitMultiple;
         this.enumValues = Collections.unmodifiableSet(enumValues);
      }

      public Set<String> getEnumValues() {
         return enumValues;
      }

      public Enumeration withEnumValues(String... toAdd) {
         Set<String> newEnumValues = new HashSet<>(enumValues);
         newEnumValues.addAll(Set.of(toAdd));
         return new Enumeration(permitMultiple, newEnumValues);
      }

      public Enumeration withoutEnumValues(String... toRemove) {
         Set<String> newEnumValues = new HashSet<>(enumValues);
         newEnumValues.removeAll(Set.of(toRemove));
         return new Enumeration(permitMultiple, newEnumValues);
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
         result.put("enumValues", enumValues.toArray(new String[0]));
         return result;
      }

      @Override
      public Object getNewDefaultValueInstance() {
         if (permitMultiple) {
            return new LinkedHashSet<String>();
         }
         return null;
      }
   }
}
