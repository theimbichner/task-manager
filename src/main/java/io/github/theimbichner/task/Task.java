package io.github.theimbichner.task;

import java.util.UUID;

import org.json.JSONObject;

public class Task {
   private String id;

   public Task() {
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

   public static Task fromJson(JSONObject json) {
      Task result = new Task();
      result.id = json.getString("id");
      return result;
   }
}
