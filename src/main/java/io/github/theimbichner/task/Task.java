package io.github.theimbichner.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;

import io.github.theimbichner.task.io.Storable;
import io.github.theimbichner.task.io.TaskStore;
import io.github.theimbichner.task.schema.Property;
import io.github.theimbichner.task.time.DateTime;

public class Task implements Storable {
   private final String id;
   private final String tableId;
   private String name;
   private DateTime dateCreated;
   private DateTime dateLastModified;
   private String markup;
   private String generatorId;
   private final Map<String, Property> properties;

   private TaskStore taskStore;

   private Task(String id, String tableId) {
      this.id = id;
      this.tableId = tableId;

      name = "";
      dateCreated = new DateTime();
      dateLastModified = dateCreated;
      markup = null;
      properties = new HashMap<>();
      generatorId = null;

      taskStore = null;
   }

   @Override
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

   public String getMarkup() {
      return markup;
   }

   public Property getProperty(String key) {
      return properties.get(key);
   }

   @Override
   public void registerTaskStore(TaskStore taskStore) {
      this.taskStore = taskStore;
   }

   @Override
   public TaskStore getTaskStore() {
      return taskStore;
   }

   public static Task createTask(Table table) {
      Task result = new Task(UUID.randomUUID().toString(), table.getId());
      result.properties.putAll(table.getDefaultProperties());
      result.registerTaskStore(table.getTaskStore());
      return result;
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("id", id);
      json.put("table", tableId);
      json.put("name", name);
      json.put("dateCreated", dateCreated.toJson());
      json.put("dateLastModified", dateLastModified.toJson());
      json.put("markup", markup == null ? JSONObject.NULL : markup);
      json.put("generator", generatorId == null ? JSONObject.NULL : generatorId);

      JSONObject propertiesJson = new JSONObject();
      for (String s : properties.keySet()) {
         propertiesJson.put(s, properties.get(s).toJson());
      }
      json.put("properties", propertiesJson);
      return json;
   }

   public static Task fromJson(JSONObject json) {
      String id = json.getString("id");
      String tableId = json.getString("table");
      Task result = new Task(id, tableId);

      result.name = json.getString("name");
      result.dateCreated = DateTime.fromJson(json.getJSONObject("dateCreated"));
      result.dateLastModified = DateTime.fromJson(json.getJSONObject("dateLastModified"));
      result.markup = json.isNull("markup") ? null : json.getString("markup");
      result.generatorId = json.isNull("generator") ? null : json.getString("generator");

      JSONObject propertiesJson = json.getJSONObject("properties");
      for (String s : propertiesJson.keySet()) {
         result.properties.put(s, Property.fromJson(propertiesJson.getJSONObject(s)));
      }

      return result;
   }
}
