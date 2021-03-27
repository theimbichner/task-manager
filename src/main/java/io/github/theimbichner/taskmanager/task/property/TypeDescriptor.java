package io.github.theimbichner.taskmanager.task.property;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.theimbichner.taskmanager.collection.SetList;

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
         return new EnumerationTypeDescriptor(false, SetList.empty());
      case "EnumList":
         return new EnumerationTypeDescriptor(true, SetList.empty());
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
      SetList<String> enumValues = SetList.empty();
      for (int i = 0; i < jsonArray.length(); i++) {
         enumValues = enumValues.add(jsonArray.getString(i));
      }
      return new EnumerationTypeDescriptor(permitMultiple, enumValues);
   }

   String getTypeName();
   JSONObject toJson();
   Property getDefaultValue();
}
