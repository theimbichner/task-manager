package io.github.theimbichner.task;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.theimbichner.task.io.Storable;
import io.github.theimbichner.task.io.TaskStore;
import io.github.theimbichner.task.schema.Property;
import io.github.theimbichner.task.schema.TypeDescriptor;
import io.github.theimbichner.task.time.DateTime;

public class Table implements Storable {
   private final String id;
   private String name;
   private DateTime dateCreated;
   private DateTime dateLastModified;
   private final Set<String> taskIds;
   private final Set<String> generatorIds;
   private final Map<String, TypeDescriptor> schema;

   private TaskStore taskStore;

   private Table(String id) {
      this.id = id;
      name = "";
      dateCreated = new DateTime();
      dateLastModified = dateCreated;
      taskIds = new LinkedHashSet<>();
      generatorIds = new LinkedHashSet<>();
      schema = new HashMap<>();

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

   public Set<String> getAllTaskIds() {
      return Set.copyOf(taskIds);
   }

   public void linkTask(String id) {
      taskIds.add(id);
   }

   public void unlinkTask(String id) {
      taskIds.remove(id);
   }

   public Set<String> getAllGeneratorIds() {
      return Set.copyOf(generatorIds);
   }

   public void linkGenerator(String id) {
      generatorIds.add(id);
   }

   public void unlinkGenerator(String id) {
      generatorIds.remove(id);
   }

   @Override
   public void registerTaskStore(TaskStore taskStore) {
      this.taskStore = taskStore;
   }

   @Override
   public TaskStore getTaskStore() {
      return taskStore;
   }

   public Map<String, Property> getDefaultProperties() {
      return new HashMap<>();
   }

   public static Table createTable() {
      return new Table(UUID.randomUUID().toString());
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
      String id = json.getString("id");
      Table result = new Table(id);

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
