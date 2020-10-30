package io.github.theimbichner.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.theimbichner.task.time.DateTime;

public class Table {
   private String id;
   private String name;
   private DateTime dateCreated;
   private DateTime dateLastModified;
   private final List<String> taskIds;
   private final List<String> generatorIds;
   private final Map<String, TypeDescriptor> schema;

   public Table() {
      id = UUID.randomUUID().toString();
      name = "";
      dateCreated = new DateTime();
      dateLastModified = dateCreated;
      taskIds = new ArrayList<>();
      generatorIds = new ArrayList<>();
      schema = new HashMap<>();
   }

   public String getId() {
      return id;
   }

   public String getName() {
      return name;
   }

   public DateTime getDateCreated() {
      return dateCreated;
   }

   public DateTime getDateLastModified() {
      return dateLastModified;
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("id", id);
      json.put("name", name);
      json.put("dateCreated", dateCreated.toJson());
      json.put("dateLastModified", dateLastModified.toJson());
      json.put("tasks", taskIds);
      json.put("generators", generatorIds);

      JSONObject schemaJson = new JSONObject();
      for (String s : schema.keySet()) {
         schemaJson.put(s, schema.get(s).toJson());
      }
      json.put("schema", schemaJson);

      return json;
   }

   public static Table fromJson(JSONObject json) {
      Table result = new Table();
      result.id = json.getString("id");
      result.name = json.getString("name");
      result.dateCreated = DateTime.fromJson(json.getJSONObject("dateCreated"));
      result.dateLastModified = DateTime.fromJson(json.getJSONObject("dateLastModified"));

      JSONArray tasksJson = json.getJSONArray("tasks");
      for (int i = 0; i < tasksJson.length(); i++) {
         result.taskIds.add(tasksJson.getString(i));
      }

      JSONArray generatorsJson = json.getJSONArray("generators");
      for (int i = 0; i < generatorsJson.length(); i++) {
         result.generatorIds.add(generatorsJson.getString(i));
      }

      JSONObject schemaJson = json.getJSONObject("schema");
      for (String s : schemaJson.keySet()) {
         result.schema.put(s, TypeDescriptor.fromJson(schemaJson.getJSONObject(s)));
      }

      return result;
   }
}
