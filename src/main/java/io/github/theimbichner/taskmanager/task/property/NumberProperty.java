package io.github.theimbichner.taskmanager.task.property;

import java.math.BigDecimal;

import org.json.JSONObject;

public class NumberProperty extends Property {
   private final BigDecimal bigDecimal;

   NumberProperty(BigDecimal bigDecimal) {
      this.bigDecimal = bigDecimal;
   }

   @Override
   public BigDecimal get() {
      return bigDecimal;
   }

   @Override
   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("type", "Number");
      json.put("value", bigDecimal);

      return json;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof NumberProperty)) {
         return false;
      }

      NumberProperty other = (NumberProperty) obj;

      return bigDecimal.compareTo(other.bigDecimal) == 0;
   }

   @Override
   public int hashCode() {
      return Double.hashCode(bigDecimal.doubleValue());
   }
}
