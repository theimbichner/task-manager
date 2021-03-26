package io.github.theimbichner.taskmanager.io;

import io.vavr.collection.Set;
import io.vavr.collection.Vector;
import io.vavr.control.Either;

public abstract class DataStore<K, V extends Storable<K>> {
   private Vector<DataStore<?, ?>> children = Vector.empty();

   public abstract Either<TaskAccessException, Set<K>> listIds();
   public abstract Either<TaskAccessException, V> getById(K id);
   public abstract Either<TaskAccessException, V> save(V value);
   public abstract Either<TaskAccessException, Void> deleteById(K id);

   protected void onCommitFailure() {}
   protected void onCommitSuccess() {}
   protected void onCancel() {}

   public final void registerChild(DataStore<?, ?> child) {
      children = children.append(child);
   }

   public final void dispatchTransactionEvent(TransactionEvent event) {
      event.onEvent(this);
      for (DataStore<?, ?> child : children) {
         child.dispatchTransactionEvent(event);
      }
   }
}
