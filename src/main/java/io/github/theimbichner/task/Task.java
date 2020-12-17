package io.github.theimbichner.task;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.json.JSONObject;

import io.github.theimbichner.task.io.Storable;
import io.github.theimbichner.task.io.TaskAccessException;
import io.github.theimbichner.task.io.TaskStore;
import io.github.theimbichner.task.schema.Property;
import io.github.theimbichner.task.time.DateTime;
import io.github.theimbichner.task.time.ModifyRecord;

public class Task implements Storable {
   private final String id;
   private final String tableId;
   private String name;
   private ModifyRecord modifyRecord;
   private String markup;
   private String generatorId;
   private final Map<String, Property> properties;

   private TaskStore taskStore;

   private Task(String id, String tableId) {
      this.id = id;
      this.tableId = tableId;

      name = "";
      modifyRecord = ModifyRecord.createdNow();
      markup = "";
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

   public Set<String> getPropertyNames() {
      return Set.copyOf(properties.keySet());
   }

   public Property getProperty(String key) {
      return properties.get(key);
   }

   public void modify(TaskDelta delta) throws TaskAccessException {
      modify(delta, true);
   }

   void modify(TaskDelta delta, boolean shouldUnlinkGenerator) throws TaskAccessException {
      if (delta.isEmpty()) {
         return;
      }

      Generator generator = null;
      if (generatorId != null) {
         generator = taskStore.getGenerators().getById(generatorId);
      }

      for (String key : delta.getProperties().keySet()) {
         Property newProperty = delta.getProperties().get(key);
         if (newProperty == Property.DELETE) {
            properties.remove(key);
         }
         else {
            properties.put(key, newProperty);
         }
      }

      name = delta.getName().orElse(name);
      markup = delta.getMarkup().orElse(markup);
      if (delta.getDuration().isPresent()) {
         if (generator == null || shouldUnlinkGenerator) {
            throw new IllegalArgumentException("Cannot set duration without generator");
         }
         String generationField = generator.getGenerationField();
         DateTime date = (DateTime) properties.get(generationField).get();
         long duration = delta.getDuration().get();
         properties.put(generationField, Property.of(date.withDuration(duration)));
      }

      if (shouldUnlinkGenerator && generator != null) {
         generator.unlinkTask(id);
         generatorId = null;
      }
      modifyRecord = modifyRecord.updatedNow();
   }

   void unlinkGenerator() {
      generatorId = null;
   }

   /*
    * TODO Should the call to unlinkGenerator on earlier tasks in the series
    * cause the modification timestamp to update?
    */
   public void modifySeries(GeneratorDelta delta) throws TaskAccessException {
      if (generatorId == null) {
         throw new IllegalStateException("Cannot modify series on non series task");
      }

      Generator generator = taskStore.getGenerators().getById(generatorId);
      generator.unlinkTasksBefore(id);
      generator.modify(delta);
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
      table.linkTask(result.id);
      result.registerTaskStore(table.getTaskStore());
      return result;
   }

   static Task newSeriesTask(Generator generator, Instant startTime) {
      Task result = new Task(UUID.randomUUID().toString(), generator.getTemplateTableId());
      result.name = generator.getTemplateName();
      result.markup = generator.getTemplateMarkup();
      result.generatorId = generator.getId();
      for (String s : generator.getTemplatePropertyNames()) {
         result.properties.put(s, generator.getTemplateProperty(s));
      }
      DateTime date = new DateTime(startTime).withDuration(generator.getTemplateDuration());
      result.properties.put(generator.getGenerationField(), Property.of(date));
      result.registerTaskStore(generator.getTaskStore());
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
      result.modifyRecord = ModifyRecord.readFromJson(json);
      result.markup = json.getString("markup");
      result.generatorId = json.isNull("generator") ? null : json.getString("generator");

      JSONObject propertiesJson = json.getJSONObject("properties");
      for (String s : propertiesJson.keySet()) {
         result.properties.put(s, Property.fromJson(propertiesJson.getJSONObject(s)));
      }

      return result;
   }
}
