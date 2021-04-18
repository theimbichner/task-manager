package io.github.theimbichner.taskmanager.io.datastore;

public class StringStorable implements Storable<String> {
   private final String id;
   private final String value;

   public StringStorable(String id, String value) {
      this.id = id;
      this.value = value;
   }

   @Override
   public String getId() {
      return id;
   }

   public String getValue() {
      return value;
   }
}
