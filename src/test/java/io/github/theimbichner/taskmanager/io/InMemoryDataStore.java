package io.github.theimbichner.taskmanager.io;

import java.util.NoSuchElementException;

import io.vavr.collection.HashMap;
import io.vavr.control.Either;

public class InMemoryDataStore<K, V extends Storable<K>> implements DataStore<K, V> {
   private HashMap<K, V> committedData = HashMap.empty();
   private HashMap<K, V> data = HashMap.empty();

   public static TaskStore createTaskStore() {
      return new TaskStore(
         new InMemoryDataStore<>(),
         new InMemoryDataStore<>(),
         new InMemoryDataStore<>());
   }

   @Override
   public Either<TaskAccessException, V> getById(K id) {
      if (!data.containsKey(id)) {
         return Either.left(new TaskAccessException(new NoSuchElementException()));
      }
      return Either.right(data.get(id).get());
   }

   @Override
   public Either<TaskAccessException, V> save(V value) {
      data = data.put(value.getId(), value);
      return Either.right(value);
   }

   @Override
   public Either<TaskAccessException, Void> deleteById(K id) {
      if (!data.containsKey(id)) {
         return Either.left(new TaskAccessException(new NoSuchElementException()));
      }
      data = data.remove(id);
      return Either.right(null);
   }

   @Override
   public Either<TaskAccessException, Void> commit() {
      committedData = data;
      return Either.right(null);
   }

   @Override
   public void cancelTransaction() {
      data = committedData;
   }
}
