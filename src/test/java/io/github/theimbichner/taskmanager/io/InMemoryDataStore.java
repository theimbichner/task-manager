package io.github.theimbichner.taskmanager.io;

import java.util.NoSuchElementException;

import io.vavr.collection.HashMap;
import io.vavr.collection.Set;

public class InMemoryDataStore<K, V extends Storable<K>> extends MultiChannelDataStore<K, V> {
   private HashMap<String, HashMap<K, V>> committedData = HashMap.empty();
   private HashMap<String, HashMap<K, V>> data = HashMap.empty();

   public static TaskStore createTaskStore() {
      return new TaskStore(new InMemoryDataStore<>(), 0, 0, 0);
   }

   @Override
   protected DataStore<K, V> createChannel(String channelId) {
      return new DataStore<>() {
         @Override
         public TaskAccessResult<Set<K>> listIds() {
            HashMap<K, V> map = data.getOrElse(channelId, HashMap.empty());
            return TaskAccessResult.ofRight(map.keySet());
         }

         @Override
         public TaskAccessResult<V> getById(K id) {
            try {
               V result = data.get(channelId).get().get(id).get();
               return TaskAccessResult.ofRight(result);
            }
            catch (NoSuchElementException e) {
               return TaskAccessResult.ofLeft(new TaskAccessException(e));
            }
         }

         @Override
         public TaskAccessResult<V> save(V value) {
            HashMap<K, V> map = data.get(channelId).getOrElse(HashMap.empty());
            map = map.put(value.getId(), value);
            data = data.put(channelId, map);
            return TaskAccessResult.ofRight(value);
         }

         @Override
         public TaskAccessResult<Void> deleteById(K id) {
            try {
               HashMap<K, V> map = data.get(channelId).get();
               map.get(id).get();
               map = map.remove(id);
               data = data.put(channelId, map);
               return TaskAccessResult.ofRight(null);
            }
            catch (NoSuchElementException e) {
               return TaskAccessResult.ofLeft(new TaskAccessException(e));
            }
         }
      };
   }

   @Override
   protected TaskAccessResult<Void> performCommit() {
      committedData = data;
      return TaskAccessResult.ofRight(null);
   }

   @Override
   protected void performCancel() {
      data = committedData;
   }
}
