package io.github.theimbichner.task;

import java.time.Instant;
import java.util.UUID;

import io.vavr.control.Either;
import io.vavr.control.Option;

import org.json.JSONObject;

import io.github.theimbichner.task.io.Storable;
import io.github.theimbichner.task.io.TaskAccessException;
import io.github.theimbichner.task.io.TaskStore;
import io.github.theimbichner.task.schema.Property;
import io.github.theimbichner.task.schema.PropertyMap;
import io.github.theimbichner.task.time.DateTime;
import io.github.theimbichner.task.time.ModifyRecord;

public class Task implements Storable {
   private final String id;
   private final String tableId;
   private String name;
   private ModifyRecord modifyRecord;
   private String markup;
   private String generatorId;
   private PropertyMap properties;

   private TaskStore taskStore;

   private Task(String id, String tableId) {
      this.id = id;
      this.tableId = tableId;

      name = "";
      modifyRecord = ModifyRecord.createdNow();
      markup = "";
      properties = PropertyMap.empty();
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
      return new DateTime(modifyRecord.getDateCreated());
   }

   public DateTime getDateLastModified() {
      return new DateTime(modifyRecord.getDateLastModified());
   }

   public String getMarkup() {
      return markup;
   }

   public String getGeneratorId() {
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

   public Either<TaskAccessException, Task> modify(TaskDelta delta) {
      return modify(delta, true);
   }

   Either<TaskAccessException, Task> modify(TaskDelta delta, boolean shouldUnlinkGenerator) {
      if (delta.isEmpty()) {
         return Either.right(this);
      }

      return getGenerator()
         .flatMap(generator -> {
            if (shouldUnlinkGenerator && generator.isDefined()) {
               return taskStore.getGenerators()
                  .save(generator.get().withoutTask(id))
                  .map(x -> {
                     generatorId = null;
                     return Option.<Generator>none();
                  });
            }
            return Either.right(generator);
         })
         .map(generator -> {
            properties = properties.merge(delta.getProperties());
            name = delta.getName().orElse(name);
            markup = delta.getMarkup().orElse(markup);

            if (delta.getDuration().isPresent()) {
               if (generator.isEmpty()) {
                  throw new IllegalArgumentException("Cannot set duration without generator");
               }
               String generationField = generator.get().getGenerationField();
               DateTime date = (DateTime) properties.asMap().get(generationField).get().get();
               DateTime newDate = date.withDuration(delta.getDuration().get());
               properties = properties.put(generationField, Property.of(newDate));
            }

            modifyRecord = modifyRecord.updatedNow();
            return this;
         });
   }

   void unlinkGenerator() {
      generatorId = null;
   }

   @Override
   public void setTaskStore(TaskStore taskStore) {
      this.taskStore = taskStore;
   }

   @Override
   public TaskStore getTaskStore() {
      return taskStore;
   }

   public static Task createTask(Table table) {
      Task result = new Task(UUID.randomUUID().toString(), table.getId());
      result.properties = table.getDefaultProperties();
      table.linkTask(result.id);
      result.setTaskStore(table.getTaskStore());
      return result;
   }

   static Task newSeriesTask(Generator generator, Instant startTime) {
      Task result = new Task(UUID.randomUUID().toString(), generator.getTemplateTableId());
      result.name = generator.getTemplateName();
      result.markup = generator.getTemplateMarkup();
      result.generatorId = generator.getId();
      DateTime date = new DateTime(startTime)
         .withDuration(generator.getTemplateDuration());
      result.properties = generator
         .getTemplateProperties()
         .put(generator.getGenerationField(), Property.of(date));
      result.setTaskStore(generator.getTaskStore());
      return result;
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("id", id);
      json.put("table", tableId);
      json.put("name", name);
      modifyRecord.writeIntoJson(json);
      json.put("markup", markup);
      json.put("generator", generatorId == null ? JSONObject.NULL : generatorId);
      json.put("properties", properties.toJson());

      return json;
   }

   public static Task fromJson(JSONObject json) {
      String id = json.getString("id");
      String tableId = json.getString("table");
      Task result = new Task(id, tableId);

      result.name = json.getString("name");
      result.modifyRecord = ModifyRecord.readFromJson(json);
      result.markup = json.getString("markup");
      result.generatorId = json.isNull("generator") ? null : json.getString("generator");
      result.properties = PropertyMap.fromJson(json.getJSONObject("properties"));

      return result;
   }
}
