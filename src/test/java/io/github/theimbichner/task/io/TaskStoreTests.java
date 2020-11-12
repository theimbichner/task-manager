package io.github.theimbichner.task.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Duration;
import java.util.Comparator;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.task.Generator;
import io.github.theimbichner.task.Table;
import io.github.theimbichner.task.Task;
import io.github.theimbichner.task.time.DatePattern;
import io.github.theimbichner.task.time.UniformDatePattern;

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
      Table table = Table.createTable();
      Table overwriteTable = Table.fromJson(table.toJson());

      Task task = Task.createTask(table);
      Task overwriteTask = Task.fromJson(task.toJson());

      String field = "";
      DatePattern datePattern = new UniformDatePattern(
         Instant.ofEpochSecond(0),
         Duration.ofSeconds(100));
      Generator generator = Generator.createGenerator(table, field, datePattern);
      Generator overwriteGenerator = Generator.fromJson(generator.toJson());

      return Stream.of(
         Arguments.of(
            task,
            overwriteTask,
            taskStore.getTasks(),
            TASK_COMPARE,
            (Supplier<Task>) () -> Task.createTask(table),
            TaskStore.MAXIMUM_TASKS_CACHED),
         Arguments.of(
            generator,
            overwriteGenerator,
            taskStore.getGenerators(),
            GENERATOR_COMPARE,
            (Supplier<Generator>) () -> Generator.createGenerator(table, field, datePattern),
            TaskStore.MAXIMUM_GENERATORS_CACHED),
         Arguments.of(
            table,
            overwriteTable,
            taskStore.getTables(),
            TABLE_COMPARE,
            (Supplier<Table>) Table::createTable,
            TaskStore.MAXIMUM_TABLES_CACHED));
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testWriteRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator
   ) throws TaskAccessException {
      dataStore.save(t);
      T result = dataStore.getById(t.getId());
      assertThat(result).usingComparator(comparator).isEqualTo(t);
      assertThat(result.getTaskStore()).isEqualTo(taskStore);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testWriteOverwriteRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator
   ) throws TaskAccessException {
      dataStore.save(t);
      dataStore.save(overwrite);
      T result = dataStore.getById(t.getId());
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
      assertThat(result.getTaskStore()).isEqualTo(taskStore);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testWriteDeleteRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator
   ) throws TaskAccessException {
      dataStore.save(t);
      dataStore.deleteById(t.getId());
      assertThatExceptionOfType(TaskAccessException.class)
         .isThrownBy(() -> dataStore.getById(t.getId()));
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testWriteDeleteDelete(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator
   ) throws TaskAccessException {
      dataStore.save(t);
      dataStore.deleteById(t.getId());
      assertThatExceptionOfType(TaskAccessException.class)
         .isThrownBy(() -> dataStore.deleteById(t.getId()));
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testWriteDeleteOverwriteRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator
   ) throws TaskAccessException {
      dataStore.save(t);
      dataStore.deleteById(t.getId());
      dataStore.save(overwrite);
      T result = dataStore.getById(t.getId());
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
      assertThat(result.getTaskStore()).isEqualTo(taskStore);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testReadInvalid(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator
   ) throws TaskAccessException {
      assertThatExceptionOfType(TaskAccessException.class)
         .isThrownBy(() -> dataStore.getById(t.getId()));
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testDeleteInvalid(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator
   ) throws TaskAccessException {
      assertThatExceptionOfType(TaskAccessException.class)
         .isThrownBy(() -> dataStore.deleteById(t.getId()));
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testWriteUncacheRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier,
      int cacheSize
   ) throws TaskAccessException {
      dataStore.save(t);
      uncache(dataStore, supplier, cacheSize);
      T result = dataStore.getById(t.getId());
      assertThat(result).usingComparator(comparator).isEqualTo(t);
      assertThat(result.getTaskStore()).isEqualTo(taskStore);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testWriteOverwriteUncacheRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier,
      int cacheSize
   ) throws TaskAccessException {
      dataStore.save(t);
      dataStore.save(overwrite);
      uncache(dataStore, supplier, cacheSize);
      T result = dataStore.getById(t.getId());
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
      assertThat(result.getTaskStore()).isEqualTo(taskStore);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testWriteUncacheOverwriteUncacheRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier,
      int cacheSize
   ) throws TaskAccessException {
      dataStore.save(t);
      uncache(dataStore, supplier, cacheSize);
      dataStore.save(overwrite);
      uncache(dataStore, supplier, cacheSize);
      T result = dataStore.getById(t.getId());
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
      assertThat(result.getTaskStore()).isEqualTo(taskStore);
   }

   <T extends Storable> void uncache(
      DataStore<T> dataStore,
      Supplier<T> supplier,
      int count
   ) throws TaskAccessException {
      for (int i = 0; i < count; i++) {
         dataStore.save(supplier.get());
      }
   }
}
