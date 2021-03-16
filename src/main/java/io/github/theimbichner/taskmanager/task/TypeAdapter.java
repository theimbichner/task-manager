package io.github.theimbichner.taskmanager.task;

import java.util.function.Function;

public class TypeAdapter<T, U> {
   private final Function<T, U> convert;
   private final Function<U, T> deconvert;

   public TypeAdapter(Function<T, U> convert, Function<U, T> deconvert) {
      this.convert = convert;
      this.deconvert = deconvert;
   }

   public U convert(T t) {
      return convert.apply(t);
   }

   public T deconvert(U u) {
      return deconvert.apply(u);
   }

   public static <T> TypeAdapter<T, T> identity() {
      return new TypeAdapter<>(x -> x, x -> x);
   }
}
