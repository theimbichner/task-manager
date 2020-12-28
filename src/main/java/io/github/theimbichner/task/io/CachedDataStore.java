package io.github.theimbichner.task.io;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vavr.control.Either;

public class CachedDataStore<T extends Storable> implements DataStore<T> {
   private final Cache<String, T> cache;
   private final DataStore<T> delegate;

   public CachedDataStore(DataStore<T> dataStore, int maxSize) {
      delegate = dataStore;
      cache = Caffeine.newBuilder()
         .maximumSize(maxSize)
         .build();
   }

   @Override
   public Either<TaskAccessException, T> getById(String id) {
      T t = cache.getIfPresent(id);
      if (t != null) {
         return Either.right(t);
      }

      return delegate.getById(id).peek(result -> cache.put(id, result));
   }

   @Override
   public Either<TaskAccessException, T> save(T t) {
      return delegate.save(t).peek(result -> cache.put(result.getId(), result));
   }

   @Override
   public Either<TaskAccessException, Void> deleteById(String id) {
      cache.invalidate(id);
      return delegate.deleteById(id);
   }
}
