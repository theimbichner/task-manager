package io.github.theimbichner.task.time;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class UniformDatePattern implements DatePattern {
   private final Instant initialTime;
   private final Duration deltaTime;

   public UniformDatePattern(Instant initialTime, Duration deltaTime) {
      this.initialTime = initialTime;
      this.deltaTime = deltaTime;
   }

   @Override
   public List<Instant> getDates(Instant start, Instant end) {
      Instant time;
      if (initialTime.isAfter(start)) {
         time = initialTime;
      }
      else {
         Duration diff = Duration.between(initialTime, start);
         long n = 1 + diff.dividedBy(deltaTime);
         time = initialTime.plus(deltaTime.multipliedBy(n));
      }

      List<Instant> result = new ArrayList<>();
      while (!time.isAfter(end)) {
         result.add(time);
         time = time.plus(deltaTime);
      }
      return result;
   }

   @Override
   public JSONObject toJson() {
      JSONObject result = new JSONObject();
      result.put("patternType", "uniform");
      result.put("initialTime", initialTime.toString());
      result.put("deltaTime", deltaTime.toString());
      return result;
   }

   public static UniformDatePattern fromJson(JSONObject json) {
      Instant initialTime = Instant.parse(json.getString("initialTime"));
      Duration deltaTime = Duration.parse(json.getString("deltaTime"));
      return new UniformDatePattern(initialTime, deltaTime);
   }
}
