package io.github.theimbichner.taskmanager.task.property;

import java.util.HashSet;
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
   static TypeDescriptor fromTypeName(String typeName) {
      switch (typeName) {
      case "Number":
      case "Integer":
      case "String":
      case "Boolean":
      case "DateTime":
         return new SimpleTypeDescriptor(typeName);
      case "Enum":
         return new EnumerationTypeDescriptor(false, new HashSet<>());
      case "EnumList":
         return new EnumerationTypeDescriptor(true, new HashSet<>());
      default:
         throw new IllegalArgumentException();
      }
   }

   static TypeDescriptor fromJson(JSONObject json) {
      if (json.opt("typeName") != null) {
         return new SimpleTypeDescriptor(json.getString("typeName"));
      }

      boolean permitMultiple = json.getBoolean("permitMultiple");
      JSONArray jsonArray = json.getJSONArray("enumValues");
      Set<String> enumValues = new HashSet<>();
      for (int i = 0; i < jsonArray.length(); i++) {
         enumValues.add(jsonArray.getString(i));
      }
      return new EnumerationTypeDescriptor(permitMultiple, enumValues);
   }

   String getTypeName();
   JSONObject toJson();
   Property getDefaultValue();
}
