package io.github.theimbichner.task.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.function.Function;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JsonFileDataStore<T> implements DataStore<T> {
   private static final String EXTENSION = ".json";

   private final File root;
   private final Function<T, String> getId;
   private final Function<T, JSONObject> toJson;
   private final Function<JSONObject, T> fromJson;

   public JsonFileDataStore(
      File root,
      Function<T, String> getId,
      Function<T, JSONObject> toJson,
      Function<JSONObject, T> fromJson
   ) {
      this.root = root;
      this.getId = getId;
      this.toJson = toJson;
      this.fromJson = fromJson;
   }

   private File getFile(String filename) {
      return new File(root, filename + EXTENSION);
   }

   @Override
   public String getId(T t) {
      return getId.apply(t);
   }

   @Override
   public T getById(String id) throws TaskAccessException {
      try (FileInputStream stream = new FileInputStream(getFile(id))) {
         JSONTokener tokener = new JSONTokener(stream);
         JSONObject json = new JSONObject(tokener);
         return fromJson.apply(json);
      }
      catch (IOException|JSONException e) {
         throw new TaskAccessException(e);
      }
   }

   @Override
   public void save(T t) throws TaskAccessException {
      String id = getId(t);
      root.mkdirs();
      try (PrintWriter writer = new PrintWriter(getFile(id))) {
         toJson.apply(t).write(writer);
      }
      catch (IOException|JSONException e) {
         throw new TaskAccessException(e);
      }
   }

   @Override
   public void deleteById(String id) throws TaskAccessException {
      try {
         Files.delete(getFile(id).toPath());
      }
      catch (IOException e) {
         throw new TaskAccessException(e);
      }
   }
}
