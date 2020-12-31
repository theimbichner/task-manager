package io.github.theimbichner.taskmanager.task;

import java.util.Map;
import java.util.UUID;

import io.vavr.Tuple2;
import io.vavr.collection.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.io.Storable;
import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.task.property.TypeDescriptor;
import io.github.theimbichner.taskmanager.time.DateTime;
import io.github.theimbichner.taskmanager.time.ModifyRecord;

/*
 * TODO restrict user property names to avoid collision with system properties.
 * Name
 * Date Created
 * Date Last Modified
 */

public class Table implements Storable {
   private static class Builder {
      private final String id;
      private String name;
      private ModifyRecord modifyRecord;
      private SetList<String> taskIds;
      private SetList<String> generatorIds;
      private HashMap<String, TypeDescriptor> schema;

      private TaskStore taskStore;

      private Builder(String id) {
         this.id = id;
         name = "";
         modifyRecord = ModifyRecord.createdNow();
         taskIds = SetList.empty();
         generatorIds = SetList.empty();
         schema = HashMap.empty();

         taskStore = null;
      }

      private Builder(Table table) {
         id = table.id;
         name = table.name;
         modifyRecord = table.modifyRecord;
         taskIds = table.taskIds;
         generatorIds = table.generatorIds;
         schema = table.schema;

         taskStore = table.taskStore;
      }
   }

   private final String id;
   private final String name;
   private final ModifyRecord modifyRecord;
   private final SetList<String> taskIds;
   private final SetList<String> generatorIds;
   private final HashMap<String, TypeDescriptor> schema;

   private TaskStore taskStore;

   private Table(Builder builder) {
      id = builder.id;
      name = builder.name;
      modifyRecord = builder.modifyRecord;
      taskIds = builder.taskIds;
      generatorIds = builder.generatorIds;
      schema = builder.schema;

      taskStore = builder.taskStore;
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

   Table withTasks(Iterable<String> ids) {
      Builder result = new Builder(this);
      result.taskIds = taskIds.addAll(ids);
      return new Table(result);
   }

   Table withoutTask(String id) {
      Builder result = new Builder(this);
      result.taskIds = taskIds.remove(id);
      return new Table(result);
   }

   public SetList<String> getAllGeneratorIds() {
      return generatorIds;
   }

   Table withGenerator(String id) {
      Builder result = new Builder(this);
      result.generatorIds = generatorIds.add(id);
      return new Table(result);
   }

   Table withoutGenerator(String id) {
      Builder result = new Builder(this);
      result.generatorIds = generatorIds.remove(id);
      return new Table(result);
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
      Map<String, Property> result = new java.util.HashMap<>();
      for (Tuple2<String, TypeDescriptor> entry : schema) {
         result.put(entry._1, entry._2.getDefaultValue());
      }
      return PropertyMap.fromJava(result);
   }

   public static Table createTable() {
      return new Table(new Builder(UUID.randomUUID().toString()));
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("id", id);
      json.put("name", name);
      modifyRecord.writeIntoJson(json);
      json.put("tasks", taskIds.asList());
      json.put("generators", generatorIds.asList());

      JSONObject schemaJson = new JSONObject();
      for (Tuple2<String, TypeDescriptor> entry : schema) {
         schemaJson.put(entry._1, entry._2.toJson());
      }
      json.put("schema", schemaJson);

      return json;
   }

   public static Table fromJson(JSONObject json) {
      String id = json.getString("id");
      Builder result = new Builder(id);

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

      return new Table(result);
   }
}
