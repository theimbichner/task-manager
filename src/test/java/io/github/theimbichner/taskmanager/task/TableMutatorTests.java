package io.github.theimbichner.taskmanager.task;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import io.vavr.collection.Vector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.taskmanager.collection.SetList;
import io.github.theimbichner.taskmanager.io.InMemoryDataStore;
import io.github.theimbichner.taskmanager.io.TaskAccessException;
import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.task.property.Schema;
import io.github.theimbichner.taskmanager.task.property.TypeDescriptor;
import io.github.theimbichner.taskmanager.time.DatePattern;
import io.github.theimbichner.taskmanager.time.DateTime;
import io.github.theimbichner.taskmanager.time.UniformDatePattern;

import static org.assertj.core.api.Assertions.*;

public class TableMutatorTests {
   private TaskStore taskStore;

   private ItemId<Table> tableId;
   private TableMutator tableMutator;
   private ItemId<Generator> generatorId;
   private SetList<ItemId<Task>> generatedTaskIds;
   private SetList<ItemId<Task>> allTaskIds;

   private Instant patternStart;
   private Duration patternStep;
   private DatePattern pattern;

   private Instant lastGenerationTimestamp;

   private TableDelta tableDelta;
   private TaskDelta taskDelta;

   @BeforeEach
   void beforeEach() throws TaskAccessException {
      taskStore = InMemoryDataStore.createTaskStore();
      Orchestration orchestrator = new Orchestration(taskStore);

      patternStart = LocalDate.now(ZoneOffset.UTC)
         .plusDays(2)
         .atStartOfDay(ZoneOffset.UTC)
         .toInstant();
      patternStep = Duration.parse("PT17M36.5S");
      pattern = new UniformDatePattern(patternStart, patternStep);

      tableId = TableMutator.createTable(taskStore).get().getId();
      tableMutator = new TableMutator(taskStore, tableId);

      TableDelta dataTableDelta = new TableDelta(
         Schema.empty()
            .withColumn("alpha", TypeDescriptor.fromTypeName("DateTime"))
            .withColumn("beta", TypeDescriptor.fromTypeName("String")),
         null);
      tableMutator.modifyTable(dataTableDelta).checkError();

      ItemId<Task> taskId = orchestrator.createTask(tableId).get().getId();
      generatorId = orchestrator.createGenerator(
         tableId,
         "alpha",
         pattern).get().getId();

      lastGenerationTimestamp = patternStart.plus(Duration.parse("PT45M"));
      allTaskIds = tableMutator.getTasksFromTable(lastGenerationTimestamp).get();
      generatedTaskIds = allTaskIds.remove(taskId);

      tableDelta = new TableDelta(
         Schema.empty().withoutColumn("beta"),
         null);
      taskDelta = new TaskDelta(
         PropertyMap.empty().put("beta", Property.DELETE),
         null,
         null);
   }
   @Test
   void testCreateTable() throws TaskAccessException {
      Table result = TableMutator.createTable(taskStore).get();

      assertThat(result)
         .usingComparator(TestComparators::compareTablesIgnoringId)
         .isEqualTo(Table.newTable());
   }

   @Test
   void testCreateTableIsSaved() throws TaskAccessException {
      Table table = TableMutator.createTable(taskStore).get();

      taskStore.cancelTransaction();
      Table savedTable = getTable(table.getId());

      assertThat(table)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(savedTable);
   }

   @Test
   void testModifyEmptyTable() throws TaskAccessException {
      Table table = TableMutator.createTable(taskStore).get();
      TableDelta delta = new TableDelta(
         Schema.empty()
            .withColumn("alpha", TypeDescriptor.fromTypeName("DateTime"))
            .withColumn("beta", TypeDescriptor.fromTypeName("String")),
         "Renamed table");

      Table result = tableMutator.modifyTable(delta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareTablesIgnoringId)
         .isEqualTo(table.withModification(delta));
   }

   @Test
   void testModifyEmptyTableIsSaved() throws TaskAccessException {
      Table table = TableMutator.createTable(taskStore).get();
      TableDelta delta = new TableDelta(
         Schema.empty()
            .withColumn("alpha", TypeDescriptor.fromTypeName("DateTime"))
            .withColumn("beta", TypeDescriptor.fromTypeName("String")),
         "Renamed table");

      table = tableMutator.modifyTable(delta).get();

      taskStore.cancelTransaction();
      Table savedTable = getTable(table.getId());

      assertThat(table)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(savedTable);
   }

   @Test
   void testGetTasksFromTableTimestamps() {
      Vector<Instant> expectedStartTimes = Vector.of(
         patternStart,
         patternStart.plus(patternStep),
         patternStart.plus(patternStep.multipliedBy(2)));

      taskStore.cancelTransaction();
      Vector<Instant> actualStartTimes = generatedTaskIds
         .asList()
         .map(this::getTask)
         .map(task -> task.getProperties().asMap().get("alpha").get())
         .map(property -> ((DateTime) property.get()).getStart());

      assertThat(actualStartTimes)
         .containsExactlyInAnyOrderElementsOf(expectedStartTimes);
   }

   @Test
   void testGetTasksFromTableTasksAreSaved() {
      taskStore.cancelTransaction();
      Generator generator = getGenerator(generatorId);

      for (ItemId<Task> id : generatedTaskIds.asList()) {
         Task task = getTask(id);
         Property timestamp = task.getProperties().asMap().get("alpha").get();
         Instant start = ((DateTime) timestamp.get()).getStart();

         Task expectedTask = Task.newSeriesTask(generator, start);
         assertThat(task)
            .usingComparator(TestComparators::compareTasksIgnoringId)
            .isEqualTo(expectedTask);
      }
   }

   @Test
   void testGetTasksFromTableGeneratorIsSaved() {
      taskStore.cancelTransaction();
      Generator savedGenerator = getGenerator(generatorId);

      assertThat(savedGenerator.getTaskIds().asList())
         .containsExactlyInAnyOrderElementsOf(generatedTaskIds.asList());
   }

   @Test
   void testGetTasksFromTableGeneratorTimestampIsSaved() throws TaskAccessException {
      taskStore.cancelTransaction();
      SetList<ItemId<Task>> result = tableMutator.getTasksFromTable(lastGenerationTimestamp).get();

      assertThat(result.asList())
         .containsExactlyInAnyOrderElementsOf(allTaskIds.asList());
   }

   @Test
   void testGetTasksFromTableTableIsSaved() {
      taskStore.cancelTransaction();
      Table savedTable = getTable(tableId);

      assertThat(savedTable.getAllTaskIds().asList())
         .containsExactlyInAnyOrderElementsOf(allTaskIds.asList());
   }

   @Test
   void testModifyTable() throws TaskAccessException {
      Table table = getTable(tableId);
      Table expectedTable = table.withModification(tableDelta);

      Table result = tableMutator.modifyTable(tableDelta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareTablesIgnoringId)
         .isEqualTo(expectedTable);
   }

   @Test
   void testModifyTableIsSaved() throws TaskAccessException {
      Table table = tableMutator.modifyTable(tableDelta).get();

      taskStore.cancelTransaction();
      Table savedTable = getTable(tableId);

      assertThat(table)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(savedTable);
   }

   @Test
   void testModifyTableGeneratorIsSaved() throws TaskAccessException {
      Generator generator = getGenerator(generatorId);

      tableMutator.modifyTable(tableDelta).checkError();

      taskStore.cancelTransaction();
      Generator savedGenerator = getGenerator(generatorId);

      assertThat(savedGenerator)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(generator.adjustToSchema(tableDelta.getSchema()));
   }

   @Test
   void testModifyTableTasksAreSaved() throws TaskAccessException {
      Vector<Task> expectedTasks = allTaskIds
         .asList()
         .map(this::getTask)
         .map(task -> task.withModification(taskDelta));

      tableMutator.modifyTable(tableDelta).checkError();

      taskStore.cancelTransaction();
      Vector<Task> actualTasks = allTaskIds.asList().map(this::getTask);

      assertThat(actualTasks)
         .usingElementComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(expectedTasks);
   }

   private Table getTable(ItemId<Table> id) {
      return taskStore.getTables().getById(id).asEither().get();
   }

   private Task getTask(ItemId<Task> id) {
      return taskStore.getTasks().getById(id).asEither().get();
   }

   private Generator getGenerator(ItemId<Generator> id) {
      return taskStore.getGenerators().getById(id).asEither().get();
   }
}
