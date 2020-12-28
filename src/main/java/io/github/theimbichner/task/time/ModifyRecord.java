package io.github.theimbichner.task.time;

import java.time.Instant;

import org.json.JSONObject;

public class ModifyRecord {
   private final Instant created;
   private final Instant lastModified;

   private ModifyRecord(Instant created, Instant lastModified) {
      this.created = created;
      this.lastModified = lastModified;
   }

   public Instant getDateCreated() {
      return created;
   }

   public Instant getDateLastModified() {
      return lastModified;
   }

   public static ModifyRecord createdNow() {
      Instant now = Instant.now();
      return new ModifyRecord(now, now);
   }

   public ModifyRecord updatedNow() {
      return new ModifyRecord(created, Instant.now());
   }

   public void writeIntoJson(JSONObject json) {
      json.put("dateCreated", created.toString());
      json.put("dateLastModified", lastModified.toString());
   }

   public static ModifyRecord readFromJson(JSONObject json) {
      Instant created = Instant.parse(json.getString("dateCreated"));
      Instant lastModified = Instant.parse(json.getString("dateLastModified"));
      return new ModifyRecord(created, lastModified);
   }
}
