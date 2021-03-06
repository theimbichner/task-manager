package io.github.theimbichner.taskmanager.io.datastore;

import io.vavr.Tuple2;
import io.vavr.collection.HashMap;

import io.github.theimbichner.taskmanager.io.TaskAccessResult;

public abstract class MultiChannelDataStore<K, V extends Storable<K>> {
   private HashMap<String, DataStore<K, V>> channels = HashMap.empty();

   protected abstract DataStore<K, V> createChannel(String channelId);
   protected abstract TaskAccessResult<Void> performCommit();
   protected abstract void performCancel();

   public final DataStore<K, V> getChannel(String channelId) {
      DataStore<K, V> result = channels
         .get(channelId)
         .getOrElse(() -> createChannel(channelId));
      channels = channels.put(channelId, result);
      return result;
   }

   public final TaskAccessResult<Void> commit() {
      return performCommit()
         .peek(x -> sendEvents(TransactionEvent.COMMIT_SUCCESS))
         .peekLeft(x -> sendEvents(TransactionEvent.COMMIT_FAILURE));
   }

   public final void cancelTransaction() {
      performCancel();
      sendEvents(TransactionEvent.CANCEL);
   }

   private void sendEvents(TransactionEvent event) {
      for (Tuple2<String, DataStore<K, V>> entry : channels) {
         entry._2.dispatchTransactionEvent(event);
      }
   }
}
