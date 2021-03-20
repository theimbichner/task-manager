package io.github.theimbichner.taskmanager.io;

import io.vavr.collection.Set;
import io.vavr.control.Either;

import org.json.JSONException;
import org.json.JSONObject;

import io.github.theimbichner.taskmanager.task.TypeAdapter;

public class JsonAdapterDataStore<K, V extends Storable<K>> extends DataStore<K, V> {
   private final DataStore<String, StringStorable> delegate;
   private final TypeAdapter<V, JSONObject> valueAdapter;
   private final TypeAdapter<K, String> keyAdapter;

   public JsonAdapterDataStore(
      DataStore<String, StringStorable> delegate,
      TypeAdapter<V, JSONObject> valueAdapter,
      TypeAdapter<K, String> keyAdapter
   ) {
      this.delegate = delegate;
      this.valueAdapter = valueAdapter;
      this.keyAdapter = keyAdapter;

      delegate.registerChild(this);
   }

   @Override
   public Either<TaskAccessException, Set<K>> listIds() {
      return delegate.listIds().map(ids -> ids.map(keyAdapter::deconvert));
   }

   @Override
   public Either<TaskAccessException, V> getById(K id) {
      try {
         return delegate.getById(keyAdapter.convert(id))
            .map(result -> {
               JSONObject json = new JSONObject(result.getValue());
               return valueAdapter.deconvert(json);
            });
      }
      catch (JSONException e) {
         return Either.left(new TaskAccessException(e));
      }
   }

   @Override
   public Either<TaskAccessException, V> save(V value) {
      try {
         JSONObject json = valueAdapter.convert(value);
         String stringValue = json.toString();
         String id = keyAdapter.convert(value.getId());
         return delegate.save(new StringStorable(id, stringValue))
            .map(x -> value);
      }
      catch (JSONException e) {
         return Either.left(new TaskAccessException(e));
      }
   }

   @Override
   public Either<TaskAccessException, Void> deleteById(K id) {
      return delegate.deleteById(keyAdapter.convert(id));
   }
}
