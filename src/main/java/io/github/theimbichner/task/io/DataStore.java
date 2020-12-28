package io.github.theimbichner.task.io;

import io.vavr.control.Either;

public interface DataStore<T extends Storable> {
   Either<TaskAccessException, T> getById(String id);
   Either<TaskAccessException, T> save(T t);
   Either<TaskAccessException, Void> deleteById(String id);
}
