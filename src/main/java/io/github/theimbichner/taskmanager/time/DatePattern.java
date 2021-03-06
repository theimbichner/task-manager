package io.github.theimbichner.taskmanager.time;

import java.time.Instant;

import io.vavr.collection.Vector;

import org.json.JSONException;
import org.json.JSONObject;

public interface DatePattern {
   static DatePattern fromJson(JSONObject json) {
      String patternType = json.getString("patternType");
      switch(json.getString("patternType")) {
      case "union":
         return UnionDatePattern.fromJson(json);
      case "uniform":
         return UniformDatePattern.fromJson(json);
      case "monthly":
         return MonthlyDatePattern.fromJson(json);
      default:
         String format = "Unrecognized patternType: %s";
         String message = String.format(format, patternType);
         throw new JSONException(message);
      }
   }

   /**
    * Returns a list of matching Instants that lie between start (exclusive) and
    * end (inclusive). Start must be strictly before end. The resulting list
    * will be sorted.
    *
    * @param start The start of the time range, exclusive
    * @param end The end of the time range, inclusive
    * @return A list of matching Instants between start and end.
    */
   Vector<Instant> getDates(Instant start, Instant end);
   JSONObject toJson();
}
