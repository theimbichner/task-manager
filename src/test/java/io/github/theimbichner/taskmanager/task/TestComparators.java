package io.github.theimbichner.taskmanager.task;

import org.json.JSONObject;

public class TestComparators {
   private static int compareJson(JSONObject lhs, JSONObject rhs) {
      return lhs.similar(rhs) ? 0 : 1;
   }

   private static JSONObject stripIdentifyingFields(JSONObject json) {
      json.remove("id");
      json.remove("dateCreated");
      json.remove("dateLastModified");
      json.remove("generationLastTimestamp");

      return json;
   }

   public static int compareTables(Table lhs, Table rhs) {
      return compareJson(lhs.toJson(), rhs.toJson());
   }

   public static int compareTablesIgnoringId(Table lhs, Table rhs) {
      return compareJson(
         stripIdentifyingFields(lhs.toJson()),
         stripIdentifyingFields(rhs.toJson()));
   }

   public static int compareTasks(Task lhs, Task rhs) {
      return compareJson(lhs.toJson(), rhs.toJson());
   }

   public static int compareTasksIgnoringId(Task lhs, Task rhs) {
      return compareJson(
         stripIdentifyingFields(lhs.toJson()),
         stripIdentifyingFields(rhs.toJson()));
   }

   public static int compareGenerators(Generator lhs, Generator rhs) {
      return compareJson(lhs.toJson(), rhs.toJson());
   }

   public static int compareGeneratorsIgnoringId(Generator lhs, Generator rhs) {
      return compareJson(
         stripIdentifyingFields(lhs.toJson()),
         stripIdentifyingFields(rhs.toJson()));
   }
}
