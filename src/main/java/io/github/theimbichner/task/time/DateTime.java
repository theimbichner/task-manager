package io.github.theimbichner.task.time;

import java.time.Instant;

import org.json.JSONObject;

public class DateTime {
   private final Instant start;
   private final Instant end;

   public DateTime() {
      start = Instant.now();
      end = start;
   }

   public DateTime(long timestamp) {
      start = Instant.ofEpochSecond(timestamp);
      end = start;
   }

   public DateTime(long start, long end) {
      this.start = Instant.ofEpochSecond(start);
      this.end = Instant.ofEpochSecond(end);
   }

   private DateTime(Instant start, Instant end) {
      this.start = start;
      this.end = end;
   }

   public Instant getStart() {
      return start;
   }

   public Instant getEnd() {
      return end;
   }

   public DateTime withDuration(long duration) {
      return new DateTime(start, start.plusSeconds(duration));
   }

   public JSONObject toJson() {
      JSONObject result = new JSONObject();
      result.put("start", start.toString());
      result.put("end", end.toString());
      return result;
   }

   public static DateTime fromJson(JSONObject json) {
      Instant start = Instant.parse(json.getString("start"));
      Instant end = Instant.parse(json.getString("end"));
      return new DateTime(start, end);
   }
}
