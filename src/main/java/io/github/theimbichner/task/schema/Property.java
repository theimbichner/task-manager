package io.github.theimbichner.task.schema;

import org.json.JSONObject;

public interface Property {
   JSONObject toJson();

   public static Property fromJson(JSONObject json) {
      return null;
   }
}
