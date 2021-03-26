package io.github.theimbichner.taskmanager.io;

import java.util.NoSuchElementException;

import io.vavr.collection.HashMap;
import io.vavr.collection.Set;
import io.vavr.control.Either;

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
         public Either<TaskAccessException, Set<K>> listIds() {
            HashMap<K, V> map = data.getOrElse(channelId, HashMap.empty());
            return Either.right(map.keySet());
         }

         @Override
         public Either<TaskAccessException, V> getById(K id) {
            try {
               V result = data.get(channelId).get().get(id).get();
               return Either.right(result);
            }
            catch (NoSuchElementException e) {
               return Either.left(new TaskAccessException(e));
            }
         }

         @Override
         public Either<TaskAccessException, V> save(V value) {
            HashMap<K, V> map = data.get(channelId).getOrElse(HashMap.empty());
            map = map.put(value.getId(), value);
            data = data.put(channelId, map);
            return Either.right(value);
         }

         @Override
         public Either<TaskAccessException, Void> deleteById(K id) {
            try {
               HashMap<K, V> map = data.get(channelId).get();
               map.get(id).get();
               map = map.remove(id);
               data = data.put(channelId, map);
               return Either.right(null);
            }
            catch (NoSuchElementException e) {
               return Either.left(new TaskAccessException(e));
            }
         }
      };
   }

   @Override
   protected Either<TaskAccessException, Void> performCommit() {
      committedData = data;
      return Either.right(null);
   }

   @Override
   protected void performCancel() {
      data = committedData;
   }
}
