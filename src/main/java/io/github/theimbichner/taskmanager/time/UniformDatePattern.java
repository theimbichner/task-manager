package io.github.theimbichner.taskmanager.time;

import java.time.Duration;
import java.time.Instant;

import io.vavr.collection.Vector;

import org.json.JSONObject;

public class UniformDatePattern implements DatePattern {
   private final Instant initialTime;
   private final Duration deltaTime;

   public UniformDatePattern(Instant initialTime, Duration deltaTime) {
      this.initialTime = initialTime;
      this.deltaTime = deltaTime;
   }

   @Override
   public Vector<Instant> getDates(Instant start, Instant end) {
      if (!start.isBefore(end)) {
         throw new IllegalArgumentException();
      }

      Instant time;
      if (initialTime.isAfter(start)) {
         time = initialTime;
      }
      else {
         Duration diff = Duration.between(initialTime, start);
         long n = 1 + diff.dividedBy(deltaTime);
         time = initialTime.plus(deltaTime.multipliedBy(n));
      }

      Vector<Instant> result = Vector.empty();
      while (!time.isAfter(end)) {
         result = result.append(time);
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
