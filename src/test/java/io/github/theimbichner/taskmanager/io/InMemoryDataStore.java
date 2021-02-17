package io.github.theimbichner.taskmanager.io;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import io.vavr.control.Either;

public class InMemoryDataStore<K, V extends Storable<K>> implements DataStore<K, V> {
   private final Map<K, V> data = new HashMap<>();

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
      return Either.right(data.get(id));
   }

   @Override
   public Either<TaskAccessException, V> save(V value) {
      data.put(value.getId(), value);
      return Either.right(value);
   }

   @Override
   public Either<TaskAccessException, Void> deleteById(K id) {
      if (!data.containsKey(id)) {
         return Either.left(new TaskAccessException(new NoSuchElementException()));
      }
      data.remove(id);
      return Either.right(null);
   }
}
