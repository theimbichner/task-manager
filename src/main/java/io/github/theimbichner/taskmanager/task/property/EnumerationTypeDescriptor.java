package io.github.theimbichner.taskmanager.task.property;

import io.vavr.collection.Vector;

import org.json.JSONObject;

import io.github.theimbichner.taskmanager.collection.SetList;

public class EnumerationTypeDescriptor implements TypeDescriptor {
   private final boolean permitMultiple;
   private final SetList<String> enumValues;

   public EnumerationTypeDescriptor(boolean permitMultiple, SetList<String> enumValues) {
      this.permitMultiple = permitMultiple;
      this.enumValues = enumValues;
   }

   public SetList<String> getEnumValues() {
      return enumValues;
   }

   public EnumerationTypeDescriptor withEnumValues(String... toAdd) {
      return new EnumerationTypeDescriptor(
         permitMultiple,
         enumValues.addAll(Vector.of(toAdd)));
   }

   public EnumerationTypeDescriptor withoutEnumValues(String... toRemove) {
      return new EnumerationTypeDescriptor(
         permitMultiple,
         enumValues.removeAll(Vector.of(toRemove)));
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
      result.put("enumValues", enumValues.asList().asJava());
      return result;
   }

   @Override
   public Property getDefaultValue() {
      if (permitMultiple) {
         return Property.of(SetList.empty());
      }
      return Property.empty();
   }
}
