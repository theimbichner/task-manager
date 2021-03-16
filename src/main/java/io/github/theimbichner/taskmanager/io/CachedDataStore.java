package io.github.theimbichner.taskmanager.io;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vavr.control.Either;

public class CachedDataStore<K, V extends Storable<K>> extends DataStore<K, V> {
   private final Cache<K, V> cache;
   private final DataStore<K, V> delegate;

   public CachedDataStore(DataStore<K, V> dataStore, int maxSize) {
      delegate = dataStore;
      cache = Caffeine.newBuilder()
         .maximumSize(maxSize)
         .build();

      dataStore.registerChild(this);
   }

   @Override
   public Either<TaskAccessException, V> getById(K id) {
      V value = cache.getIfPresent(id);
      if (value != null) {
         return Either.right(value);
      }

      return delegate.getById(id).peek(result -> cache.put(id, result));
   }

   @Override
   public Either<TaskAccessException, V> save(V value) {
      return delegate
         .save(value)
         .peek(result -> cache.put(result.getId(), result));
   }

   @Override
   public Either<TaskAccessException, Void> deleteById(K id) {
      cache.invalidate(id);
      return delegate.deleteById(id);
   }

   @Override
   protected void onCommitFailure() {
      cache.invalidateAll();
   }

   @Override
   protected void onCancel() {
      cache.invalidateAll();
   }
}
