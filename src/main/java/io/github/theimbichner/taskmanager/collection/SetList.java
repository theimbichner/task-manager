package io.github.theimbichner.taskmanager.collection;

import java.util.NoSuchElementException;

import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashSet;
import io.vavr.collection.Vector;

public class SetList<T> {
   private final HashSet<T> set;
   private final HashSet<T> removed;
   private final Vector<T> list;
   private final Lazy<SetList<T>> cleaned;

   private SetList(HashSet<T> set, HashSet<T> removed, Vector<T> list) {
      this.set = set;
      this.removed = removed;
      this.list = list;
      if (removed.isEmpty()) {
         this.cleaned = Lazy.of(() -> this);
      }
      else {
         this.cleaned = Lazy.of(this::cleaned);
      }
   }

   public static <T> SetList<T> empty() {
      return new SetList<>(HashSet.empty(), HashSet.empty(), Vector.empty());
   }

   private SetList<T> cleaned() {
      return new SetList<>(set, HashSet.empty(), list.removeAll(removed));
   }

   public SetList<T> add(T t) {
      if (set.contains(t)) {
         return this;
      }

      SetList<T> base = this;
      if (removed.contains(t)) {
         base = cleaned.get();
      }

      return new SetList<>(base.set.add(t), base.removed, base.list.append(t));
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
      if (!set.contains(t)) {
         throw new NoSuchElementException();
      }

      Vector<T> cleanList = asList();
      int index = cleanList.indexOf(t, 0);

      // t should fall into the right Vector
      Vector<T> left = cleanList.dropRight(cleanList.size() - index);
      Vector<T> right = cleanList.drop(index);

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
      return cleaned.get().list;
   }
}
