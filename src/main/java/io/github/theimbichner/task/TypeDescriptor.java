package io.github.theimbichner.task;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

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

   public static TypeDescriptor fromJson(JSONObject json) {
      if (json.opt("typeName") != null) {
         return new Simple(json.getString("typeName"));
      }

      boolean permitMultiple = json.getBoolean("permitMultiple");
      JSONArray jsonArray = json.getJSONArray("enumValues");
      Set<String> enumValues = new HashSet<>();
      for (int i = 0; i < jsonArray.length(); i++) {
         enumValues.add(jsonArray.getString(i));
      }
      return new Enumeration(permitMultiple, enumValues);
   }

   String getTypeName();
   JSONObject toJson();
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
      public JSONObject toJson() {
         JSONObject result = new JSONObject();
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
      public JSONObject toJson() {
         JSONObject result = new JSONObject();
         result.put("permitMultiple", permitMultiple);
         result.put("enumValues", enumValues);
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
