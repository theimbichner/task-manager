package io.github.theimbichner.taskmanager.io.datastore.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vavr.collection.Set;

import io.github.theimbichner.taskmanager.io.TaskAccessResult;
import io.github.theimbichner.taskmanager.io.datastore.DataStore;
import io.github.theimbichner.taskmanager.io.datastore.Storable;

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
   public TaskAccessResult<Set<K>> listIds() {
      return delegate.listIds();
   }

   @Override
   public TaskAccessResult<V> getById(K id) {
      V value = cache.getIfPresent(id);
      if (value != null) {
         return TaskAccessResult.ofRight(value);
      }

      return delegate.getById(id).peek(this::addToCache);
   }

   @Override
   public TaskAccessResult<V> save(V value) {
      return delegate.save(value).peek(this::addToCache);
   }

   @Override
   public TaskAccessResult<Void> deleteById(K id) {
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

   private void addToCache(V value) {
      cache.put(value.getId(), value);
   }
}
