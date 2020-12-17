package io.github.theimbichner.task.io;

public interface DataStore<T extends Storable> {
   T getById(String id) throws TaskAccessException;
   void save(T t) throws TaskAccessException;
   void deleteById(String id) throws TaskAccessException;
}
