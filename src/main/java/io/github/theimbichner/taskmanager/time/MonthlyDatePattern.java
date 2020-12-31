package io.github.theimbichner.taskmanager.time;

import java.time.Instant;
import java.util.List;

import org.json.JSONObject;

public class MonthlyDatePattern implements DatePattern {
   @Override
   public List<Instant> getDates(Instant start, Instant end) {
      return null;
   }

   @Override
   public JSONObject toJson() {
      return null;
   }

   public static MonthlyDatePattern fromJson(JSONObject json) {
      return null;
   }
}
