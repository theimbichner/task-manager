package io.github.theimbichner.taskmanager.io.datastore;

import io.vavr.collection.Set;
import io.vavr.collection.Vector;

import io.github.theimbichner.taskmanager.io.TaskAccessResult;

public abstract class DataStore<K, V extends Storable<K>> {
   private Vector<DelegatingDataStore<?, ?, K, V>> children = Vector.empty();

   public abstract TaskAccessResult<Set<K>> listIds();
   public abstract TaskAccessResult<V> getById(K id);
   public abstract TaskAccessResult<V> save(V value);
   public abstract TaskAccessResult<Void> deleteById(K id);

   protected void onCommitFailure() {}
   protected void onCommitSuccess() {}
   protected void onCancel() {}

   public final void registerChild(DelegatingDataStore<?, ?, K, V> child) {
      children = children.append(child);
   }

   public final void dispatchTransactionEvent(TransactionEvent event) {
      event.onEvent(this);
      for (DataStore<?, ?> child : children) {
         child.dispatchTransactionEvent(event);
      }
   }
}
