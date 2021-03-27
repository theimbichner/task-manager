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
import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.task.property.Property;
import io.github.theimbichner.taskmanager.task.property.PropertyMap;
import io.github.theimbichner.taskmanager.task.property.Schema;
import io.github.theimbichner.taskmanager.task.property.TypeDescriptor;
import io.github.theimbichner.taskmanager.time.DatePattern;
import io.github.theimbichner.taskmanager.time.DateTime;
import io.github.theimbichner.taskmanager.time.UniformDatePattern;

import static org.assertj.core.api.Assertions.*;

public class OrchestrationTests {
   private TaskStore taskStore;
   private Orchestration orchestrator;

   private ItemId<Table> dataTableId;
   private ItemId<Task> dataTaskId;
   private ItemId<Generator> dataGeneratorId;
   private SetList<ItemId<Task>> generatedTaskIds;
   private SetList<ItemId<Task>> allTaskIds;
   private ItemId<Task> priorTaskId;
   private ItemId<Task> middleTaskId;
   private ItemId<Task> subsequentTaskId;

   private Instant patternStart;
   private Duration patternStep;
   private DatePattern pattern;

   private Instant lastGenerationTimestamp;

   private TableDelta tableDelta;
   private TaskDelta taskDelta;
   private GeneratorDelta generatorDelta;

   @BeforeEach
   void beforeEach() {
      taskStore = InMemoryDataStore.createTaskStore();
      orchestrator = new Orchestration(taskStore);

      patternStart = LocalDate.now(ZoneOffset.UTC)
         .plusDays(2)
         .atStartOfDay(ZoneOffset.UTC)
         .toInstant();
      patternStep = Duration.parse("PT17M36.5S");
      pattern = new UniformDatePattern(patternStart, patternStep);

      dataTableId = orchestrator.createTable().get().getId();

      TableDelta dataTableDelta = new TableDelta(
         Schema.empty()
            .withColumn("alpha", TypeDescriptor.fromTypeName("DateTime"))
            .withColumn("beta", TypeDescriptor.fromTypeName("String")),
         null);
      orchestrator.modifyTable(dataTableId, dataTableDelta);

      dataTaskId = orchestrator.createTask(dataTableId).get().getId();
      dataGeneratorId = orchestrator.createGenerator(
         dataTableId,
         "alpha",
         pattern).get().getId();

      lastGenerationTimestamp = patternStart.plus(Duration.parse("PT45M"));
      allTaskIds = orchestrator.getTasksFromTable(
         dataTableId,
         lastGenerationTimestamp).get();
      generatedTaskIds = allTaskIds.remove(dataTaskId);

      priorTaskId = getGeneratedTaskId(patternStart);
      middleTaskId = getGeneratedTaskId(patternStart.plus(patternStep));
      subsequentTaskId = getGeneratedTaskId(
         patternStart.plus(patternStep.multipliedBy(2)));

      tableDelta = new TableDelta(
         Schema.empty().withoutColumn("beta"),
         null);
      taskDelta = new TaskDelta(
         PropertyMap.empty().put("beta", Property.DELETE),
         null,
         null);
      generatorDelta = new GeneratorDelta(
         PropertyMap.empty().put("beta", Property.DELETE),
         null,
         null,
         null,
         null);
   }

   @Test
   void testCreateTable() {
      Table result = orchestrator.createTable().get();

      assertThat(result)
         .usingComparator(TestComparators::compareTablesIgnoringId)
         .isEqualTo(Table.newTable());
   }

   @Test
   void testCreateTableIsSaved() {
      Table table = orchestrator.createTable().get();

      taskStore.cancelTransaction();
      Table savedTable = taskStore.getTables().getById(table.getId()).get();

      assertThat(table)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(savedTable);
   }

   @Test
   void testModifyEmptyTable() {
      Table table = orchestrator.createTable().get();
      TableDelta delta = new TableDelta(
         Schema.empty()
            .withColumn("alpha", TypeDescriptor.fromTypeName("DateTime"))
            .withColumn("beta", TypeDescriptor.fromTypeName("String")),
         "Renamed table");

      Table result = orchestrator.modifyTable(table.getId(), delta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareTablesIgnoringId)
         .isEqualTo(table.withModification(delta));
   }

   @Test
   void testModifyEmptyTableIsSaved() {
      Table table = orchestrator.createTable().get();
      TableDelta delta = new TableDelta(
         Schema.empty()
            .withColumn("alpha", TypeDescriptor.fromTypeName("DateTime"))
            .withColumn("beta", TypeDescriptor.fromTypeName("String")),
         "Renamed table");

      table = orchestrator.modifyTable(table.getId(), delta).get();

      taskStore.cancelTransaction();
      Table savedTable = taskStore.getTables().getById(table.getId()).get();

      assertThat(table)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(savedTable);
   }

   @Test
   void testCreateTask() {
      Table table = getTable(dataTableId);

      Task result = orchestrator.createTask(dataTableId).get();
      assertThat(result)
         .usingComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(Task.newTask(table));
   }

   @Test
   void testCreateTaskResultIsSaved() {
      Task result = orchestrator.createTask(dataTableId).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(result.getId());

      assertThat(result)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(savedTask);
   }

   @Test
   void testCreateTaskTableIsSaved() {
      Table table = getTable(dataTableId);

      Task task = orchestrator.createTask(dataTableId).get();
      Table expectedTable = table.withTasks(Vector.of(task.getId()));

      taskStore.cancelTransaction();
      Table savedTable = getTable(dataTableId);

      assertThat(savedTable)
         .usingComparator(TestComparators::compareTablesIgnoringId)
         .isEqualTo(expectedTable);
   }

   @Test
   void testCreateGenerator() {
      Table table = getTable(dataTableId);

      Generator result = orchestrator.createGenerator(
         dataTableId,
         "alpha",
         pattern).get();
      assertThat(result)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(Generator.newGenerator(table, "alpha", pattern));
   }

   @Test
   void testCreateGeneratorResultIsSaved() {
      Generator generator = orchestrator.createGenerator(
         dataTableId,
         "alpha",
         pattern).get();

      taskStore.cancelTransaction();
      Generator savedGenerator = getGenerator(generator.getId());

      assertThat(generator)
         .usingComparator(TestComparators::compareGenerators)
         .isEqualTo(savedGenerator);
   }

   @Test
   void testCreateGeneratorTableIsSaved() {
      Table table = getTable(dataTableId);

      Generator generator = orchestrator.createGenerator(
         dataTableId,
         "alpha",
         pattern).get();
      Table expectedTable = table.withGenerator(generator.getId());

      taskStore.cancelTransaction();
      Table savedTable = getTable(dataTableId);

      assertThat(savedTable)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(expectedTable);
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
      Generator generator = getGenerator(dataGeneratorId);

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
      Generator savedGenerator = getGenerator(dataGeneratorId);

      assertThat(savedGenerator.getTaskIds().asList())
         .containsExactlyInAnyOrderElementsOf(generatedTaskIds.asList());
   }

   @Test
   void testGetTasksFromTableGeneratorTimestampIsSaved() {
      taskStore.cancelTransaction();
      SetList<ItemId<Task>> result = orchestrator.getTasksFromTable(
         dataTableId,
         lastGenerationTimestamp).get();

      assertThat(result.asList())
         .containsExactlyInAnyOrderElementsOf(allTaskIds.asList());
   }

   @Test
   void testGetTasksFromTableTableIsSaved() {
      taskStore.cancelTransaction();
      Table savedTable = getTable(dataTableId);

      assertThat(savedTable.getAllTaskIds().asList())
         .containsExactlyInAnyOrderElementsOf(allTaskIds.asList());
   }

   @Test
   void testModifyTable() {
      Table table = getTable(dataTableId);
      Table expectedTable = table.withModification(tableDelta);

      Table result = orchestrator.modifyTable(dataTableId, tableDelta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareTablesIgnoringId)
         .isEqualTo(expectedTable);
   }

   @Test
   void testModifyTableTableIsSaved() {
      Table table = orchestrator.modifyTable(dataTableId, tableDelta).get();

      taskStore.cancelTransaction();
      Table savedTable = getTable(dataTableId);

      assertThat(table)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(savedTable);
   }

   @Test
   void testModifyTableGeneratorIsSaved() {
      Generator generator = getGenerator(dataGeneratorId);

      orchestrator.modifyTable(dataTableId, tableDelta).get();

      taskStore.cancelTransaction();
      Generator savedGenerator = getGenerator(dataGeneratorId);

      assertThat(savedGenerator)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(generator.adjustToSchema(tableDelta.getSchema()));
   }

   @Test
   void testModifyTableTasksAreSaved() {
      Vector<Task> expectedTasks = allTaskIds
         .asList()
         .map(this::getTask)
         .map(task -> task.withModification(taskDelta));

      orchestrator.modifyTable(dataTableId, tableDelta).get();

      taskStore.cancelTransaction();
      Vector<Task> actualTasks = allTaskIds.asList().map(this::getTask);

      assertThat(actualTasks)
         .usingElementComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(expectedTasks);
   }

   @Test
   void testModifyGenerator() {
      Generator generator = getGenerator(dataGeneratorId);

      Generator result = orchestrator.modifyGenerator(
         dataGeneratorId,
         generatorDelta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(generator.withModification(generatorDelta));
   }

   @Test
   void testModifyGeneratorGeneratorIsSaved() {
      Generator generator = orchestrator.modifyGenerator(
         dataGeneratorId,
         generatorDelta).get();

      taskStore.cancelTransaction();
      Generator savedGenerator = getGenerator(dataGeneratorId);

      assertThat(generator)
         .usingComparator(TestComparators::compareGenerators)
         .isEqualTo(savedGenerator);
   }

   @Test
   void testModifyGeneratorTasksAreSaved() {
      Generator generator = getGenerator(dataGeneratorId);
      Vector<Task> expectedTasks = generatedTaskIds
         .asList()
         .map(this::getTask)
         .map(task -> task.withSeriesModification(generatorDelta, generator));

      orchestrator.modifyGenerator(dataGeneratorId, generatorDelta).get();

      taskStore.cancelTransaction();
      Vector<Task> actualTasks = generatedTaskIds.asList().map(this::getTask);

      assertThat(actualTasks)
         .usingElementComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(expectedTasks);
   }

   @Test
   void testModifyGeneratorStandaloneTaskIsUnchanged() {
      Task expectedTask = getTask(dataTaskId);

      orchestrator.modifyGenerator(dataGeneratorId, generatorDelta).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(dataTaskId);

      assertThat(savedTask)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(expectedTask);
   }

   @Test
   void testModifyGeneratorTableIsUnchanged() {
      Table expectedTable = getTable(dataTableId);

      orchestrator.modifyGenerator(dataGeneratorId, generatorDelta).get();

      taskStore.cancelTransaction();
      Table savedTable = getTable(dataTableId);

      assertThat(savedTable)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(expectedTable);
   }

   @Test
   void testModifyAndSeverTaskStandalone() {
      Task task = getTask(dataTaskId);

      Task result = orchestrator.modifyAndSeverTask(dataTaskId, taskDelta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(task.withModification(taskDelta));
   }

   @Test
   void testModifyAndSeverTaskStandaloneIsSaved() {
      Task task = orchestrator.modifyAndSeverTask(dataTaskId, taskDelta).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(dataTaskId);

      assertThat(task)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(savedTask);
   }

   @Test
   void testModifyAndSeverTaskStandaloneGeneratorIsUnchanged() {
      Generator generator = getGenerator(dataGeneratorId);

      orchestrator.modifyAndSeverTask(dataTaskId, taskDelta).get();

      taskStore.cancelTransaction();
      Generator savedGenerator = getGenerator(dataGeneratorId);

      assertThat(savedGenerator)
         .usingComparator(TestComparators::compareGenerators)
         .isEqualTo(generator);
   }

   @Test
   void testModifyAndSeverTaskStandaloneGeneratedTasksAreUnchanged() {
      Vector<Task> expectedTasks = generatedTaskIds
         .asList()
         .map(this::getTask);

      orchestrator.modifyAndSeverTask(dataTaskId, taskDelta).get();

      taskStore.cancelTransaction();
      Vector<Task> actualTasks = generatedTaskIds
         .asList()
         .map(this::getTask);

      assertThat(actualTasks)
         .usingElementComparator(TestComparators::compareTasks)
         .isEqualTo(expectedTasks);
   }

   @Test
   void testModifyAndSeverTaskStandaloneTableIsUnchanged() {
      Table expectedTable = getTable(dataTableId);

      orchestrator.modifyAndSeverTask(dataTaskId, taskDelta).get();

      taskStore.cancelTransaction();
      Table actualTable = getTable(dataTableId);

      assertThat(actualTable)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(expectedTable);
   }

   @Test
   void testModifyAndSeverTaskSeries() {
      Task task = getTask(middleTaskId);
      Task expectedTask = task
         .withoutGenerator()
         .withModification(taskDelta);

      Task result = orchestrator.modifyAndSeverTask(middleTaskId, taskDelta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(expectedTask);
   }

   @Test
   void testModifyAndSeverTaskSeriesIsSaved() {
      Task task = orchestrator.modifyAndSeverTask(middleTaskId, taskDelta).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(middleTaskId);

      assertThat(task)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(savedTask);
   }

   @Test
   void testModifyAndSeverTaskSeriesOtherTasksAreUnchanged() {
      Vector<Task> expectedTasks = allTaskIds
         .asList()
         .map(this::getTask)
         .filter(task -> !task.getId().equals(middleTaskId));

      orchestrator.modifyAndSeverTask(middleTaskId, taskDelta).get();

      taskStore.cancelTransaction();
      Vector<Task> actualTasks = allTaskIds
         .asList()
         .map(this::getTask)
         .filter(task -> !task.getId().equals(middleTaskId));

      assertThat(actualTasks)
         .usingElementComparator(TestComparators::compareTasks)
         .isEqualTo(expectedTasks);
   }

   @Test
   void testModifyAndSeverTaskSeriesGeneratorIsSaved() {
      Generator expectedGenerator = getGenerator(dataGeneratorId)
         .withoutTask(middleTaskId);

      orchestrator.modifyAndSeverTask(middleTaskId, taskDelta).get();

      taskStore.cancelTransaction();
      Generator savedGenerator = getGenerator(dataGeneratorId);

      assertThat(savedGenerator)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(expectedGenerator);
   }

   @Test
   void testModifyAndSeverTaskTableIsUnchanged() {
      Table expectedTable = getTable(dataTableId);

      orchestrator.modifyAndSeverTask(middleTaskId, taskDelta).get();

      taskStore.cancelTransaction();
      Table savedTable = getTable(dataTableId);

      assertThat(savedTable)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(expectedTable);
   }

   @Test
   void testModifySeries() {
      Task task = getTask(middleTaskId);
      Generator generator = getGenerator(dataGeneratorId);
      Task result = orchestrator.modifySeries(middleTaskId, generatorDelta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(task.withSeriesModification(generatorDelta, generator));
   }

   @Test
   void testModifySeriesIsSaved() {
      Task task = orchestrator.modifySeries(middleTaskId, generatorDelta).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(middleTaskId);

      assertThat(task)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(savedTask);
   }

   @Test
   void testModifySeriesPriorTaskIsSaved() {
      Task expectedTask = getTask(priorTaskId).withoutGenerator();

      orchestrator.modifySeries(middleTaskId, generatorDelta).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(priorTaskId);

      assertThat(savedTask)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(expectedTask);
   }

   @Test
   void testModifySeriesSubsequentTaskIsSaved() {
      Generator generator = getGenerator(dataGeneratorId);
      Task expectedTask = getTask(subsequentTaskId)
         .withSeriesModification(generatorDelta, generator);

      orchestrator.modifySeries(middleTaskId, generatorDelta).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(subsequentTaskId);

      assertThat(savedTask)
         .usingComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(expectedTask);
   }

   @Test
   void testModifySeriesGeneratorIsSaved() {
      Generator expectedGenerator = getGenerator(dataGeneratorId)
         .withoutTask(priorTaskId)
         .withModification(generatorDelta);

      orchestrator.modifySeries(middleTaskId, generatorDelta).get();

      taskStore.cancelTransaction();
      Generator savedGenerator = getGenerator(dataGeneratorId);

      assertThat(savedGenerator)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(expectedGenerator);
   }

   @Test
   void testModifySeriesStandaloneTaskIsUnchanged() {
      Task expectedTask = getTask(dataTaskId);

      orchestrator.modifySeries(middleTaskId, generatorDelta).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(dataTaskId);

      assertThat(savedTask)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(expectedTask);
   }

   @Test
   void testModifySeriesTableIsUnchanged() {
      Table expectedTable = getTable(dataTableId);

      orchestrator.modifySeries(middleTaskId, generatorDelta).get();

      taskStore.cancelTransaction();
      Table savedTable = getTable(dataTableId);

      assertThat(savedTable)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(expectedTable);
   }

   @Test
   void testModifySeriesStandaloneTaskIsInvalid() {
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> orchestrator.modifySeries(dataTaskId, generatorDelta));
   }

   private Table getTable(ItemId<Table> id) {
      return taskStore.getTables().getById(id).get();
   }

   private Task getTask(ItemId<Task> id) {
      return taskStore.getTasks().getById(id).get();
   }

   private Generator getGenerator(ItemId<Generator> id) {
      return taskStore.getGenerators().getById(id).get();
   }

   private ItemId<Task> getGeneratedTaskId(Instant timestamp) {
      return generatedTaskIds
         .asList()
         .map(id -> taskStore.getTasks().getById(id).get())
         .filter(t -> {
            Property property = t.getProperties().asMap().get("alpha").get();
            Instant startTime = ((DateTime) property.get()).getStart();
            return startTime.equals(timestamp);
         })
         .get(0)
         .getId();
   }
}
