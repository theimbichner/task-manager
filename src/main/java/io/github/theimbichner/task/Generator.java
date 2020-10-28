package io.github.theimbichner.task;

import java.util.UUID;

import org.json.JSONObject;

public class Generator {
   private String id;

   public Generator() {
      id = UUID.randomUUID().toString();
   }

   public String getId() {
      return id;
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("id", id);
      return json;
   }

   public static Generator fromJson(JSONObject json) {
      Generator result = new Generator();
      result.id = json.getString("id");
      return result;
   }
}
