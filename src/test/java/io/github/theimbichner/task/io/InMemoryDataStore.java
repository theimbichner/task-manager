package io.github.theimbichner.task.io;

import java.util.HashMap;
import java.util.Map;

public class InMemoryDataStore<T extends Storable> implements DataStore<T> {
   private Map<String, T> data = new HashMap<>();

   @Override
   public T getById(String id) throws TaskAccessException {
      if (!data.containsKey(id)) {
         throw new TaskAccessException(new IllegalStateException());
      }
      return data.get(id);
   }

   @Override
   public void save(T t) {
      data.put(t.getId(), t);
   }

   @Override
   public void deleteById(String id) throws TaskAccessException {
      if (!data.containsKey(id)) {
         throw new TaskAccessException(new IllegalStateException());
      }
      data.remove(id);
   }
}
