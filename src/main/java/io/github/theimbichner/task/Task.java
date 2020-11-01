package io.github.theimbichner.task;

import java.util.UUID;

import org.json.JSONObject;

public class Task {
   private final String id;

   public Task(String id) {
      this.id = id;
   }

   public String getId() {
      return id;
   }

   public static Task createTask() {
      return new Task(UUID.randomUUID().toString());
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("id", id);
      return json;
   }

   public static Task fromJson(JSONObject json) {
      String id = json.getString("id");
      Task result = new Task(id);
      return result;
   }
}
