package io.github.theimbichner.taskmanager.task;

import java.time.Instant;

import io.vavr.collection.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.io.datastore.Storable;
import io.github.theimbichner.taskmanager.task.property.Schema;
import io.github.theimbichner.taskmanager.time.ModifyRecord;

/*
 * TODO restrict user property names to avoid collision with system properties.
 * Name
 * Date Created
 * Date Last Modified
 */

public class Table implements Storable<ItemId<Table>> {
   private static class Builder {
      private final ItemId<Table> id;
      private String name;
      private ModifyRecord modifyRecord;
      private SetList<ItemId<Task>> taskIds;
      private SetList<ItemId<Generator>> generatorIds;
      private Schema schema;

      private Builder(ItemId<Table> id) {
         this.id = id;
         name = "";
         modifyRecord = ModifyRecord.createdNow();
         taskIds = SetList.empty();
         generatorIds = SetList.empty();
         schema = Schema.empty();
      }

      private Builder(Table table) {
         id = table.id;
         name = table.name;
         modifyRecord = table.modifyRecord;
         taskIds = table.taskIds;
         generatorIds = table.generatorIds;
         schema = table.schema;
      }
   }

   private final ItemId<Table> id;
   private final String name;
   private final ModifyRecord modifyRecord;
   private final SetList<ItemId<Task>> taskIds;
   private final SetList<ItemId<Generator>> generatorIds;
   private final Schema schema;

   private Table(Builder builder) {
      id = builder.id;
      name = builder.name;
      modifyRecord = builder.modifyRecord;
      taskIds = builder.taskIds;
      generatorIds = builder.generatorIds;
      schema = builder.schema;
   }

   @Override
   public ItemId<Table> getId() {
      return id;
   }

   public String getName() {
      return name;
   }

   public Instant getDateCreated() {
      return modifyRecord.getDateCreated();
   }

   public Instant getDateLastModified() {
      return modifyRecord.getDateLastModified();
   }

   public Table withModification(TableDelta delta) {
      if (delta.isEmpty()) {
         return this;
      }

      Builder result = new Builder(this);
      result.name = delta.getName().getOrElse(name);
      result.schema = schema.merge(delta.getSchema());
      result.modifyRecord = modifyRecord.updatedNow();

      return new Table(result);
   }

   SetList<ItemId<Task>> getTaskIds() {
      return taskIds;
   }

   Table withTasks(Iterable<ItemId<Task>> ids) {
      Builder result = new Builder(this);
      result.taskIds = taskIds.addAll(ids);
      return new Table(result);
   }

   Table withoutTask(ItemId<Task> id) {
      Builder result = new Builder(this);
      result.taskIds = taskIds.remove(id);
      return new Table(result);
   }

   public SetList<ItemId<Generator>> getGeneratorIds() {
      return generatorIds;
   }

   Table withGenerator(ItemId<Generator> id) {
      Builder result = new Builder(this);
      result.generatorIds = generatorIds.add(id);
      return new Table(result);
   }

   Table withoutGenerator(ItemId<Generator> id) {
      Builder result = new Builder(this);
      result.generatorIds = generatorIds.remove(id);
      return new Table(result);
   }

   public Schema getSchema() {
      return schema;
   }

   static Table newTable() {
      return new Table(new Builder(ItemId.randomId()));
   }

   public JSONObject toJson() {
      Vector<String> stringTaskIds = taskIds
         .asList()
         .map(ItemId::toString);
      Vector<String> stringGeneratorIds = generatorIds
         .asList()
         .map(ItemId::toString);

      JSONObject json = new JSONObject();
      json.put("id", id.toString());
      json.put("name", name);
      modifyRecord.writeIntoJson(json);
      json.put("tasks", stringTaskIds.asJava());
      json.put("generators", stringGeneratorIds.asJava());
      json.put("schema", schema.toJson());

      return json;
   }

   public static Table fromJson(JSONObject json) {
      ItemId<Table> id = ItemId.of(json.getString("id"));
      Builder result = new Builder(id);

      result.name = json.getString("name");
      result.modifyRecord = ModifyRecord.readFromJson(json);

      JSONArray tasksJson = json.getJSONArray("tasks");
      for (int i = 0; i < tasksJson.length(); i++) {
         ItemId<Task> taskId = ItemId.of(tasksJson.getString(i));
         result.taskIds = result.taskIds.add(taskId);
      }

      JSONArray generatorsJson = json.getJSONArray("generators");
      for (int i = 0; i < generatorsJson.length(); i++) {
         ItemId<Generator> generatorId = ItemId.of(generatorsJson.getString(i));
         result.generatorIds = result.generatorIds.add(generatorId);
      }

      result.schema = Schema.fromJson(json.getJSONObject("schema"));

      return new Table(result);
   }

   public static TypeAdapter<Table, JSONObject> jsonAdapter() {
      return new TypeAdapter<>(Table::toJson, Table::fromJson);
   }
}
