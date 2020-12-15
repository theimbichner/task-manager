package io.github.theimbichner.task.io;

import io.github.theimbichner.task.Generator;
import io.github.theimbichner.task.Table;
import io.github.theimbichner.task.Task;

public interface DataStore<T extends Storable> {
   T getById(String id) throws TaskAccessException;
   void save(T t) throws TaskAccessException;
   void deleteById(String id) throws TaskAccessException;
}
