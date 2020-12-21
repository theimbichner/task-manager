package io.github.theimbichner.task.collection;

import java.util.List;

import io.vavr.collection.HashSet;
import io.vavr.collection.Vector;

public class SetList<T> {
   private final HashSet<T> set;
   private final Vector<T> list;

   private SetList(HashSet<T> set, Vector<T> list) {
      this.set = set;
      this.list = list;
   }

   public static <T> SetList<T> empty() {
      return new SetList<>(HashSet.empty(), Vector.empty());
   }

   public List<T> asList() {
       return list.asJava();
   }
}
