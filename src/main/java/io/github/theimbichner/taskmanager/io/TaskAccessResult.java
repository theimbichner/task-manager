package io.github.theimbichner.taskmanager.io;

import java.util.function.Consumer;

import io.vavr.control.Either;

public class TaskAccessResult<T> {
   public static interface ResultSupplier<T> {
      T call() throws TaskAccessException;

      default Either<TaskAccessException, T> asEither() {
         try {
            return Either.right(call());
         }
         catch (TaskAccessException e) {
            return Either.left(e);
         }
      }
   }

   public static interface ResultFunction<T, U> {
      U call(T t) throws TaskAccessException;

      default Either<TaskAccessException, U> asEither(T t) {
         try {
            return Either.right(call(t));
         }
         catch (TaskAccessException e) {
            return Either.left(e);
         }
      }
   }

   private final Either<TaskAccessException, T> value;

   private TaskAccessResult(Either<TaskAccessException, T> value) {
      this.value = value;
   }

   public static <T> TaskAccessResult<T> of(ResultSupplier<T> supplier) {
      return new TaskAccessResult<>(supplier.asEither());
   }

   public static <T> TaskAccessResult<T> ofRight(T t) {
      return new TaskAccessResult<>(Either.right(t));
   }

   public static <T> TaskAccessResult<T> ofLeft(TaskAccessException e) {
      return new TaskAccessResult<>(Either.left(e));
   }

   public static <T> TaskAccessResult<T> transaction(
      TaskStore taskStore,
      ResultSupplier<T> supplier
   ) {
      return of(supplier)
         .peekLeft(x -> taskStore.cancelTransaction())
         .andThen(result -> {
            taskStore.commit().checkError();
            return result;
         });
   }

   public void checkError() throws TaskAccessException {
      if (value.isLeft()) {
         throw value.getLeft();
      }
   }

   public T get() throws TaskAccessException {
      checkError();
      return value.get();
   }

   public static <T> T getEither(Either<TaskAccessException, T> either) throws TaskAccessException {
      return new TaskAccessResult<>(either).get();
   }

   public TaskAccessResult<T> peek(Consumer<T> consumer) {
      value.peek(consumer);
      return this;
   }

   public TaskAccessResult<T> peekLeft(Consumer<TaskAccessException> consumer) {
      value.peekLeft(consumer);
      return this;
   }

   public <U> TaskAccessResult<U> andThen(ResultFunction<T, U> function) {
      return new TaskAccessResult<>(value.flatMap(function::asEither));
   }

   public Either<TaskAccessException, T> asEither() {
      return value;
   }
}
