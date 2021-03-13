package io.github.theimbichner.taskmanager.io;

import io.vavr.control.Either;

public interface MultiChannelDataStore<K, V extends Storable<K>> {
   DataStore<K, V> getChannel(String channelId);
   Either<TaskAccessException, Void> commit();
   void cancelTransaction();
}
