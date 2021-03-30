package io.github.theimbichner.taskmanager.io;

import java.util.Comparator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.taskmanager.task.Orchestration;
import io.github.theimbichner.taskmanager.task.Table;
import io.github.theimbichner.taskmanager.task.TestComparators;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.vavr.api.VavrAssertions.*;

public class TaskAccessResultTests {
   private static Table table;
   private TaskStore taskStore;

   @BeforeAll
   static void beforeAll() {
      TaskStore taskStore = new TaskStore(new InMemoryDataStore<>(), 0, 0, 0);
      Orchestration orchestrator = new Orchestration(taskStore);
      table = orchestrator.createTable().get();
   }

   @BeforeEach
   void beforeEach() {
      taskStore = new TaskStore(new InMemoryDataStore<>(), 0, 0, 0);
   }

   @Test
   void testResult() {
      TaskAccessResult<String> result = TaskAccessResult.of(() -> "alpha");

      assertThat(result.asEither()).containsOnRight("alpha");
   }

   @Test
   void testFailResult() {
      TaskAccessResult<String> result = TaskAccessResult.of(() -> {
         throw new TaskAccessException(new RuntimeException());
      });

      assertThat(result.asEither()).isLeft();
   }

   @Test
   void testCheckError() {
      TaskAccessResult<String> result = TaskAccessResult.of(() -> "alpha");

      assertThatCode(() -> result.checkError())
         .doesNotThrowAnyException();
   }

   @Test
   void testFailCheckError() {
      TaskAccessResult<String> result = TaskAccessResult.of(() -> {
         throw new TaskAccessException(new RuntimeException());
      });

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
      TaskAccessResult<String> result = TaskAccessResult.of(() -> {
         throw new TaskAccessException(new RuntimeException());
      });

      assertThatExceptionOfType(TaskAccessException.class)
         .isThrownBy(() -> result.get());
   }

   @Test
   void testTransactionPass() {
      TaskAccessResult<String> result = TaskAccessResult.transaction(taskStore, () -> {
         taskStore.getTables().save(table).get();
         return "alpha";
      });

      assertThat(result.asEither()).containsOnRight("alpha");
      assertThat(taskStore.getTables().getById(table.getId()))
         .usingValueComparator((Comparator<Table>) TestComparators::compareTables)
         .containsOnRight(table);
   }

   @Test
   void testTransactionFail() {
      TaskAccessResult<String> result = TaskAccessResult.transaction(taskStore, () -> {
         taskStore.getTables().save(table).get();
         throw new TaskAccessException(new RuntimeException());
      });

      assertThat(result.asEither()).isLeft();
      assertThat(taskStore.getTables().getById(table.getId())).isLeft();
   }
}
