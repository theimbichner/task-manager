package io.github.theimbichner.taskmanager.task;

import java.util.UUID;

public class ItemId<T> {
   private final String value;

   private ItemId(String value) {
      this.value = value;
   }

   public static <T> ItemId<T> randomId() {
      return new ItemId<>(UUID.randomUUID().toString());
   }

   public static <T> ItemId<T> of(String value) {
      if (value == null) {
         throw new NullPointerException("ItemId value cannot be null.");
      }

      return new ItemId<>(value);
   }

   public static <T> TypeAdapter<ItemId<T>, String> stringAdapter() {
      return new TypeAdapter<>(ItemId::toString, ItemId::of);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof ItemId) {
         return value.equals(((ItemId<?>) obj).value);
      }

      return false;
   }

   @Override
   public int hashCode() {
      return value.hashCode();
   }

   @Override
   public String toString() {
      return value;
   }
}
