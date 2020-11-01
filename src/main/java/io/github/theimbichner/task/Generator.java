package io.github.theimbichner.task;

import java.util.UUID;

import org.json.JSONObject;

public class Generator {
   private final String id;

   private Generator(String id) {
      this.id = id;
   }

   public String getId() {
      return id;
   }

   public static Generator createGenerator() {
      return new Generator(UUID.randomUUID().toString());
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("id", id);
      return json;
   }

   public static Generator fromJson(JSONObject json) {
      String id = json.getString("id");
      Generator result = new Generator(id);
      return result;
   }
}
