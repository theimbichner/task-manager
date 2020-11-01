package io.github.theimbichner.task.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.task.Generator;
import io.github.theimbichner.task.Table;
import io.github.theimbichner.task.Task;

import static org.assertj.core.api.Assertions.*;

public class TaskStoreTests {
   static final File TEST_ROOT = new File("./TaskStoreTests/");
   static final Comparator<Task> TASK_COMPARE = (x, y) -> {
      return x.toJson().similar(y.toJson()) ? 0 : 1;
   };
   static final Comparator<Generator> GENERATOR_COMPARE = (x, y) -> {
      return x.toJson().similar(y.toJson()) ? 0 : 1;
   };
   static final Comparator<Table> TABLE_COMPARE = (x, y) -> {
      return x.toJson().similar(y.toJson()) ? 0 : 1;
   };

   static TaskStore taskStore;

   @BeforeAll
   static void beforeAll() {
      taskStore = TaskStore.getDefault(TEST_ROOT);
   }

   @BeforeEach
   void beforeEach() throws IOException {
      assertThat(deleteRecursive(TEST_ROOT)).isTrue();
   }

   @AfterAll
   static void afterAll() throws IOException {
      assertThat(deleteRecursive(TEST_ROOT)).isTrue();
   }

   static boolean deleteRecursive(File file) throws IOException {
      if (!file.exists()) {
         return true;
      }
      return Files.walk(file.toPath())
         .sorted(Comparator.reverseOrder())
         .map(Path::toFile)
         .filter(f -> !f.delete())
         .count() == 0;
   }

   static Stream<Arguments> provideTaskGeneratorTable() {
      Task task = Task.createTask();
      Task overwriteTask = Task.fromJson(task.toJson());
      Generator generator = Generator.createGenerator();
      Generator overwriteGenerator = Generator.fromJson(generator.toJson());
      Table table = Table.createTable();
      Table overwriteTable = Table.fromJson(table.toJson());

      return Stream.of(
         Arguments.of(
            task,
            overwriteTask,
            taskStore.getTasks(),
            TASK_COMPARE,
            (Supplier<Task>) Task::createTask),
         Arguments.of(
            generator,
            overwriteGenerator,
            taskStore.getGenerators(),
            GENERATOR_COMPARE,
            (Supplier<Generator>) Generator::createGenerator),
         Arguments.of(
            table,
            overwriteTable,
            taskStore.getTables(),
            TABLE_COMPARE,
            (Supplier<Table>) Table::createTable));
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T> void testWriteRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier
   ) throws TaskAccessException {
      dataStore.save(t);
      T result = dataStore.getById(dataStore.getId(t));
      assertThat(result).usingComparator(comparator).isEqualTo(t);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T> void testWriteOverwriteRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier
   ) throws TaskAccessException {
      dataStore.save(t);
      dataStore.save(overwrite);
      T result = dataStore.getById(dataStore.getId(t));
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T> void testWriteDeleteRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier
   ) throws TaskAccessException {
      dataStore.save(t);
      dataStore.deleteById(dataStore.getId(t));
      assertThatExceptionOfType(TaskAccessException.class)
         .isThrownBy(() -> dataStore.getById(dataStore.getId(t)));
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T> void testWriteDeleteDelete(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier
   ) throws TaskAccessException {
      dataStore.save(t);
      dataStore.deleteById(dataStore.getId(t));
      assertThatExceptionOfType(TaskAccessException.class)
         .isThrownBy(() -> dataStore.deleteById(dataStore.getId(t)));
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T> void testWriteDeleteOverwriteRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier
   ) throws TaskAccessException {
      dataStore.save(t);
      dataStore.deleteById(dataStore.getId(t));
      dataStore.save(overwrite);
      T result = dataStore.getById(dataStore.getId(t));
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T> void testReadInvalid(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier
   ) throws TaskAccessException {
      assertThatExceptionOfType(TaskAccessException.class)
         .isThrownBy(() -> dataStore.getById(dataStore.getId(t)));
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T> void testDeleteInvalid(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier
   ) throws TaskAccessException {
      assertThatExceptionOfType(TaskAccessException.class)
         .isThrownBy(() -> dataStore.deleteById(dataStore.getId(t)));
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T> void testWriteUncacheRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier
   ) throws TaskAccessException {
      dataStore.save(t);
      uncache(dataStore, supplier);
      T result = dataStore.getById(dataStore.getId(t));
      assertThat(result).usingComparator(comparator).isEqualTo(t);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T> void testWriteOverwriteUncacheRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier
   ) throws TaskAccessException {
      dataStore.save(t);
      dataStore.save(overwrite);
      uncache(dataStore, supplier);
      T result = dataStore.getById(dataStore.getId(t));
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T> void testWriteUncacheOverwriteUncacheRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier
   ) throws TaskAccessException {
      dataStore.save(t);
      uncache(dataStore, supplier);
      dataStore.save(overwrite);
      uncache(dataStore, supplier);
      T result = dataStore.getById(dataStore.getId(t));
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
   }

   <T> void uncache(DataStore<T> dataStore, Supplier<T> supplier) throws TaskAccessException {
      int count = IntStream.of(
         TaskStore.MAXIMUM_TASKS_CACHED,
         TaskStore.MAXIMUM_GENERATORS_CACHED,
         TaskStore.MAXIMUM_TABLES_CACHED).max().getAsInt();
      for (int i = 0; i < count; i++) {
         dataStore.save(supplier.get());
      }
   }

   @Test
   void testGetTaskFromTable() throws TaskAccessException {
      TaskStore taskStore = TaskStore.getDefault(TEST_ROOT);
      Table table = Table.createTable();
      taskStore.getTables().save(table);
      table = taskStore.getTables().getById(table.getId());

      Task task = Task.createTask();
      taskStore.getTasks().save(task);
      Task newTask = table.getTaskById(task.getId());

      assertThat(newTask).usingComparator(TASK_COMPARE).isEqualTo(task);
   }

   @Test
   void testGetGeneratorFromTable() throws TaskAccessException {
      TaskStore taskStore = TaskStore.getDefault(TEST_ROOT);
      Table table = Table.createTable();
      taskStore.getTables().save(table);
      table = taskStore.getTables().getById(table.getId());

      Generator generator = Generator.createGenerator();
      taskStore.getGenerators().save(generator);
      Generator newGenerator = table.getGeneratorById(generator.getId());

      assertThat(newGenerator).usingComparator(GENERATOR_COMPARE).isEqualTo(generator);
   }
}
