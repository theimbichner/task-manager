package io.github.theimbichner.taskmanager.task;

import java.time.Instant;

import org.json.JSONObject;

import io.github.theimbichner.taskmanager.io.datastore.Storable;
import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.time.DateTime;
import io.github.theimbichner.taskmanager.time.ModifyRecord;

public class Task implements Storable<ItemId<Task>> {
   private static class Builder {
      private final ItemId<Task> id;
      private final ItemId<Table> tableId;
      private String name;
      private ModifyRecord modifyRecord;
      private String markup;
      private ItemId<Generator> generatorId;
      private PropertyMap properties;

      private Builder(ItemId<Task> id, ItemId<Table> tableId) {
         this.id = id;
         this.tableId = tableId;
         name = "";
         modifyRecord = ModifyRecord.createdNow();
         markup = "";
         generatorId = null;
         properties = PropertyMap.empty();
      }

      private Builder(Task task) {
         id = task.id;
         tableId = task.tableId;
         name = task.name;
         modifyRecord = task.modifyRecord;
         markup = task.markup;
         generatorId = task.generatorId;
         properties = task.properties;
      }
   }

   private final ItemId<Task> id;
   private final ItemId<Table> tableId;
   private final String name;
   private final ModifyRecord modifyRecord;
   private final String markup;
   private final ItemId<Generator> generatorId;
   private final PropertyMap properties;

   private Task(Builder builder) {
      id = builder.id;
      tableId = builder.tableId;
      name = builder.name;
      modifyRecord = builder.modifyRecord;
      markup = builder.markup;
      generatorId = builder.generatorId;
      properties = builder.properties;
   }

   @Override
   public ItemId<Task> getId() {
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

   public String getMarkup() {
      return markup;
   }

   public ItemId<Generator> getGeneratorId() {
      return generatorId;
   }

   public PropertyMap getProperties() {
      return properties;
   }

   Task withModification(TaskDelta delta) {
      if (delta.isEmpty()) {
         return this;
      }

      Builder result = new Builder(this);
      result.properties = properties.merge(delta.getProperties());
      result.name = delta.getName().getOrElse(name);
      result.markup = delta.getMarkup().getOrElse(markup);

      result.modifyRecord = modifyRecord.updatedNow();
      return new Task(result);
   }

   Task withSeriesModification(GeneratorDelta delta, Generator generator) {
      if (!generator.getId().equals(generatorId)) {
         String message = "The provided generator must be the parent of this task";
         throw new IllegalArgumentException(message);
      }

      String generationField = generator.getGenerationField();
      TaskDelta taskDelta = delta.asTaskDelta(generationField, properties);
      return withModification(taskDelta);
   }

   Task withoutGenerator() {
      Builder result = new Builder(this);
      result.generatorId = null;
      return new Task(result);
   }

   static Task newTask(Table table) {
      Builder result = new Builder(ItemId.randomId(), table.getId());
      result.properties = table.getSchema().getDefaultProperties();

      return new Task(result);
   }

   static Task newSeriesTask(Generator generator, Instant startTime) {
      Builder result = new Builder(ItemId.randomId(), generator.getTemplateTableId());
      result.name = generator.getTemplateName();
      result.markup = generator.getTemplateMarkup();
      result.generatorId = generator.getId();
      DateTime date = new DateTime(startTime)
         .withDuration(generator.getTemplateDuration());
      result.properties = generator
         .getTemplateProperties()
         .put(generator.getGenerationField(), Property.of(date));
      return new Task(result);
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("id", id.toString());
      json.put("table", tableId.toString());
      json.put("name", name);
      modifyRecord.writeIntoJson(json);
      json.put("markup", markup);
      if (generatorId == null) {
         json.put("generator", JSONObject.NULL);
      }
      else {
         json.put("generator", generatorId.toString());
      }
      json.put("properties", properties.toJson());

      return json;
   }

   public static Task fromJson(JSONObject json) {
      ItemId<Task> id = ItemId.of(json.getString("id"));
      ItemId<Table> tableId = ItemId.of(json.getString("table"));
      Builder result = new Builder(id, tableId);

      result.name = json.getString("name");
      result.modifyRecord = ModifyRecord.readFromJson(json);
      result.markup = json.getString("markup");
      result.generatorId = json.isNull("generator") ? null : ItemId.of(json.getString("generator"));
      result.properties = PropertyMap.fromJson(json.getJSONObject("properties"));

      return new Task(result);
   }

   public static TypeAdapter<Task, JSONObject> jsonAdapter() {
      return new TypeAdapter<>(Task::toJson, Task::fromJson);
   }
}
