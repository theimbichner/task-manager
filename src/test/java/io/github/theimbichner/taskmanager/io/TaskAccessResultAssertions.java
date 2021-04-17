package io.github.theimbichner.taskmanager.io;

import org.assertj.vavr.api.EitherAssert;
import org.assertj.vavr.api.VavrAssertions;

public class TaskAccessResultAssertions {
   public static <T> EitherAssert<TaskAccessException, T> assertThat(TaskAccessResult<T> actual) {
      return VavrAssertions.assertThat(actual.asEither());
   }
}
