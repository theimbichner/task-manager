package io.github.theimbichner.taskmanager.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Duration;
import java.util.Comparator;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.taskmanager.task.Generator;
import io.github.theimbichner.taskmanager.task.GeneratorDelta;
import io.github.theimbichner.taskmanager.task.ItemId;
import io.github.theimbichner.taskmanager.task.Orchestration;
import io.github.theimbichner.taskmanager.task.Table;
import io.github.theimbichner.taskmanager.task.TableDelta;
import io.github.theimbichner.taskmanager.task.Task;
import io.github.theimbichner.taskmanager.task.TaskDelta;
import io.github.theimbichner.taskmanager.task.TestComparators;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.task.property.Schema;
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

   static Stream<Arguments> provideTaskGeneratorTable() {
      Orchestration orchestrator = new Orchestration(taskStore);

      TableDelta tableDelta = new TableDelta(Schema.empty(), "modified");
      TaskDelta taskDelta = new TaskDelta(
         PropertyMap.empty(),
         "modified",
         null);
      GeneratorDelta generatorDelta = new GeneratorDelta(
         PropertyMap.empty(),
         "modified",
         null,
         null,
         null);

      Table table = orchestrator.createTable().get();
      Table overwriteTable = orchestrator.modifyTable(table.getId(), tableDelta).get();

      Task task = orchestrator.createTask(table.getId()).get();
      Task overwriteTask = orchestrator.modifyAndSeverTask(task.getId(), taskDelta).get();

      String field = "";
      DatePattern datePattern = new UniformDatePattern(
         Instant.ofEpochSecond(0),
         Duration.ofSeconds(100));
      Generator generator = orchestrator
         .createGenerator(table.getId(), field, datePattern)
         .get();
      Generator overwriteGenerator = orchestrator
         .modifyGenerator(generator.getId(), generatorDelta)
         .get();

      return Stream.of(
         Arguments.of(
            task,
            overwriteTask,
            taskStore.getTasks(),
            (Comparator<Task>) TestComparators::compareTasks,
            (Supplier<Task>) () -> {
               Table tempTable = orchestrator.createTable().get();
               return orchestrator.createTask(tempTable.getId()).get();
            },
            TaskStore.MAXIMUM_TASKS_CACHED),
         Arguments.of(
            generator,
            overwriteGenerator,
            taskStore.getGenerators(),
            (Comparator<Generator>) TestComparators::compareGenerators,
            (Supplier<Generator>) () -> {
               Table tempTable = orchestrator.createTable().get();
               return orchestrator
                  .createGenerator(tempTable.getId(), field, datePattern)
                  .get();
            },
            TaskStore.MAXIMUM_GENERATORS_CACHED),
         Arguments.of(
            table,
            overwriteTable,
            taskStore.getTables(),
            (Comparator<Table>) TestComparators::compareTables,
            (Supplier<Table>) () -> orchestrator.createTable().get(),
            TaskStore.MAXIMUM_TABLES_CACHED));
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable<ItemId<T>>> void testWriteRead(
      T t,
      T overwrite,
      DataStore<ItemId<T>, T> dataStore,
      Comparator<T> comparator
   ) {
      dataStore.save(t).get();
      T result = dataStore.getById(t.getId()).get();
      assertThat(result).usingComparator(comparator).isEqualTo(t);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable<ItemId<T>>> void testWriteOverwriteRead(
      T t,
      T overwrite,
      DataStore<ItemId<T>, T> dataStore,
      Comparator<T> comparator
   ) {
      dataStore.save(t).get();
      dataStore.save(overwrite).get();
      T result = dataStore.getById(t.getId()).get();
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable<ItemId<T>>> void testWriteDeleteRead(
      T t,
      T overwrite,
      DataStore<ItemId<T>, T> dataStore,
      Comparator<T> comparator
   ) {
      dataStore.save(t).get();
      dataStore.deleteById(t.getId()).get();
      assertThat(dataStore.getById(t.getId()).getLeft())
         .isInstanceOf(TaskAccessException.class);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable<ItemId<T>>> void testWriteDeleteDelete(
      T t,
      T overwrite,
      DataStore<ItemId<T>, T> dataStore,
      Comparator<T> comparator
   ) {
      dataStore.save(t).get();
      dataStore.deleteById(t.getId()).get();
      assertThat(dataStore.deleteById(t.getId()).getLeft())
         .isInstanceOf(TaskAccessException.class);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable<ItemId<T>>> void testWriteDeleteOverwriteRead(
      T t,
      T overwrite,
      DataStore<ItemId<T>, T> dataStore,
      Comparator<T> comparator
   ) {
      dataStore.save(t).get();
      dataStore.deleteById(t.getId());
      dataStore.save(overwrite).get();
      T result = dataStore.getById(t.getId()).get();
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable<ItemId<T>>> void testReadInvalid(
      T t,
      T overwrite,
      DataStore<ItemId<T>, T> dataStore,
      Comparator<T> comparator
   ) {
      assertThat(dataStore.getById(ItemId.randomId()).getLeft())
         .isInstanceOf(TaskAccessException.class);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable<ItemId<T>>> void testDeleteInvalid(
      T t,
      T overwrite,
      DataStore<ItemId<T>, T> dataStore,
      Comparator<T> comparator
   ) {
      assertThat(dataStore.deleteById(t.getId()).getLeft())
         .isInstanceOf(TaskAccessException.class);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable<ItemId<T>>> void testWriteUncacheRead(
      T t,
      T overwrite,
      DataStore<ItemId<T>, T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier,
      int cacheSize
   ) {
      dataStore.save(t).get();
      uncache(dataStore, supplier, cacheSize);
      T result = dataStore.getById(t.getId()).get();
      assertThat(result).usingComparator(comparator).isEqualTo(t);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable<ItemId<T>>> void testWriteOverwriteUncacheRead(
      T t,
      T overwrite,
      DataStore<ItemId<T>, T> dataStore,
      Comparator<T> comparator,
      Supplier<T> supplier,
      int cacheSize
   ) {
      dataStore.save(t).get();
      dataStore.save(overwrite).get();
      uncache(dataStore, supplier, cacheSize);
      T result = dataStore.getById(t.getId()).get();
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable<ItemId<T>>> void testWriteUncacheOverwriteUncacheRead(
      T t,
      T overwrite,
      DataStore<ItemId<T>, T> dataStore,
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
   }

   <T extends Storable<ItemId<T>>> void uncache(
      DataStore<ItemId<T>, T> dataStore,
      Supplier<T> supplier,
      int count
   ) {
      for (int i = 0; i <= count; i++) {
         dataStore.save(supplier.get()).get();
      }
   }
}
