package io.github.theimbichner.taskmanager.io.datastore.impl;

import io.vavr.collection.Set;

import org.json.JSONException;
import org.json.JSONObject;

import io.github.theimbichner.taskmanager.io.TaskAccessException;
import io.github.theimbichner.taskmanager.io.TaskAccessResult;
import io.github.theimbichner.taskmanager.io.datastore.DataStore;
import io.github.theimbichner.taskmanager.io.datastore.DelegatingDataStore;
import io.github.theimbichner.taskmanager.io.datastore.Storable;
import io.github.theimbichner.taskmanager.io.datastore.StringStorable;
import io.github.theimbichner.taskmanager.task.TypeAdapter;

public class JsonAdapterDataStore<K, V extends Storable<K>> extends DelegatingDataStore<K, V, String, StringStorable> {
   private final TypeAdapter<V, JSONObject> valueAdapter;
   private final TypeAdapter<K, String> keyAdapter;

   public JsonAdapterDataStore(
      DataStore<String, StringStorable> delegate,
      TypeAdapter<V, JSONObject> valueAdapter,
      TypeAdapter<K, String> keyAdapter
   ) {
      super(delegate);
      this.valueAdapter = valueAdapter;
      this.keyAdapter = keyAdapter;
   }

   @Override
   public TaskAccessResult<Set<K>> listIds() {
      return getDelegate().listIds().andThen(ids -> ids.map(keyAdapter::deconvert));
   }

   @Override
   public TaskAccessResult<V> getById(K id) {
      try {
         return getDelegate()
            .getById(keyAdapter.convert(id))
            .andThen(result -> {
               JSONObject json = new JSONObject(result.getValue());
               return valueAdapter.deconvert(json);
            });
      }
      catch (JSONException e) {
         return TaskAccessResult.ofLeft(new TaskAccessException(e));
      }
   }

   @Override
   public TaskAccessResult<V> save(V value) {
      try {
         JSONObject json = valueAdapter.convert(value);
         String stringValue = json.toString();
         String id = keyAdapter.convert(value.getId());
         return getDelegate()
            .save(new StringStorable(id, stringValue))
            .andThen(x -> value);
      }
      catch (JSONException e) {
         return TaskAccessResult.ofLeft(new TaskAccessException(e));
      }
   }

   @Override
   public TaskAccessResult<Void> deleteById(K id) {
      return getDelegate().deleteById(keyAdapter.convert(id));
   }
}
