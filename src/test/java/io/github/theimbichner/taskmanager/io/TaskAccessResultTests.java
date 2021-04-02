package io.github.theimbichner.taskmanager.io;

import java.util.Comparator;

import io.vavr.control.Either;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.taskmanager.task.Table;
import io.github.theimbichner.taskmanager.task.TableMutator;
import io.github.theimbichner.taskmanager.task.TestComparators;

import static org.assertj.core.api.Assertions.*;

import static io.github.theimbichner.taskmanager.io.TaskAccessResultAssertions.*;

public class TaskAccessResultTests {
   private static Table table;
   private TaskStore taskStore;
   private TaskAccessException exception;
   private int counter;

   @BeforeAll
   static void beforeAll() throws TaskAccessException {
      TaskStore taskStore = new TaskStore(new InMemoryDataStore<>(), 0, 0, 0);
      table = TableMutator.createTable(taskStore).get();
   }

   @BeforeEach
   void beforeEach() {
      taskStore = new TaskStore(new InMemoryDataStore<>(), 0, 0, 0);
      exception = new TaskAccessException(new RuntimeException());
      counter = 0;
   }

   @Test
   void testOf() {
      assertThat(TaskAccessResult.of(() -> "alpha")).containsOnRight("alpha");
   }

   @Test
   void testFailOf() {
      assertThat(TaskAccessResult.of(this::errorSupplier)).isLeft();
   }

   @Test
   void testOfRight() {
      assertThat(TaskAccessResult.ofRight("alpha")).containsOnRight("alpha");
   }

   @Test
   void testOfLeft() {
      assertThat(TaskAccessResult.ofLeft(exception)).isLeft();
   }

   @Test
   void testCheckError() {
      TaskAccessResult<String> result = TaskAccessResult.of(() -> "alpha");

      assertThatCode(() -> result.checkError())
         .doesNotThrowAnyException();
   }

   @Test
   void testFailCheckError() {
      TaskAccessResult<String> result = TaskAccessResult.of(this::errorSupplier);

      assertThatExceptionOfType(TaskAccessException.class)
         .isThrownBy(() -> result.checkError());
   }

   @Test
   void testGet() throws TaskAccessException {
      TaskAccessResult<String> result = TaskAccessResult.of(() -> "alpha");

      assertThat(result.get()).isEqualTo("alpha");
   }

   @Test
   void testFailGet() {
      TaskAccessResult<String> result = TaskAccessResult.of(this::errorSupplier);

      assertThatExceptionOfType(TaskAccessException.class)
         .isThrownBy(() -> result.get());
   }

   @Test
   void testGetEither() throws TaskAccessException{
      String result = TaskAccessResult.getEither(Either.right("alpha"));
      assertThat(result).isEqualTo("alpha");
   }

   @Test
   void testFailGetEither() {
      assertThatExceptionOfType(TaskAccessException.class)
         .isThrownBy(() -> TaskAccessResult.getEither(Either.left(exception)));
   }

   @Test
   void testPeek() {
      TaskAccessResult<String> result = TaskAccessResult.ofRight("alpha");
      result.peek(x -> counter++);

      assertThat(counter).isEqualTo(1);
   }

   @Test
   void testFailPeek() {
      TaskAccessResult<String> result = TaskAccessResult.ofLeft(exception);

      result.peek(x -> fail("peek() on left called consumer"));
   }

   @Test
   void testPeekLeft() {
      TaskAccessResult<String> result = TaskAccessResult.ofRight("alpha");

      result.peekLeft(x -> fail("peekLeft() on right called consumer"));
   }

   @Test
   void testFailPeekLeft() {
      TaskAccessResult<String> result = TaskAccessResult.ofLeft(exception);
      result.peekLeft(x -> counter++);

      assertThat(counter).isEqualTo(1);
   }

   @Test
   void testAndThen() {
      TaskAccessResult<String> right = TaskAccessResult.ofRight("alpha");

      assertThat(right.andThen(String::toUpperCase)).containsOnRight("ALPHA");
   }

   @Test
   void testFailAndThen() {
      TaskAccessResult<String> left = TaskAccessResult.ofLeft(exception);

      assertThat(left.andThen(String::toUpperCase)).isLeft();
   }

   @Test
   void testAndThenException() {
      TaskAccessResult<String> right = TaskAccessResult.ofRight("alpha");

      assertThat(right.andThen(this::errorFunction)).isLeft();
   }

   @Test
   void testFailAndThenException() {
      TaskAccessResult<String> left = TaskAccessResult.ofLeft(exception);

      assertThat(left.andThen(this::errorFunction)).isLeft();
   }

   @Test
   void testTransactionPass() {
      TaskAccessResult<String> result = TaskAccessResult.transaction(taskStore, () -> {
         taskStore.getTables().save(table).get();
         return "alpha";
      });

      assertThat(result).containsOnRight("alpha");
      assertThat(taskStore.getTables().getById(table.getId()))
         .usingValueComparator((Comparator<Table>) TestComparators::compareTables)
         .containsOnRight(table);
   }

   @Test
   void testTransactionFail() {
      TaskAccessResult<String> result = TaskAccessResult.transaction(taskStore, () -> {
         taskStore.getTables().save(table).get();
         throw exception;
      });

      assertThat(result).isLeft();
      assertThat(taskStore.getTables().getById(table.getId())).isLeft();
   }

   private String errorSupplier() throws TaskAccessException {
      throw exception;
   }

   private String errorFunction(String input) throws TaskAccessException {
      throw exception;
   }
}
