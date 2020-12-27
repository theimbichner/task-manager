package io.github.theimbichner.task;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.vavr.control.Either;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.theimbichner.task.collection.SetList;
import io.github.theimbichner.task.io.Storable;
import io.github.theimbichner.task.io.TaskAccessException;
import io.github.theimbichner.task.io.TaskStore;
import io.github.theimbichner.task.schema.Property;
import io.github.theimbichner.task.schema.PropertyMap;
import io.github.theimbichner.task.schema.TypeDescriptor;
import io.github.theimbichner.task.time.DateTime;
import io.github.theimbichner.task.time.ModifyRecord;

/*
 * TODO restrict user property names to avoid collision with system properties.
 * Name
 * Date Created
 * Date Last Modified
 */

public class Table implements Storable {
   private final String id;
   private String name;
   private ModifyRecord modifyRecord;
   private SetList<String> taskIds;
   private SetList<String> generatorIds;
   private final Map<String, TypeDescriptor> schema;

   private TaskStore taskStore;

   private Table(String id) {
      this.id = id;
      name = "";
      modifyRecord = ModifyRecord.createdNow();
      taskIds = SetList.empty();
      generatorIds = SetList.empty();
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
      return new DateTime(modifyRecord.getDateCreated());
   }

   public DateTime getDateLastModified() {
      return new DateTime(modifyRecord.getDateLastModified());
   }

   SetList<String> getAllTaskIds() {
      return taskIds;
   }

   Table withTaskIds(Iterable<String> ids) {
      taskIds = taskIds.addAll(ids);
      return this;
   }

   void linkTask(String id) {
      taskIds = taskIds.add(id);
   }

   void unlinkTask(String id) {
      taskIds = taskIds.remove(id);
   }

   public SetList<String> getAllGeneratorIds() {
      return generatorIds;
   }

   void linkGenerator(String id) {
      generatorIds = generatorIds.add(id);
   }

   void unlinkGenerator(String id) {
      generatorIds = generatorIds.remove(id);
   }

   @Override
   public void setTaskStore(TaskStore taskStore) {
      this.taskStore = taskStore;
   }

   @Override
   public TaskStore getTaskStore() {
      return taskStore;
   }

   public PropertyMap getDefaultProperties() {
      Map<String, Property> result = new HashMap<>();
      for (String key : schema.keySet()) {
         result.put(key, schema.get(key).getDefaultValue());
      }
      return PropertyMap.fromJava(result);
   }

   public static Table createTable() {
      return new Table(UUID.randomUUID().toString());
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("id", id);
      json.put("name", name);
      modifyRecord.writeIntoJson(json);
      json.put("tasks", taskIds.asList());
      json.put("generators", generatorIds.asList());

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
      result.modifyRecord = ModifyRecord.readFromJson(json);

      JSONArray tasksJson = json.getJSONArray("tasks");
      for (int i = 0; i < tasksJson.length(); i++) {
         result.taskIds = result.taskIds.add(tasksJson.getString(i));
      }

      JSONArray generatorsJson = json.getJSONArray("generators");
      for (int i = 0; i < generatorsJson.length(); i++) {
         result.generatorIds = result.generatorIds.add(generatorsJson.getString(i));
      }

      JSONObject schemaJson = json.getJSONObject("schema");
      for (String s : schemaJson.keySet()) {
         result.schema.put(s, TypeDescriptor.fromJson(schemaJson.getJSONObject(s)));
      }

      return result;
   }
}
