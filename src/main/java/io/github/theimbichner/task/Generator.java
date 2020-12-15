package io.github.theimbichner.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.theimbichner.task.io.Storable;
import io.github.theimbichner.task.io.TaskAccessException;
import io.github.theimbichner.task.io.TaskStore;
import io.github.theimbichner.task.schema.Property;
import io.github.theimbichner.task.time.DateTime;
import io.github.theimbichner.task.time.DatePattern;

public class Generator implements Storable {
   private final String id;
   private String name;
   private DateTime dateCreated;
   private DateTime dateLastModified;
   private String templateName;
   private Optional<String> templateMarkup;
   private final String templateTableId;
   private final Map<String, Property> templateProperties;
   private long templateDuration;
   private Instant generationLastTimestamp;
   private final String generationField;
   private final DatePattern generationDatePattern;
   private final LinkedHashSet<String> taskIds;

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
      dateCreated = new DateTime();
      dateLastModified = dateCreated;
      templateName = "";
      templateMarkup = Optional.empty();
      templateProperties = new HashMap<>();
      templateDuration = 0;
      generationLastTimestamp = dateCreated.getStart();
      taskIds = new LinkedHashSet<>();

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
      return dateCreated;
   }

   public DateTime getDateLastModified() {
      return dateLastModified;
   }

   public String getTemplateName() {
      return templateName;
   }

   public String getTemplateMarkup() {
      return templateMarkup.orElse(null);
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
      taskIds.remove(id);
   }

   public void modify(GeneratorDelta delta) throws TaskAccessException {
      if (delta.isEmpty()) {
         return;
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
      for (String id : taskIds) {
         Task task = taskStore.getTasks().getById(id);
         task.modify(taskDelta, false);
      }

      dateLastModified = new DateTime();
   }

   void unlinkTasksBefore(String taskId) throws TaskAccessException {
      if (!taskIds.contains(taskId)) {
         throw new IllegalArgumentException("Task not found in generator");
      }

      Iterator<String> iterator = taskIds.iterator();
      while (iterator.hasNext()) {
         String nextId = iterator.next();
         if (nextId.equals(taskId)) {
            return;
         }
         Task task = taskStore.getTasks().getById(nextId);
         task.unlinkGenerator();
         iterator.remove();
      }
   }

   List<String> generateTasks(Instant timestamp) throws TaskAccessException {
      if (!timestamp.isAfter(generationLastTimestamp)) {
         return new ArrayList<>();
      }
      List<String> result = generationDatePattern
         .getDates(generationLastTimestamp, timestamp)
         .stream()
         .map(instant -> Task.newSeriesTask(this, instant))
         // TODO fix when refactoring TaskAccessException
         .peek(task -> { try { taskStore.getTasks().save(task); } catch (TaskAccessException e) {} })
         .map(Task::getId)
         .peek(taskIds::add)
         .collect(Collectors.toList());
      generationLastTimestamp = timestamp;
      return result;
   }

   @Override
   public void registerTaskStore(TaskStore taskStore) {
      this.taskStore = taskStore;
   }

   @Override
   public TaskStore getTaskStore() {
      return taskStore;
   }

   public static Generator createGenerator(Table table, String field, DatePattern pattern) {
      Generator result = new Generator(UUID.randomUUID().toString(), table.getId(), field, pattern);
      result.templateProperties.putAll(table.getDefaultProperties());
      result.registerTaskStore(table.getTaskStore());

      return result;
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("id", id);
      json.put("name", name);
      json.put("dateCreated", dateCreated.toJson());
      json.put("dateLastModified", dateLastModified.toJson());
      json.put("templateName", templateName);
      json.put("templateMarkup", templateMarkup.map(s -> (Object) s).orElse(JSONObject.NULL));
      json.put("templateTable", templateTableId);
      json.put("templateDuration", templateDuration);
      json.put("generationLastTimestamp", generationLastTimestamp.toString());
      json.put("generationField", generationField);
      json.put("generationDatePattern", generationDatePattern.toJson());
      json.put("tasks", taskIds);

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
      result.dateCreated = DateTime.fromJson(json.getJSONObject("dateCreated"));
      result.dateLastModified = DateTime.fromJson(json.getJSONObject("dateLastModified"));
      result.templateName = json.getString("templateName");
      result.templateMarkup = Optional.ofNullable(json.optString("templateMarkup", null));
      result.templateDuration = json.getLong("templateDuration");
      result.generationLastTimestamp = Instant.parse(json.getString("generationLastTimestamp"));

      JSONArray tasksJson = json.getJSONArray("tasks");
      for (int i = 0; i < tasksJson.length(); i++) {
         result.taskIds.add(tasksJson.getString(i));
      }

      JSONObject propertiesJson = json.getJSONObject("templateProperties");
      for (String s : propertiesJson.keySet()) {
         result.templateProperties.put(s, Property.fromJson(propertiesJson.getJSONObject(s)));
      }

      return result;
   }
}
