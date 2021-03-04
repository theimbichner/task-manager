package io.github.theimbichner.taskmanager.io;

import io.vavr.control.Either;

public interface DataStore<K, V extends Storable<K>> {
   Either<TaskAccessException, V> getById(K id);
   Either<TaskAccessException, V> save(V value);
   Either<TaskAccessException, Void> deleteById(K id);
   Either<TaskAccessException, Void> commit();
   void cancelTransaction();
}
