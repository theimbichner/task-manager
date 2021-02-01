package io.github.theimbichner.taskmanager.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Duration;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.taskmanager.task.Generator;
import io.github.theimbichner.taskmanager.task.Orchestration;
import io.github.theimbichner.taskmanager.task.Table;
import io.github.theimbichner.taskmanager.task.Task;
import io.github.theimbichner.taskmanager.time.DatePattern;
import io.github.theimbichner.taskmanager.time.UniformDatePattern;

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

   // TODO actually modify the task/table/generators
   static Stream<Arguments> provideTaskGeneratorTable() {
      Table table = Orchestration.createTable(taskStore).get();
      Table overwriteTable = Table.fromJson(table.toJson());

      Task task = Orchestration.createTask(table).get();
      Task overwriteTask = Task.fromJson(task.toJson());

      String field = "";
      DatePattern datePattern = new UniformDatePattern(
         Instant.ofEpochSecond(0),
         Duration.ofSeconds(100));
      Generator generator = Orchestration.createGenerator(table, field, datePattern).get();
      Generator overwriteGenerator = Generator.fromJson(generator.toJson());

      return Stream.of(
         Arguments.of(
            task,
            overwriteTask,
            taskStore.getTasks(),
            TASK_COMPARE,
            (Supplier<Task>) () -> Orchestration.createTask(table).get(),
            TaskStore.MAXIMUM_TASKS_CACHED),
         Arguments.of(
            generator,
            overwriteGenerator,
            taskStore.getGenerators(),
            GENERATOR_COMPARE,
            (Supplier<Generator>) () -> Orchestration.createGenerator(
               table,
               field,
               datePattern).get(),
            TaskStore.MAXIMUM_GENERATORS_CACHED),
         Arguments.of(
            table,
            overwriteTable,
            taskStore.getTables(),
            TABLE_COMPARE,
            (Supplier<Table>) () -> Orchestration.createTable(taskStore).get(),
            TaskStore.MAXIMUM_TABLES_CACHED));
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testWriteRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator
   ) {
      dataStore.save(t).get();
      T result = dataStore.getById(t.getId()).get();
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
   ) {
      dataStore.save(t).get();
      dataStore.save(overwrite).get();
      T result = dataStore.getById(t.getId()).get();
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
   ) {
      dataStore.save(t).get();
      dataStore.deleteById(t.getId()).get();
      assertThat(dataStore.getById(t.getId()).getLeft())
         .isInstanceOf(TaskAccessException.class);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testWriteDeleteDelete(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator
   ) {
      dataStore.save(t).get();
      dataStore.deleteById(t.getId()).get();
      assertThat(dataStore.deleteById(t.getId()).getLeft())
         .isInstanceOf(TaskAccessException.class);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testWriteDeleteOverwriteRead(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator
   ) {
      dataStore.save(t).get();
      dataStore.deleteById(t.getId());
      dataStore.save(overwrite).get();
      T result = dataStore.getById(t.getId()).get();
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
   ) {
      assertThat(dataStore.getById(UUID.randomUUID().toString()).getLeft())
         .isInstanceOf(TaskAccessException.class);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable> void testDeleteInvalid(
      T t,
      T overwrite,
      DataStore<T> dataStore,
      Comparator<T> comparator
   ) {
      assertThat(dataStore.deleteById(t.getId()).getLeft())
         .isInstanceOf(TaskAccessException.class);
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
   ) {
      dataStore.save(t).get();
      uncache(dataStore, supplier, cacheSize);
      T result = dataStore.getById(t.getId()).get();
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
   ) {
      dataStore.save(t).get();
      dataStore.save(overwrite).get();
      uncache(dataStore, supplier, cacheSize);
      T result = dataStore.getById(t.getId()).get();
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
   ) {
      dataStore.save(t).get();
      uncache(dataStore, supplier, cacheSize);
      dataStore.save(overwrite).get();
      uncache(dataStore, supplier, cacheSize);
      T result = dataStore.getById(t.getId()).get();
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
      assertThat(result.getTaskStore()).isEqualTo(taskStore);
   }

   <T extends Storable> void uncache(
      DataStore<T> dataStore,
      Supplier<T> supplier,
      int count
   ) {
      for (int i = 0; i <= count; i++) {
         dataStore.save(supplier.get()).get();
      }
   }
}
