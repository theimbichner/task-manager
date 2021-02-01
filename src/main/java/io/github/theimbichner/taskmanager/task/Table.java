package io.github.theimbichner.taskmanager.task;

import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.io.Storable;
import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.task.property.Schema;
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
      private Schema schema;

      private TaskStore taskStore;

      private Builder(String id) {
         this.id = id;
         name = "";
         modifyRecord = ModifyRecord.createdNow();
         taskIds = SetList.empty();
         generatorIds = SetList.empty();
         schema = Schema.empty();

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
   private final Schema schema;

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

   public Table withModification(TableDelta delta) {
      if (delta.isEmpty()) {
         return this;
      }

      Builder result = new Builder(this);
      result.name = delta.getName().orElse(name);
      result.schema = schema.merge(delta.getSchema());
      result.modifyRecord = modifyRecord.updatedNow();

      return new Table(result);
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

   public Schema getSchema() {
      return schema;
   }

   @Override
   public void setTaskStore(TaskStore taskStore) {
      this.taskStore = taskStore;
   }

   @Override
   public TaskStore getTaskStore() {
      return taskStore;
   }

   static Table newTable() {
      return new Table(new Builder(UUID.randomUUID().toString()));
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("id", id);
      json.put("name", name);
      modifyRecord.writeIntoJson(json);
      json.put("tasks", taskIds.asList());
      json.put("generators", generatorIds.asList());
      json.put("schema", schema.toJson());

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

      result.schema = Schema.fromJson(json.getJSONObject("schema"));

      return new Table(result);
   }
}
