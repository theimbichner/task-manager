package io.github.theimbichner.taskmanager.time;

import java.time.Instant;

import org.json.JSONObject;

public class DateTime {
   private final Instant start;
   private final Instant end;

   public DateTime() {
      start = Instant.now();
      end = start;
   }

   public DateTime(Instant timestamp) {
      start = timestamp;
      end = timestamp;
   }

   public DateTime(Instant start, Instant end) {
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

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof DateTime)) {
         return false;
      }

      DateTime other = (DateTime) obj;
      return start.equals(other.start) && end.equals(other.end);
   }

   @Override
   public int hashCode() {
      return start.hashCode() ^ end.hashCode();
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
