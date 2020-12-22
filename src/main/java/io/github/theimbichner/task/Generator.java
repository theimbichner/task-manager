package io.github.theimbichner.task;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import io.vavr.Tuple2;
import io.vavr.control.Either;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.theimbichner.task.collection.SetList;
import io.github.theimbichner.task.io.Storable;
import io.github.theimbichner.task.io.TaskAccessException;
import io.github.theimbichner.task.io.TaskStore;
import io.github.theimbichner.task.schema.Property;
import io.github.theimbichner.task.time.DateTime;
import io.github.theimbichner.task.time.DatePattern;
import io.github.theimbichner.task.time.ModifyRecord;

public class Generator implements Storable {
   private final String id;
   private String name;
   private ModifyRecord modifyRecord;
   private String templateName;
   private String templateMarkup;
   private final String templateTableId;
   private final Map<String, Property> templateProperties;
   private long templateDuration;
   private Instant generationLastTimestamp;
   private final String generationField;
   private final DatePattern generationDatePattern;
   private SetList<String> taskIds;

   private TaskStore taskStore;

   private Generator(
      String id,
      String templateTableId,
      String generationField,
      DatePattern generationDatePattern
   ) {
      this.id = id;
      this.templateTableId = templateTableId;
      this.generationField = generationField;
      this.generationDatePattern = generationDatePattern;

      name = "";
      modifyRecord = ModifyRecord.createdNow();
      templateName = "";
      templateMarkup = "";
      templateProperties = new HashMap<>();
      templateDuration = 0;
      generationLastTimestamp = modifyRecord.getDateCreated();
      taskIds = SetList.empty();

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

   public String getTemplateName() {
      return templateName;
   }

   public String getTemplateMarkup() {
      return templateMarkup;
   }

   public String getTemplateTableId() {
      return templateTableId;
   }

   public Set<String> getTemplatePropertyNames() {
      return Set.copyOf(templateProperties.keySet());
   }

   public Property getTemplateProperty(String key) {
      return templateProperties.get(key);
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

   void unlinkTask(String id) {
      taskIds = taskIds.remove(id);
   }

   public Either<TaskAccessException, Generator> modify(GeneratorDelta delta) {
      if (delta.isEmpty()) {
         return Either.right(this);
      }

      for (String key : delta.getProperties().keySet()) {
         if (key.equals(generationField)) {
            throw new IllegalArgumentException("Cannot modify generator generationField");
         }
         Property newProperty = delta.getProperties().get(key);
         if (newProperty == Property.DELETE) {
            templateProperties.remove(key);
         }
         else {
            templateProperties.put(key, newProperty);
         }
      }

      name = delta.getName().orElse(name);
      templateName = delta.getTemplateName().orElse(templateName);
      templateMarkup = delta.getTemplateMarkup().orElse(templateMarkup);
      templateDuration = delta.getTemplateDuration().orElse(templateDuration);

      TaskDelta taskDelta = delta.asTaskDelta();
      return Either
         .sequenceRight(taskIds.asList().stream()
            .map(id -> taskStore
               .getTasks().getById(id)
               .flatMap(t -> t.modify(taskDelta, false)))
            .collect(Collectors.toList()))
         .map(x -> {
            modifyRecord = modifyRecord.updatedNow();
            return this;
         });
   }

   Either<TaskAccessException, Generator> unlinkTasksBefore(String taskId) {
      if (!taskIds.contains(taskId)) {
         throw new IllegalArgumentException("Task not found in generator");
      }

      Either<TaskAccessException, Task> result = Either.right(null);

      Tuple2<List<String>, List<String>> split = taskIds.split(taskId);
      for (String s : split._1) {
         result = result
            .flatMap(x -> taskStore.getTasks().getById(s))
            .peek(task -> task.unlinkGenerator());
      }
      taskIds = SetList.empty();
      taskIds = taskIds.addAll(split._2);

      return result.map(x -> this);
   }

   Either<TaskAccessException, List<String>> generateTasks(Instant timestamp) {
      if (!timestamp.isAfter(generationLastTimestamp)) {
         return Either.right(List.of());
      }
      return Either
         .sequenceRight(generationDatePattern
            .getDates(generationLastTimestamp, timestamp)
            .stream()
            .map(instant -> Task.newSeriesTask(this, instant))
            .map(task -> taskStore.getTasks().save(task))
            .collect(Collectors.toList()))
         .map(tasks -> tasks
            .map(task -> {
               taskIds = taskIds.add(task.getId());
               return task.getId();
            })
            .asJava())
         .peek(x -> generationLastTimestamp = timestamp);
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
      Generator result = new Generator(UUID.randomUUID().toString(), table.getId(), field, pattern);
      result.templateProperties.putAll(table.getDefaultProperties());
      result.setTaskStore(table.getTaskStore());

      return result;
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

      JSONObject propertiesJson = new JSONObject();
      for (String s : templateProperties.keySet()) {
         propertiesJson.put(s, templateProperties.get(s).toJson());
      }
      json.put("templateProperties", propertiesJson);

      return json;
   }

   public static Generator fromJson(JSONObject json) {
      String id = json.getString("id");
      String templateTableId = json.getString("templateTable");
      String generationField = json.getString("generationField");
      JSONObject jsonDatePattern = json.getJSONObject("generationDatePattern");
      DatePattern generationDatePattern = DatePattern.fromJson(jsonDatePattern);

      Generator result = new Generator(id, templateTableId, generationField, generationDatePattern);
      result.name = json.getString("name");
      result.modifyRecord = ModifyRecord.readFromJson(json);
      result.templateName = json.getString("templateName");
      result.templateMarkup = json.getString("templateMarkup");
      result.templateDuration = json.getLong("templateDuration");
      result.generationLastTimestamp = Instant.parse(json.getString("generationLastTimestamp"));

      JSONArray tasksJson = json.getJSONArray("tasks");
      for (int i = 0; i < tasksJson.length(); i++) {
         result.taskIds = result.taskIds.add(tasksJson.getString(i));
      }

      JSONObject propertiesJson = json.getJSONObject("templateProperties");
      for (String s : propertiesJson.keySet()) {
         result.templateProperties.put(s, Property.fromJson(propertiesJson.getJSONObject(s)));
      }

      return result;
   }
}
