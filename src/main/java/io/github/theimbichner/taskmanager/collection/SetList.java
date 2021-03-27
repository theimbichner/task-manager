package io.github.theimbichner.taskmanager.collection;

import java.util.NoSuchElementException;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashSet;
import io.vavr.collection.Vector;

public class SetList<T> {
   private final HashSet<T> set;
   private HashSet<T> removed;
   private Vector<T> list;

   private SetList(HashSet<T> set, HashSet<T> removed, Vector<T> list) {
      this.set = set;
      this.removed = removed;
      this.list = list;
   }

   public static <T> SetList<T> empty() {
      return new SetList<>(HashSet.empty(), HashSet.empty(), Vector.empty());
   }

   private void clean() {
      list = list.removeAll(removed);
      removed = HashSet.empty();
   }

   public SetList<T> add(T t) {
      if (set.contains(t)) {
         return this;
      }
      if (removed.contains(t)) {
         clean();
      }

      return new SetList<>(set.add(t), removed, list.append(t));
   }

   public SetList<T> addAll(Iterable<T> ts) {
      SetList<T> result = this;
      for (T t : ts) {
         result = result.add(t);
      }

      return result;
   }

   public SetList<T> remove(T t) {
      if (!set.contains(t)) {
         return this;
      }

      return new SetList<>(set.remove(t), removed.add(t), list);
   }

   public SetList<T> removeAll(Iterable<T> ts) {
      SetList<T> result = this;
      for (T t : ts) {
         result = result.remove(t);
      }

      return result;
   }

   public boolean contains(T t) {
      return set.contains(t);
   }

   public Tuple2<Vector<T>, Vector<T>> split(T t) {
      clean();
      int index = list.indexOf(t, 0);
      if (index == -1) {
         throw new NoSuchElementException();
      }

      // t should fall into the right Vector
      Vector<T> left = list.dropRight(list.size() - index);
      Vector<T> right = list.drop(index);

      return Tuple.of(left, right);
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof SetList)) {
         return false;
      }

      SetList<?> other = (SetList<?>) obj;

      return asList().equals(other.asList());
   }

   @Override
   public int hashCode() {
      return asList().hashCode();
   }

   public Vector<T> asList() {
      clean();
      return list;
   }
}
