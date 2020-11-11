package io.github.theimbichner.task;

import java.util.UUID;

import org.json.JSONObject;

import io.github.theimbichner.task.io.Storable;
import io.github.theimbichner.task.io.TaskStore;

public class Generator implements Storable {
   private final String id;

   private TaskStore taskStore;

   private Generator(String id) {
      this.id = id;

      taskStore = null;
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public void registerTaskStore(TaskStore taskStore) {
      this.taskStore = taskStore;
   }

   @Override
   public TaskStore getTaskStore() {
      return taskStore;
   }

   public static Generator createGenerator() {
      return new Generator(UUID.randomUUID().toString());
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("id", id);
      return json;
   }

   public static Generator fromJson(JSONObject json) {
      String id = json.getString("id");
      Generator result = new Generator(id);
      return result;
   }
}
