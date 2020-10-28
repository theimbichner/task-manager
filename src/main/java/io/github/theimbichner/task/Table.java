package io.github.theimbichner.task;

import java.util.UUID;

import org.json.JSONObject;

public class Table {
   private String id;

   public Table() {
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

   public static Table fromJson(JSONObject json) {
      Table result = new Table();
      result.id = json.getString("id");
      return result;
   }
}
