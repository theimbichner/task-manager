package io.github.theimbichner.task.io;

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

public class JsonFileDataStore<T extends Storable> implements DataStore<T> {
   private static final String EXTENSION = ".json";

   private final File root;
   private final Function<T, JSONObject> toJson;
   private final Function<JSONObject, T> fromJson;

   public JsonFileDataStore(
      File root,
      Function<T, JSONObject> toJson,
      Function<JSONObject, T> fromJson
   ) {
      this.root = root;
      this.toJson = toJson;
      this.fromJson = fromJson;
   }

   private File getFile(String filename) {
      return new File(root, filename + EXTENSION);
   }

   @Override
   public Either<TaskAccessException, T> getById(String id) {
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
   public Either<TaskAccessException, T> save(T t) {
      String id = t.getId();
      root.mkdirs();
      try (PrintWriter writer = new PrintWriter(getFile(id))) {
         toJson.apply(t).write(writer);
         return Either.right(t);
      }
      catch (IOException|JSONException e) {
         return Either.left(new TaskAccessException(e));
      }
   }

   @Override
   public Either<TaskAccessException, Void> deleteById(String id) {
      try {
         Files.delete(getFile(id).toPath());
         return Either.right(null);
      }
      catch (IOException e) {
         return Either.left(new TaskAccessException(e));
      }
   }
}
