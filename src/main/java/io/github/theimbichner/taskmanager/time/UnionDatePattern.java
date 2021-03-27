package io.github.theimbichner.taskmanager.time;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import io.vavr.collection.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

public class UnionDatePattern implements DatePattern {
   private final List<DatePattern> patterns;

   public UnionDatePattern() {
      this.patterns = new ArrayList<>();
   }

   private UnionDatePattern(List<DatePattern> patterns) {
      this.patterns = patterns;
   }

   public UnionDatePattern withPatterns(DatePattern... toAdd) {
      List<DatePattern> newPatterns = new ArrayList<>(patterns);
      newPatterns.addAll(List.of(toAdd));
      return new UnionDatePattern(newPatterns);
   }

   public int getNumberOfPatterns() {
      return patterns.size();
   }

   public DatePattern getPatternAt(int i) {
      return patterns.get(i);
   }

   public UnionDatePattern withoutPatternAt(int i) {
      List<DatePattern> newPatterns = new ArrayList<>(patterns);
      newPatterns.remove(i);
      return new UnionDatePattern(newPatterns);
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
      JSONObject result = new JSONObject();
      result.put("patternType", "union");
      result.put("patterns", new JSONArray(patterns.stream()
         .map(DatePattern::toJson)
         .toArray()));
      return result;
   }

   public static UnionDatePattern fromJson(JSONObject json) {
      List<DatePattern> patterns = new ArrayList<>();
      JSONArray jsonArray = json.getJSONArray("patterns");
      for (int i = 0; i < jsonArray.length(); i++) {
         patterns.add(DatePattern.fromJson(jsonArray.getJSONObject(i)));
      }
      return new UnionDatePattern(patterns);
   }
}
