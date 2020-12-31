package io.github.theimbichner.taskmanager.task.property;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.json.JSONObject;

public class EnumerationTypeDescriptor implements TypeDescriptor {
   private final boolean permitMultiple;
   private final Set<String> enumValues;

   public EnumerationTypeDescriptor(boolean permitMultiple, Set<String> enumValues) {
      this.permitMultiple = permitMultiple;
      this.enumValues = Collections.unmodifiableSet(enumValues);
   }

   public Set<String> getEnumValues() {
      return enumValues;
   }

   public EnumerationTypeDescriptor withEnumValues(String... toAdd) {
      Set<String> newEnumValues = new HashSet<>(enumValues);
      newEnumValues.addAll(Set.of(toAdd));
      return new EnumerationTypeDescriptor(permitMultiple, newEnumValues);
   }

   public EnumerationTypeDescriptor withoutEnumValues(String... toRemove) {
      Set<String> newEnumValues = new HashSet<>(enumValues);
      newEnumValues.removeAll(Set.of(toRemove));
      return new EnumerationTypeDescriptor(permitMultiple, newEnumValues);
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
   public Property getDefaultValue() {
      if (permitMultiple) {
         return Property.of(new LinkedHashSet<String>());
      }
      return Property.of(null);
   }
}
