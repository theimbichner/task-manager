package io.github.theimbichner.taskmanager.task;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.vavr.Tuple;
import io.vavr.Tuple2;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.io.Storable;
import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.time.DateTime;
import io.github.theimbichner.taskmanager.time.DatePattern;
import io.github.theimbichner.taskmanager.time.ModifyRecord;

public class Generator implements Storable {
   private static class Builder {
      private final String id;
      private String name;
      private ModifyRecord modifyRecord;
      private String templateName;
      private String templateMarkup;
      private final String templateTableId;
      private PropertyMap templateProperties;
      private long templateDuration;
      private Instant generationLastTimestamp;
      private final String generationField;
      private final DatePattern generationDatePattern;
      private SetList<String> taskIds;

      private TaskStore taskStore;

      private Builder(
         String id,
         String templateTableId,
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

         taskStore = null;
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

         taskStore = g.taskStore;
      }
   }

   private final String id;
   private final String name;
   private final ModifyRecord modifyRecord;
   private final String templateName;
   private final String templateMarkup;
   private final String templateTableId;
   private final PropertyMap templateProperties;
   private final long templateDuration;
   private final Instant generationLastTimestamp;
   private final String generationField;
   private final DatePattern generationDatePattern;
   private final SetList<String> taskIds;

   private TaskStore taskStore;

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

   public String getTemplateName() {
      return templateName;
   }

   public String getTemplateMarkup() {
      return templateMarkup;
   }

   public String getTemplateTableId() {
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

   SetList<String> getTaskIds() {
      return taskIds;
   }

   Generator withoutTask(String id) {
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

   // TODO update modification timestamp here?
   Tuple2<Generator, List<String>> withoutTasksBefore(String taskId) {
      if (!taskIds.contains(taskId)) {
         throw new IllegalArgumentException("Task not found in generator");
      }

      Tuple2<List<String>, List<String>> split = taskIds.split(taskId);
      Builder result = new Builder(this);
      result.taskIds = SetList.<String>empty().addAll(split._2);

      return Tuple.of(new Generator(result), split._1);
   }

   // TODO update the modification timestamp?
   Tuple2<Generator, List<Task>> withTasksUntil(Instant timestamp) {
      if (!timestamp.isAfter(generationLastTimestamp)) {
         return Tuple.of(this, List.of());
      }

      List<Task> tasks = generationDatePattern
         .getDates(generationLastTimestamp, timestamp)
         .stream()
         .map(instant -> Task.newSeriesTask(this, instant))
         .collect(Collectors.toList());
      List<String> ids = tasks.stream().map(Task::getId).collect(Collectors.toList());

      Builder result = new Builder(this);
      result.generationLastTimestamp = timestamp;
      result.taskIds = result.taskIds.addAll(ids);

      return Tuple.of(new Generator(result), tasks);
   }

   @Override
   public void setTaskStore(TaskStore taskStore) {
      this.taskStore = taskStore;
   }

   @Override
   public TaskStore getTaskStore() {
      return taskStore;
   }

   public static Generator createGenerator(Table table, String field, DatePattern pattern) {
      Builder result = new Builder(UUID.randomUUID().toString(), table.getId(), field, pattern);
      result.templateProperties = table.getSchema().getDefaultProperties();
      result.taskStore = table.getTaskStore();

      return new Generator(result);
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("id", id);
      json.put("name", name);
      modifyRecord.writeIntoJson(json);
      json.put("templateName", templateName);
      json.put("templateMarkup", templateMarkup);
      json.put("templateTable", templateTableId);
      json.put("templateDuration", templateDuration);
      json.put("generationLastTimestamp", generationLastTimestamp.toString());
      json.put("generationField", generationField);
      json.put("generationDatePattern", generationDatePattern.toJson());
      json.put("tasks", taskIds.asList());
      json.put("templateProperties", templateProperties.toJson());

      return json;
   }

   public static Generator fromJson(JSONObject json) {
      String id = json.getString("id");
      String templateTableId = json.getString("templateTable");
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
         result.taskIds = result.taskIds.add(tasksJson.getString(i));
      }

      return new Generator(result);
   }
}
