package io.github.theimbichner.taskmanager.task;

import java.time.Instant;

import io.vavr.control.Either;
import io.vavr.control.Option;

import org.json.JSONObject;

import io.github.theimbichner.taskmanager.io.Storable;
import io.github.theimbichner.taskmanager.io.TaskAccessException;
import io.github.theimbichner.taskmanager.io.TaskStore;
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

      private TaskStore taskStore;

      private Builder(ItemId<Task> id, ItemId<Table> tableId) {
         this.id = id;
         this.tableId = tableId;
         name = "";
         modifyRecord = ModifyRecord.createdNow();
         markup = "";
         generatorId = null;
         properties = PropertyMap.empty();

         taskStore = null;
      }

      private Builder(Task task) {
         id = task.id;
         tableId = task.tableId;
         name = task.name;
         modifyRecord = task.modifyRecord;
         markup = task.markup;
         generatorId = task.generatorId;
         properties = task.properties;

         taskStore = task.taskStore;
      }
   }

   private final ItemId<Task> id;
   private final ItemId<Table> tableId;
   private final String name;
   private final ModifyRecord modifyRecord;
   private final String markup;
   private final ItemId<Generator> generatorId;
   private final PropertyMap properties;

   private TaskStore taskStore;

   private Task(Builder builder) {
      id = builder.id;
      tableId = builder.tableId;
      name = builder.name;
      modifyRecord = builder.modifyRecord;
      markup = builder.markup;
      generatorId = builder.generatorId;
      properties = builder.properties;

      taskStore = builder.taskStore;
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

   Either<TaskAccessException, Option<Generator>> getGenerator() {
      if (generatorId == null) {
         return Either.right(Option.none());
      }
      return taskStore.getGenerators().getById(generatorId).map(Option::some);
   }

   Either<TaskAccessException, Task> withModification(TaskDelta delta) {
      if (delta.isEmpty()) {
         return Either.right(this);
      }

      return getGenerator()
         .map(generator -> {
            Builder result = new Builder(this);
            result.properties = properties.merge(delta.getProperties());
            result.name = delta.getName().orElse(name);
            result.markup = delta.getMarkup().orElse(markup);

            if (delta.getDuration().isPresent()) {
               if (generator.isEmpty()) {
                  throw new IllegalArgumentException("Cannot set duration without generator");
               }
               String generationField = generator.get().getGenerationField();
               DateTime date = (DateTime) properties.asMap().get(generationField).get().get();
               DateTime newDate = date.withDuration(delta.getDuration().get());
               result.properties = result.properties.put(generationField, Property.of(newDate));
            }

            result.modifyRecord = modifyRecord.updatedNow();
            return new Task(result);
         });
   }

   Task withoutGenerator() {
      Builder result = new Builder(this);
      result.generatorId = null;
      return new Task(result);
   }

   @Override
   public void setTaskStore(TaskStore taskStore) {
      this.taskStore = taskStore;
   }

   @Override
   public TaskStore getTaskStore() {
      return taskStore;
   }

   static Task newTask(Table table) {
      Builder result = new Builder(ItemId.randomId(), table.getId());
      result.properties = table.getSchema().getDefaultProperties();
      result.taskStore = table.getTaskStore();

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
      result.taskStore = generator.getTaskStore();
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
}
