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

public class OrchestrationTests {
   private TaskStore taskStore;

   private ItemId<Table> dataTableId;
   private ItemId<Task> dataTaskId;
   private TaskMutator taskMutator;
   private ItemId<Generator> dataGeneratorId;
   private SetList<ItemId<Task>> generatedTaskIds;
   private SetList<ItemId<Task>> allTaskIds;
   private ItemId<Task> priorTaskId;
   private ItemId<Task> middleTaskId;
   private TaskMutator middleTaskMutator;
   private ItemId<Task> subsequentTaskId;

   private Instant patternStart;
   private Duration patternStep;
   private DatePattern pattern;

   private Instant lastGenerationTimestamp;

   private TaskDelta taskDelta;
   private GeneratorDelta generatorDelta;

   @BeforeEach
   void beforeEach() throws TaskAccessException {
      taskStore = InMemoryDataStore.createTaskStore();

      patternStart = LocalDate.now(ZoneOffset.UTC)
         .plusDays(2)
         .atStartOfDay(ZoneOffset.UTC)
         .toInstant();
      patternStep = Duration.parse("PT17M36.5S");
      pattern = new UniformDatePattern(patternStart, patternStep);

      dataTableId = TableMutator.createTable(taskStore).get().getId();
      TableMutator tableMutator = new TableMutator(taskStore, dataTableId);

      TableDelta dataTableDelta = new TableDelta(
         Schema.empty()
            .withColumn("alpha", TypeDescriptor.fromTypeName("DateTime"))
            .withColumn("beta", TypeDescriptor.fromTypeName("String")),
         null);
      tableMutator.modifyTable(dataTableDelta).checkError();

      dataTaskId = TaskMutator.createTask(taskStore, dataTableId).get().getId();
      taskMutator = new TaskMutator(taskStore, dataTaskId);
      dataGeneratorId = GeneratorMutator
         .createGenerator(taskStore, dataTableId, "alpha", pattern)
         .get()
         .getId();

      lastGenerationTimestamp = patternStart.plus(Duration.parse("PT45M"));
      allTaskIds = tableMutator.getTasksFromTable(lastGenerationTimestamp).get();
      generatedTaskIds = allTaskIds.remove(dataTaskId);

      priorTaskId = getGeneratedTaskId(patternStart);
      middleTaskId = getGeneratedTaskId(patternStart.plus(patternStep));
      middleTaskMutator = new TaskMutator(taskStore, middleTaskId);
      subsequentTaskId = getGeneratedTaskId(
         patternStart.plus(patternStep.multipliedBy(2)));

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
   void testCreateTask() throws TaskAccessException {
      Table table = getTable(dataTableId);

      Task result = TaskMutator.createTask(taskStore, dataTableId).get();
      assertThat(result)
         .usingComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(Task.newTask(table));
   }

   @Test
   void testCreateTaskResultIsSaved() throws TaskAccessException {
      Task result = TaskMutator.createTask(taskStore, dataTableId).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(result.getId());

      assertThat(result)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(savedTask);
   }

   @Test
   void testCreateTaskTableIsSaved() throws TaskAccessException {
      Table table = getTable(dataTableId);

      Task task = TaskMutator.createTask(taskStore, dataTableId).get();
      Table expectedTable = table.withTasks(Vector.of(task.getId()));

      taskStore.cancelTransaction();
      Table savedTable = getTable(dataTableId);

      assertThat(savedTable)
         .usingComparator(TestComparators::compareTablesIgnoringId)
         .isEqualTo(expectedTable);
   }

   @Test
   void testModifyAndSeverTaskStandalone() throws TaskAccessException {
      Task task = getTask(dataTaskId);

      Task result = taskMutator.modifyAndSeverTask(taskDelta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(task.withModification(taskDelta));
   }

   @Test
   void testModifyAndSeverTaskStandaloneIsSaved() throws TaskAccessException {
      Task task = taskMutator.modifyAndSeverTask(taskDelta).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(dataTaskId);

      assertThat(task)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(savedTask);
   }

   @Test
   void testModifyAndSeverTaskStandaloneGeneratorIsUnchanged() throws TaskAccessException {
      Generator generator = getGenerator(dataGeneratorId);

      taskMutator.modifyAndSeverTask(taskDelta).get();

      taskStore.cancelTransaction();
      Generator savedGenerator = getGenerator(dataGeneratorId);

      assertThat(savedGenerator)
         .usingComparator(TestComparators::compareGenerators)
         .isEqualTo(generator);
   }

   @Test
   void testModifyAndSeverTaskStandaloneGeneratedTasksAreUnchanged() throws TaskAccessException {
      Vector<Task> expectedTasks = generatedTaskIds
         .asList()
         .map(this::getTask);

      taskMutator.modifyAndSeverTask(taskDelta).get();

      taskStore.cancelTransaction();
      Vector<Task> actualTasks = generatedTaskIds
         .asList()
         .map(this::getTask);

      assertThat(actualTasks)
         .usingElementComparator(TestComparators::compareTasks)
         .isEqualTo(expectedTasks);
   }

   @Test
   void testModifyAndSeverTaskStandaloneTableIsUnchanged() throws TaskAccessException {
      Table expectedTable = getTable(dataTableId);

      taskMutator.modifyAndSeverTask(taskDelta).get();

      taskStore.cancelTransaction();
      Table actualTable = getTable(dataTableId);

      assertThat(actualTable)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(expectedTable);
   }

   @Test
   void testModifyAndSeverTaskSeries() throws TaskAccessException {
      Task task = getTask(middleTaskId);
      Task expectedTask = task
         .withoutGenerator()
         .withModification(taskDelta);

      Task result = middleTaskMutator.modifyAndSeverTask(taskDelta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(expectedTask);
   }

   @Test
   void testModifyAndSeverTaskSeriesIsSaved() throws TaskAccessException {
      Task task = middleTaskMutator.modifyAndSeverTask(taskDelta).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(middleTaskId);

      assertThat(task)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(savedTask);
   }

   @Test
   void testModifyAndSeverTaskSeriesOtherTasksAreUnchanged() throws TaskAccessException {
      Vector<Task> expectedTasks = allTaskIds
         .asList()
         .map(this::getTask)
         .filter(task -> !task.getId().equals(middleTaskId));

      middleTaskMutator.modifyAndSeverTask(taskDelta).get();

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
   void testModifyAndSeverTaskSeriesGeneratorIsSaved() throws TaskAccessException {
      Generator expectedGenerator = getGenerator(dataGeneratorId)
         .withoutTask(middleTaskId);

      middleTaskMutator.modifyAndSeverTask(taskDelta).get();

      taskStore.cancelTransaction();
      Generator savedGenerator = getGenerator(dataGeneratorId);

      assertThat(savedGenerator)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(expectedGenerator);
   }

   @Test
   void testModifyAndSeverTaskTableIsUnchanged() throws TaskAccessException {
      Table expectedTable = getTable(dataTableId);

      middleTaskMutator.modifyAndSeverTask(taskDelta).get();

      taskStore.cancelTransaction();
      Table savedTable = getTable(dataTableId);

      assertThat(savedTable)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(expectedTable);
   }

   @Test
   void testModifySeries() throws TaskAccessException {
      Task task = getTask(middleTaskId);
      Generator generator = getGenerator(dataGeneratorId);
      Task result = middleTaskMutator.modifySeries(generatorDelta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(task.withSeriesModification(generatorDelta, generator));
   }

   @Test
   void testModifySeriesIsSaved() throws TaskAccessException {
      Task task = middleTaskMutator.modifySeries(generatorDelta).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(middleTaskId);

      assertThat(task)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(savedTask);
   }

   @Test
   void testModifySeriesPriorTaskIsSaved() throws TaskAccessException {
      Task expectedTask = getTask(priorTaskId).withoutGenerator();

      middleTaskMutator.modifySeries(generatorDelta).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(priorTaskId);

      assertThat(savedTask)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(expectedTask);
   }

   @Test
   void testModifySeriesSubsequentTaskIsSaved() throws TaskAccessException {
      Generator generator = getGenerator(dataGeneratorId);
      Task expectedTask = getTask(subsequentTaskId)
         .withSeriesModification(generatorDelta, generator);

      middleTaskMutator.modifySeries(generatorDelta).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(subsequentTaskId);

      assertThat(savedTask)
         .usingComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(expectedTask);
   }

   @Test
   void testModifySeriesGeneratorIsSaved() throws TaskAccessException {
      Generator expectedGenerator = getGenerator(dataGeneratorId)
         .withoutTask(priorTaskId)
         .withModification(generatorDelta);

      middleTaskMutator.modifySeries(generatorDelta).get();

      taskStore.cancelTransaction();
      Generator savedGenerator = getGenerator(dataGeneratorId);

      assertThat(savedGenerator)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(expectedGenerator);
   }

   @Test
   void testModifySeriesStandaloneTaskIsUnchanged() throws TaskAccessException {
      Task expectedTask = getTask(dataTaskId);

      middleTaskMutator.modifySeries(generatorDelta).get();

      taskStore.cancelTransaction();
      Task savedTask = getTask(dataTaskId);

      assertThat(savedTask)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(expectedTask);
   }

   @Test
   void testModifySeriesTableIsUnchanged() throws TaskAccessException {
      Table expectedTable = getTable(dataTableId);

      middleTaskMutator.modifySeries(generatorDelta).get();

      taskStore.cancelTransaction();
      Table savedTable = getTable(dataTableId);

      assertThat(savedTable)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(expectedTable);
   }

   @Test
   void testModifySeriesStandaloneTaskIsInvalid() {
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> taskMutator.modifySeries(generatorDelta));
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

   private ItemId<Task> getGeneratedTaskId(Instant timestamp) {
      return generatedTaskIds
         .asList()
         .map(this::getTask)
         .filter(t -> {
            Property property = t.getProperties().asMap().get("alpha").get();
            Instant startTime = ((DateTime) property.get()).getStart();
            return startTime.equals(timestamp);
         })
         .get(0)
         .getId();
   }
}
