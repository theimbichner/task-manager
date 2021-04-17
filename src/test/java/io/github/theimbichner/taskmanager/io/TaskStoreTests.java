package io.github.theimbichner.taskmanager.io;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.Duration;
import java.util.Comparator;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.taskmanager.task.Generator;
import io.github.theimbichner.taskmanager.task.GeneratorMutator;
import io.github.theimbichner.taskmanager.task.GeneratorDelta;
import io.github.theimbichner.taskmanager.task.ItemId;
import io.github.theimbichner.taskmanager.task.Table;
import io.github.theimbichner.taskmanager.task.TableMutator;
import io.github.theimbichner.taskmanager.task.TableDelta;
import io.github.theimbichner.taskmanager.task.Task;
import io.github.theimbichner.taskmanager.task.TaskMutator;
import io.github.theimbichner.taskmanager.task.TaskDelta;
import io.github.theimbichner.taskmanager.task.TestComparators;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.task.property.Schema;
import io.github.theimbichner.taskmanager.time.DatePattern;
import io.github.theimbichner.taskmanager.time.UniformDatePattern;

import static org.assertj.core.api.Assertions.*;

public class TaskStoreTests {
   static final File TEST_ROOT = new File("./TaskStoreTests/");

   static TaskStore taskStore;

   @BeforeAll
   static void beforeAll() throws IOException {
      taskStore = TaskStore.getDefault(TEST_ROOT);
   }

   @AfterAll
   static void afterAll() throws IOException {
      IOUtils.deleteFolder(TEST_ROOT);
   }

   static Stream<Arguments> provideTaskGeneratorTable() throws TaskAccessException {
      TaskStore secondaryTaskStore = InMemoryDataStore.createTaskStore();

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

      Table table = TableMutator.createTable(secondaryTaskStore).get();
      TableMutator tableMutator = new TableMutator(secondaryTaskStore, table.getId());
      Table overwriteTable = tableMutator.modifyTable(tableDelta).get();

      Task task = TaskMutator.createTask(secondaryTaskStore, table.getId()).get();
      TaskMutator taskMutator = new TaskMutator(secondaryTaskStore, task.getId());
      Task overwriteTask = taskMutator.modifyAndSeverTask(taskDelta).get();

      String field = "";
      DatePattern datePattern = new UniformDatePattern(
         Instant.ofEpochSecond(0),
         Duration.ofSeconds(100));
      Generator generator = GeneratorMutator
         .createGenerator(secondaryTaskStore, table.getId(), field, datePattern)
         .get();
      GeneratorMutator generatorMutator = new GeneratorMutator(
         secondaryTaskStore,
         generator.getId());
      Generator overwriteGenerator = generatorMutator.modifyGenerator(generatorDelta).get();

      return Stream.of(
         Arguments.of(
            task,
            overwriteTask,
            taskStore.getTasks(),
            (Comparator<Task>) TestComparators::compareTasks,
            (Supplier<Task>) () -> {
               Table tempTable = TableMutator.createTable(secondaryTaskStore).asEither().get();
               return TaskMutator.createTask(secondaryTaskStore, tempTable.getId()).asEither().get();
            },
            TaskStore.MAXIMUM_TASKS_CACHED),
         Arguments.of(
            generator,
            overwriteGenerator,
            taskStore.getGenerators(),
            (Comparator<Generator>) TestComparators::compareGenerators,
            (Supplier<Generator>) () -> {
               Table tempTable = TableMutator.createTable(secondaryTaskStore).asEither().get();
               return GeneratorMutator
                  .createGenerator(secondaryTaskStore, tempTable.getId(), field, datePattern)
                  .asEither()
                  .get();
            },
            TaskStore.MAXIMUM_GENERATORS_CACHED),
         Arguments.of(
            table,
            overwriteTable,
            taskStore.getTables(),
            (Comparator<Table>) TestComparators::compareTables,
            (Supplier<Table>) () -> TableMutator.createTable(secondaryTaskStore).asEither().get(),
            TaskStore.MAXIMUM_TABLES_CACHED));
   }

   @ParameterizedTest
   @MethodSource("provideTaskGeneratorTable")
   <T extends Storable<ItemId<T>>> void testWriteDeleteOverwriteRead(
      T t,
      T overwrite,
      DataStore<ItemId<T>, T> dataStore,
      Comparator<T> comparator
   ) {
      dataStore.save(t).asEither().get();
      dataStore.deleteById(t.getId());
      dataStore.save(overwrite).asEither().get();
      T result = dataStore.getById(t.getId()).asEither().get();
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
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
      dataStore.save(t).asEither().get();
      uncache(dataStore, supplier, cacheSize);
      T result = dataStore.getById(t.getId()).asEither().get();
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
      dataStore.save(t).asEither().get();
      dataStore.save(overwrite).asEither().get();
      uncache(dataStore, supplier, cacheSize);
      T result = dataStore.getById(t.getId()).asEither().get();
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
      dataStore.save(t).asEither().get();
      uncache(dataStore, supplier, cacheSize);
      dataStore.save(overwrite).asEither().get();
      uncache(dataStore, supplier, cacheSize);
      T result = dataStore.getById(t.getId()).asEither().get();
      assertThat(result).usingComparator(comparator).isEqualTo(overwrite);
   }

   <T extends Storable<ItemId<T>>> void uncache(
      DataStore<ItemId<T>, T> dataStore,
      Supplier<T> supplier,
      int count
   ) {
      for (int i = 0; i <= count; i++) {
         dataStore.save(supplier.get()).asEither().get();
      }
   }
}
