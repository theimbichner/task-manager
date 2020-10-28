package io.github.theimbichner.task.io;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class CachedDataStore<T> implements DataStore<T> {
   private final Cache<String, T> cache;
   private final DataStore<T> delegate;

   public CachedDataStore(DataStore<T> dataStore, int maxSize) {
      delegate = dataStore;
      cache = Caffeine.newBuilder()
         .maximumSize(maxSize)
         .build();
   }

   @Override
   public String getId(T t) {
      return delegate.getId(t);
   }

   @Override
   public T getById(String id) throws TaskAccessException {
      T t = cache.getIfPresent(id);
      if (t == null) {
         t = delegate.getById(id);
         cache.put(id, t);
      }
      return t;
   }

   @Override
   public void save(T t) throws TaskAccessException {
      delegate.save(t);
      cache.put(getId(t), t);
   }

   @Override
   public void deleteById(String id) throws TaskAccessException {
      cache.invalidate(id);
      delegate.deleteById(id);
   }
}
