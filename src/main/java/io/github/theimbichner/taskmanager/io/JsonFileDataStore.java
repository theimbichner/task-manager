package io.github.theimbichner.taskmanager.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.function.Function;

import io.vavr.control.Either;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JsonFileDataStore<K, V extends Storable<K>> implements DataStore<K, V> {
   private static final String EXTENSION = ".json";

   private final File root;
   private final Function<V, JSONObject> toJson;
   private final Function<JSONObject, V> fromJson;

   public JsonFileDataStore(
      File root,
      Function<V, JSONObject> toJson,
      Function<JSONObject, V> fromJson
   ) {
      this.root = root;
      this.toJson = toJson;
      this.fromJson = fromJson;
   }

   private File getFile(K id) {
      String filename = id.toString();
      return new File(root, filename + EXTENSION);
   }

   @Override
   public Either<TaskAccessException, V> getById(K id) {
      try (FileInputStream stream = new FileInputStream(getFile(id))) {
         JSONTokener tokener = new JSONTokener(stream);
         JSONObject json = new JSONObject(tokener);
         return Either.right(fromJson.apply(json));
      }
      catch (IOException|JSONException e) {
         return Either.left(new TaskAccessException(e));
      }
   }

   @Override
   public Either<TaskAccessException, V> save(V value) {
      K id = value.getId();
      root.mkdirs();
      try (PrintWriter writer = new PrintWriter(getFile(id))) {
         toJson.apply(value).write(writer);
         return Either.right(value);
      }
      catch (IOException|JSONException e) {
         return Either.left(new TaskAccessException(e));
      }
   }

   @Override
   public Either<TaskAccessException, Void> deleteById(K id) {
      try {
         Files.delete(getFile(id).toPath());
         return Either.right(null);
      }
      catch (IOException e) {
         return Either.left(new TaskAccessException(e));
      }
   }
}
