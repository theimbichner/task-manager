package io.github.theimbichner.taskmanager.task.property;

import org.json.JSONObject;

public class SimpleTypeDescriptor implements TypeDescriptor {
   private final String typeName;

   public SimpleTypeDescriptor(String typeName) {
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
   public Property getDefaultValue() {
      switch (typeName) {
         case "String":
            return Property.of("");
         case "Boolean":
            return Property.of(false);
         default:
            return Property.of(null);
      }
   }
}
