package io.github.theimbichner.taskmanager.io.datastore.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vavr.collection.Set;

import io.github.theimbichner.taskmanager.io.TaskAccessResult;
import io.github.theimbichner.taskmanager.io.datastore.DataStore;
import io.github.theimbichner.taskmanager.io.datastore.DelegatingDataStore;
import io.github.theimbichner.taskmanager.io.datastore.Storable;

public class CachedDataStore<K, V extends Storable<K>> extends DelegatingDataStore<K, V, K, V> {
   private final Cache<K, V> cache;

   public CachedDataStore(DataStore<K, V> dataStore, int maxSize) {
      super(dataStore);
      cache = Caffeine.newBuilder()
         .maximumSize(maxSize)
         .build();
   }

   @Override
   public TaskAccessResult<Set<K>> listIds() {
      return getDelegate().listIds();
   }

   @Override
   public TaskAccessResult<V> getById(K id) {
      V value = cache.getIfPresent(id);
      if (value != null) {
         return TaskAccessResult.ofRight(value);
      }

      return getDelegate().getById(id).peek(this::addToCache);
   }

   @Override
   public TaskAccessResult<V> save(V value) {
      return getDelegate().save(value).peek(this::addToCache);
   }

   @Override
   public TaskAccessResult<Void> deleteById(K id) {
      cache.invalidate(id);
      return getDelegate().deleteById(id);
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
