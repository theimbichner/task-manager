package io.github.theimbichner.taskmanager.io;

import java.util.function.Function;

import io.vavr.control.Either;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonAdapterDataStore<K, V extends Storable<K>> extends DataStore<K, V> {
   private final Function<V, JSONObject> toJson;
   private final Function<JSONObject, V> fromJson;
   private final DataStore<String, StringStorable> delegate;

   public JsonAdapterDataStore(
      DataStore<String, StringStorable> delegate,
      Function<V, JSONObject> toJson,
      Function<JSONObject, V> fromJson
   ) {
      this.delegate = delegate;
      this.toJson = toJson;
      this.fromJson = fromJson;

      delegate.registerChild(this);
   }

   @Override
   public Either<TaskAccessException, V> getById(K id) {
      try {
         return delegate.getById(id.toString())
            .map(result -> {
               JSONObject json = new JSONObject(result.getValue());
               return fromJson.apply(json);
            });
      }
      catch (JSONException e) {
         return Either.left(new TaskAccessException(e));
      }
   }

   @Override
   public Either<TaskAccessException, V> save(V value) {
      try {
         JSONObject json = toJson.apply(value);
         String stringValue = json.toString();
         String id = value.getId().toString();
         return delegate.save(new StringStorable(id, stringValue))
            .map(x -> value);
      }
      catch (JSONException e) {
         return Either.left(new TaskAccessException(e));
      }
   }

   @Override
   public Either<TaskAccessException, Void> deleteById(K id) {
      return delegate.deleteById(id.toString());
   }
}
