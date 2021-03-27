package io.github.theimbichner.taskmanager.task;

import java.time.Instant;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Vector;
import io.vavr.control.Option;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.io.Storable;
import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.task.property.Schema;
import io.github.theimbichner.taskmanager.time.DateTime;
import io.github.theimbichner.taskmanager.time.DatePattern;
import io.github.theimbichner.taskmanager.time.ModifyRecord;

public class Generator implements Storable<ItemId<Generator>> {
   private static class Builder {
      private final ItemId<Generator> id;
      private String name;
      private ModifyRecord modifyRecord;
      private String templateName;
      private String templateMarkup;
      private final ItemId<Table> templateTableId;
      private PropertyMap templateProperties;
      private long templateDuration;
      private Instant generationLastTimestamp;
      private String generationField;
      private final DatePattern generationDatePattern;
      private SetList<ItemId<Task>> taskIds;

      private Builder(
         ItemId<Generator> id,
         ItemId<Table> templateTableId,
         String generationField,
         DatePattern generationDatePattern
      ) {
         this.id = id;
         name = "";
         modifyRecord = ModifyRecord.createdNow();
         templateName = "";
         templateMarkup = "";
         this.templateTableId = templateTableId;
         templateProperties = PropertyMap.empty();
         templateDuration = 0;
         generationLastTimestamp = modifyRecord.getDateCreated();
         this.generationField = generationField;
         this.generationDatePattern = generationDatePattern;
         taskIds = SetList.empty();
      }

      private Builder(Generator g) {
         id = g.id;
         name = g.name;
         modifyRecord = g.modifyRecord;
         templateName = g.templateName;
         templateMarkup = g.templateMarkup;
         templateTableId = g.templateTableId;
         templateProperties = g.templateProperties;
         templateDuration = g.templateDuration;
         generationLastTimestamp = g.generationLastTimestamp;
         generationField = g.generationField;
         generationDatePattern = g.generationDatePattern;
         taskIds = g.taskIds;
      }
   }

   private final ItemId<Generator> id;
   private final String name;
   private final ModifyRecord modifyRecord;
   private final String templateName;
   private final String templateMarkup;
   private final ItemId<Table> templateTableId;
   private final PropertyMap templateProperties;
   private final long templateDuration;
   private final Instant generationLastTimestamp;
   private final String generationField;
   private final DatePattern generationDatePattern;
   private final SetList<ItemId<Task>> taskIds;

   private Generator(Builder builder) {
      id = builder.id;
      name = builder.name;
      modifyRecord = builder.modifyRecord;
      templateName = builder.templateName;
      templateMarkup = builder.templateMarkup;
      templateTableId = builder.templateTableId;
      templateProperties = builder.templateProperties;
      templateDuration = builder.templateDuration;
      generationLastTimestamp = builder.generationLastTimestamp;
      generationField = builder.generationField;
      generationDatePattern = builder.generationDatePattern;
      taskIds = builder.taskIds;
   }

   @Override
   public ItemId<Generator> getId() {
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

   public String getTemplateName() {
      return templateName;
   }

   public String getTemplateMarkup() {
      return templateMarkup;
   }

   public ItemId<Table> getTemplateTableId() {
      return templateTableId;
   }

   public PropertyMap getTemplateProperties() {
      return templateProperties;
   }

   public long getTemplateDuration() {
      return templateDuration;
   }

   public String getGenerationField() {
      return generationField;
   }

   public DatePattern getGenerationDatePattern() {
      return generationDatePattern;
   }

   SetList<ItemId<Task>> getTaskIds() {
      return taskIds;
   }

   Generator withoutTask(ItemId<Task> id) {
      Builder result = new Builder(this);
      result.taskIds = taskIds.remove(id);
      return new Generator(result);
   }

   Generator withModification(GeneratorDelta delta) {
      if (delta.isEmpty()) {
         return this;
      }

      if (delta.getProperties().asMap().containsKey(generationField)) {
         throw new IllegalArgumentException("Cannot modify generator generationField");
      }

      Builder result = new Builder(this);
      result.templateProperties = templateProperties.merge(delta.getProperties());
      result.name = delta.getName().orElse(name);
      result.templateName = delta.getTemplateName().orElse(templateName);
      result.templateMarkup = delta.getTemplateMarkup().orElse(templateMarkup);
      result.templateDuration = delta.getTemplateDuration().orElse(templateDuration);
      result.modifyRecord = modifyRecord.updatedNow();

      return new Generator(result);
   }

   Generator adjustToSchema(Schema schemaDelta) {
      if (schemaDelta.isEmpty()) {
         return this;
      }

      Builder result = new Builder(this);

      Option<String> newName = schemaDelta.findNewNameOf(generationField);
      PropertyMap propertyDelta = schemaDelta.asPropertiesDelta(templateProperties);

      if (newName.isEmpty() && propertyDelta.asMap().containsKey(generationField)) {
         throw new IllegalArgumentException("Schema change would wipe out generation field");
      }

      result.generationField = newName.getOrElse(generationField);
      result.templateProperties = templateProperties.merge(propertyDelta);
      result.modifyRecord = modifyRecord.updatedNow();

      return new Generator(result);
   }

   // TODO update modification timestamp here?
   Tuple2<Generator, Vector<ItemId<Task>>> withoutTasksBefore(ItemId<Task> taskId) {
      if (!taskIds.contains(taskId)) {
         throw new IllegalArgumentException("Task not found in generator");
      }

      Tuple2<Vector<ItemId<Task>>, Vector<ItemId<Task>>> split = taskIds.split(taskId);
      Builder result = new Builder(this);
      result.taskIds = SetList.<ItemId<Task>>empty().addAll(split._2);

      return Tuple.of(new Generator(result), split._1);
   }

   // TODO update the modification timestamp?
   Tuple2<Generator, Vector<Task>> withTasksUntil(Instant timestamp) {
      if (!timestamp.isAfter(generationLastTimestamp)) {
         return Tuple.of(this, Vector.empty());
      }

      Vector<Task> tasks = generationDatePattern
         .getDates(generationLastTimestamp, timestamp)
         .map(instant -> Task.newSeriesTask(this, instant));
      Vector<ItemId<Task>> ids = tasks.map(Task::getId);

      Builder result = new Builder(this);
      result.generationLastTimestamp = timestamp;
      result.taskIds = result.taskIds.addAll(ids);

      return Tuple.of(new Generator(result), tasks);
   }

   static Generator newGenerator(Table table, String field, DatePattern pattern) {
      Builder result = new Builder(ItemId.randomId(), table.getId(), field, pattern);
      result.templateProperties = table.getSchema().getDefaultProperties();
      result.templateProperties = result.templateProperties.put(field, Property.empty());

      return new Generator(result);
   }

   public JSONObject toJson() {
      Vector<String> stringTaskIds = taskIds.asList().map(ItemId::toString);

      JSONObject json = new JSONObject();
      json.put("id", id.toString());
      json.put("name", name);
      modifyRecord.writeIntoJson(json);
      json.put("templateName", templateName);
      json.put("templateMarkup", templateMarkup);
      json.put("templateTable", templateTableId.toString());
      json.put("templateDuration", templateDuration);
      json.put("generationLastTimestamp", generationLastTimestamp.toString());
      json.put("generationField", generationField);
      json.put("generationDatePattern", generationDatePattern.toJson());
      json.put("tasks", stringTaskIds.asJava());
      json.put("templateProperties", templateProperties.toJson());

      return json;
   }

   public static Generator fromJson(JSONObject json) {
      ItemId<Generator> id = ItemId.of(json.getString("id"));
      ItemId<Table> templateTableId = ItemId.of(json.getString("templateTable"));
      String generationField = json.getString("generationField");
      JSONObject jsonDatePattern = json.getJSONObject("generationDatePattern");
      DatePattern generationDatePattern = DatePattern.fromJson(jsonDatePattern);

      Builder result = new Builder(id, templateTableId, generationField, generationDatePattern);
      result.name = json.getString("name");
      result.modifyRecord = ModifyRecord.readFromJson(json);
      result.templateName = json.getString("templateName");
      result.templateMarkup = json.getString("templateMarkup");
      result.templateDuration = json.getLong("templateDuration");
      result.generationLastTimestamp = Instant.parse(json.getString("generationLastTimestamp"));
      result.templateProperties = PropertyMap.fromJson(json.getJSONObject("templateProperties"));

      JSONArray tasksJson = json.getJSONArray("tasks");
      for (int i = 0; i < tasksJson.length(); i++) {
         ItemId<Task> taskId = ItemId.of(tasksJson.getString(i));
         result.taskIds = result.taskIds.add(taskId);
      }

      return new Generator(result);
   }

   public static TypeAdapter<Generator, JSONObject> jsonAdapter() {
      return new TypeAdapter<>(Generator::toJson, Generator::fromJson);
   }
}
