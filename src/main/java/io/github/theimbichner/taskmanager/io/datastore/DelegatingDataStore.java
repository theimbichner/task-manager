package io.github.theimbichner.taskmanager.io.datastore;

public abstract class DelegatingDataStore<K1, V1 extends Storable<K1>, K2, V2 extends Storable<K2>> extends DataStore<K1, V1> {
   private final DataStore<K2, V2> delegate;

   protected DelegatingDataStore(DataStore<K2, V2> parent) {
      parent.registerChild(this);
      this.delegate = parent;
   }

   protected DataStore<K2, V2> getDelegate() {
      return delegate;
   }
}
