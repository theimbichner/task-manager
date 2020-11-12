package io.github.theimbichner.task;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.theimbichner.task.io.Storable;
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
   private String templateMarkup;
   private final String templateTableId;
   private final Map<String, Property> templateProperties;
   private long templateDuration;
   private Instant generationLastTimestamp;
   private final String generationField;
   private final DatePattern generationDatePattern;
   private final Set<String> taskIds;

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
      templateMarkup = null;
      templateProperties = new HashMap<>();
      templateDuration = 0;
      generationLastTimestamp = Instant.now();
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

   public DateTime getLastModified() {
      return dateLastModified;
   }

   public String getTemplateName() {
      return templateName;
   }

   public String getTemplateMarkup() {
      return templateMarkup;
   }

   public Property getProperty(String key) {
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
      json.put("templateMarkup", templateMarkup == null ? JSONObject.NULL : templateMarkup);
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
      result.templateMarkup = json.optString("templateMarkup", null);
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
