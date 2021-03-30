package io.github.theimbichner.taskmanager.io;

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

   private final Either<TaskAccessException, T> value;

   private TaskAccessResult(Either<TaskAccessException, T> value) {
      this.value = value;
   }

   public static <T> TaskAccessResult<T> of(ResultSupplier<T> supplier) {
      return new TaskAccessResult<>(supplier.asEither());
   }

   public static <T> TaskAccessResult<T> transaction(
      TaskStore taskStore,
      ResultSupplier<T> supplier
   ) {
      Either<TaskAccessException, T> either = supplier
         .asEither()
         .peekLeft(x -> taskStore.cancelTransaction())
         .flatMap(result -> taskStore.commit().map(x -> result));

      return new TaskAccessResult<>(either);
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

   public Either<TaskAccessException, T> asEither() {
      return value;
   }
}
