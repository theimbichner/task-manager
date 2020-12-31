package io.github.theimbichner.taskmanager.io;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import io.vavr.control.Either;

public class InMemoryDataStore<T extends Storable> implements DataStore<T> {
   private final Map<String, T> data = new HashMap<>();

   public static TaskStore createTaskStore() {
      return new TaskStore(
         new InMemoryDataStore<>(),
         new InMemoryDataStore<>(),
         new InMemoryDataStore<>());
   }

   @Override
   public Either<TaskAccessException, T> getById(String id) {
      if (!data.containsKey(id)) {
         return Either.left(new TaskAccessException(new NoSuchElementException()));
      }
      return Either.right(data.get(id));
   }

   @Override
   public Either<TaskAccessException, T> save(T t) {
      data.put(t.getId(), t);
      return Either.right(t);
   }

   @Override
   public Either<TaskAccessException, Void> deleteById(String id) {
      if (!data.containsKey(id)) {
         return Either.left(new TaskAccessException(new NoSuchElementException()));
      }
      data.remove(id);
      return Either.right(null);
   }
}
