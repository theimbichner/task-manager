package io.github.theimbichner.taskmanager.time;

import java.time.Instant;
import java.util.TreeSet;

import io.vavr.collection.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

public class UnionDatePattern implements DatePattern {
   private final Vector<DatePattern> patterns;

   public UnionDatePattern() {
      this.patterns = Vector.empty();
   }

   private UnionDatePattern(Vector<DatePattern> patterns) {
      this.patterns = patterns;
   }

   public UnionDatePattern withPatterns(DatePattern... toAdd) {
      return new UnionDatePattern(patterns.appendAll(Vector.of(toAdd)));
   }

   public int getNumberOfPatterns() {
      return patterns.size();
   }

   public DatePattern getPatternAt(int i) {
      return patterns.get(i);
   }

   public UnionDatePattern withoutPatternAt(int i) {
      return new UnionDatePattern(patterns.removeAt(i));
   }

   @Override
   public Vector<Instant> getDates(Instant start, Instant end) {
      TreeSet<Instant> set = new TreeSet<>();
      for (DatePattern p : patterns) {
         set.addAll(p.getDates(start, end).asJava());
      }
      return Vector.ofAll(set);
   }

   @Override
   public JSONObject toJson() {
      Vector<JSONObject> jsonPatterns = patterns.map(DatePattern::toJson);

      JSONObject json = new JSONObject();
      json.put("patternType", "union");
      json.put("patterns", new JSONArray(jsonPatterns.asJava()));

      return json;
   }

   public static UnionDatePattern fromJson(JSONObject json) {
      JSONArray jsonArray = json.getJSONArray("patterns");
      return new UnionDatePattern(Vector.range(0, jsonArray.length())
         .map(jsonArray::getJSONObject)
         .map(DatePattern::fromJson));
   }
}
