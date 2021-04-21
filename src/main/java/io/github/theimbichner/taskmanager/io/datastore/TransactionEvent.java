package io.github.theimbichner.taskmanager.io.datastore;

import java.util.function.Consumer;

public enum TransactionEvent {
   COMMIT_FAILURE(DataStore::onCommitFailure),
   COMMIT_SUCCESS(DataStore::onCommitSuccess),
   CANCEL(DataStore::onCancel);

   private final Consumer<DataStore<?, ?>> action;

   private TransactionEvent(Consumer<DataStore<?, ?>> action) {
      this.action = action;
   }

   public void onEvent(DataStore<?, ?> dataStore) {
      action.accept(dataStore);
   }
}
