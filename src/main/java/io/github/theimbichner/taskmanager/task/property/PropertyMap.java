package io.github.theimbichner.taskmanager.task.property;

import io.vavr.Tuple2;
import io.vavr.collection.HashMap;

import org.json.JSONObject;

public class PropertyMap {
   private final HashMap<String, Property> map;

   private PropertyMap(HashMap<String, Property> map) {
      this.map = map;
   }

   public static PropertyMap empty() {
      return new PropertyMap(HashMap.empty());
   }

   public static PropertyMap fromJava(java.util.Map<String, Property> javaMap) {
      return new PropertyMap(HashMap.ofAll(javaMap));
   }

   public PropertyMap put(String key, Property property) {
      return new PropertyMap(map.put(key, property));
   }

   public PropertyMap merge(PropertyMap delta) {
      HashMap<String, Property> result = map;
      for (Tuple2<String, Property> entry : delta.asMap()) {
         if (entry._2 == Property.DELETE) {
            result = result.remove(entry._1);
         }
         else {
            result = result.put(entry);
         }
      }

      return new PropertyMap(result);
   }

   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      for (Tuple2<String, Property> entry : map) {
         json.put(entry._1, entry._2.toJson());
      }

      return json;
   }

   public static PropertyMap fromJson(JSONObject json) {
      HashMap<String, Property> result = HashMap.empty();
      for (String s : json.keySet()) {
         result = result.put(s, Property.fromJson(json.getJSONObject(s)));
      }

      return new PropertyMap(result);
   }

   public HashMap<String, Property> asMap() {
      return map;
   }
}
