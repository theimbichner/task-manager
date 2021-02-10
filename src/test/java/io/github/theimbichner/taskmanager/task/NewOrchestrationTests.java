package io.github.theimbichner.taskmanager.task;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

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
import static org.assertj.vavr.api.VavrAssertions.*;

public class NewOrchestrationTests {
   private TaskStore taskStore;

   private String dataTableId;
   private String dataTaskId;
   private String dataGeneratorId;
   private SetList<String> generatedTaskIds;
   private SetList<String> allTaskIds;

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

      patternStart = LocalDate.now(ZoneOffset.UTC)
         .plusDays(2)
         .atStartOfDay(ZoneOffset.UTC)
         .toInstant();
      patternStep = Duration.parse("PT17M36.5S");
      pattern = new UniformDatePattern(patternStart, patternStep);

      Table table = Orchestration.createTable(taskStore).get();
      dataTableId = table.getId();

      TableDelta dataTableDelta = new TableDelta(
         Schema.empty()
            .withColumn("alpha", TypeDescriptor.fromTypeName("DateTime"))
            .withColumn("beta", TypeDescriptor.fromTypeName("String")),
         null);
      Orchestration.modifyTable(table, dataTableDelta);
      table = taskStore.getTables().getById(dataTableId).get();

      Task task = Orchestration.createTask(table).get();
      dataTaskId = task.getId();
      table = taskStore.getTables().getById(dataTableId).get();

      Generator generator = Orchestration.createGenerator(
         table,
         "alpha",
         pattern).get();
      dataGeneratorId = generator.getId();
      table = taskStore.getTables().getById(dataTableId).get();

      lastGenerationTimestamp = patternStart.plus(Duration.parse("PT45M"));
      allTaskIds = Orchestration.getTasksFromTable(
         table,
         lastGenerationTimestamp).get();
      generatedTaskIds = allTaskIds.remove(dataTaskId);

      tableDelta = new TableDelta(
         Schema.empty().withoutColumn("beta"),
         null);
      taskDelta = new TaskDelta(
         PropertyMap.empty().put("beta", Property.DELETE),
         null,
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
      Table table = Orchestration.createTable(taskStore).get();

      assertThat(table)
         .usingComparator(TestComparators::compareTablesIgnoringId)
         .isEqualTo(Table.newTable());
   }

   @Test
   void testCreateTableIsSaved() {
      Table table = Orchestration.createTable(taskStore).get();
      Table savedTable = taskStore.getTables().getById(table.getId()).get();

      assertThat(table)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(savedTable);
   }

   @Test
   void testModifyEmptyTable() {
      Table table = Orchestration.createTable(taskStore).get();
      TableDelta delta = new TableDelta(
         Schema.empty()
            .withColumn("alpha", TypeDescriptor.fromTypeName("DateTime"))
            .withColumn("beta", TypeDescriptor.fromTypeName("String")),
         "Renamed table");

      Table modifiedTable = Orchestration.modifyTable(table, delta).get();

      assertThat(modifiedTable)
         .usingComparator(TestComparators::compareTablesIgnoringId)
         .isEqualTo(table.withModification(delta));
   }

   @Test
   void testModifyEmptyTableIsSaved() {
      Table table = Orchestration.createTable(taskStore).get();
      TableDelta delta = new TableDelta(
         Schema.empty()
            .withColumn("alpha", TypeDescriptor.fromTypeName("DateTime"))
            .withColumn("beta", TypeDescriptor.fromTypeName("String")),
         "Renamed table");

      table = Orchestration.modifyTable(table, delta).get();

      Table savedTable = taskStore.getTables().getById(table.getId()).get();
      assertThat(table)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(savedTable);
   }

   @Test
   void testCreateTask() {
      Table table = taskStore.getTables().getById(dataTableId).get();

      Task task = Orchestration.createTask(table).get();
      assertThat(task)
         .usingComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(Task.newTask(table));
   }

   @Test
   void testCreateTaskResultIsSaved() {
      Table table = taskStore.getTables().getById(dataTableId).get();

      Task task = Orchestration.createTask(table).get();
      Task savedTask = taskStore.getTasks().getById(task.getId()).get();

      assertThat(task)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(savedTask);
   }

   @Test
   void testCreateTaskTableIsSaved() {
      Table table = taskStore.getTables().getById(dataTableId).get();

      Task task = Orchestration.createTask(table).get();

      Table savedTable = taskStore.getTables().getById(table.getId()).get();
      assertThat(savedTable.getAllTaskIds().asList()).contains(task.getId());
   }

   @Test
   void testCreateGenerator() {
      Table table = taskStore.getTables().getById(dataTableId).get();

      Generator generator = Orchestration.createGenerator(
         table,
         "alpha",
         pattern).get();
      assertThat(generator)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(Generator.newGenerator(table, "alpha", pattern));
   }

   @Test
   void testCreateGeneratorResultIsSaved() {
      Table table = taskStore.getTables().getById(dataTableId).get();

      Generator generator = Orchestration.createGenerator(
         table,
         "alpha",
         pattern).get();
      Generator savedGenerator = taskStore
         .getGenerators()
         .getById(generator.getId())
         .get();
      assertThat(generator)
         .usingComparator(TestComparators::compareGenerators)
         .isEqualTo(savedGenerator);
   }

   @Test
   void testCreateGeneratorTableIsSaved() {
      Table table = taskStore.getTables().getById(dataTableId).get();

      Generator generator = Orchestration.createGenerator(
         table,
         "alpha",
         pattern).get();

      Table savedTable = taskStore.getTables().getById(table.getId()).get();
      assertThat(savedTable.getAllGeneratorIds().asList())
         .contains(generator.getId());
   }

   @Test
   void testGetTasksFromTableTimestamps() {
      List<Instant> actualStartTimes = generatedTaskIds
         .asList()
         .stream()
         .map(id -> taskStore.getTasks().getById(id).get())
         .map(task -> task.getProperties().asMap().get("alpha").get())
         .map(property -> ((DateTime) property.get()).getStart())
         .collect(Collectors.toList());
      List<Instant> expectedStartTimes = List.of(
         patternStart,
         patternStart.plus(patternStep),
         patternStart.plus(patternStep.multipliedBy(2)));

      assertThat(actualStartTimes)
         .containsExactlyInAnyOrderElementsOf(expectedStartTimes);
   }

   @Test
   void testGetTasksFromTableTasksAreSaved() {
      Generator generator = taskStore
         .getGenerators()
         .getById(dataGeneratorId)
         .get();

      for (String id : generatedTaskIds.asList()) {
         Task task = taskStore.getTasks().getById(id).get();
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
      Generator savedGenerator = taskStore
         .getGenerators()
         .getById(dataGeneratorId)
         .get();
      assertThat(savedGenerator.getTaskIds().asList())
         .containsExactlyInAnyOrderElementsOf(generatedTaskIds.asList());
   }

   @Test
   void testGetTasksFromTableGeneratorTimestampIsSaved() {
      Table table = taskStore.getTables().getById(dataTableId).get();
      SetList<String> result = Orchestration.getTasksFromTable(
         table,
         lastGenerationTimestamp).get();

      assertThat(result.asList())
         .containsExactlyInAnyOrderElementsOf(allTaskIds.asList());
   }

   @Test
   void testGetTasksFromTableTableIsSaved() {
      Table savedTable = taskStore.getTables().getById(dataTableId).get();
      assertThat(savedTable.getAllTaskIds().asList())
         .containsExactlyInAnyOrderElementsOf(allTaskIds.asList());
   }

   @Test
   void testModifyTable() {
      Table table = taskStore.getTables().getById(dataTableId).get();

      Table result = Orchestration.modifyTable(table, tableDelta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareTablesIgnoringId)
         .isEqualTo(table.withModification(tableDelta));
   }

   @Test
   void testModifyTableTableIsSaved() {
      Table table = taskStore.getTables().getById(dataTableId).get();

      table = Orchestration.modifyTable(table, tableDelta).get();
      Table savedTable = taskStore.getTables().getById(dataTableId).get();

      assertThat(table)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(savedTable);
   }

   @Test
   void testModfyTableGeneratorIsSaved() {
      Generator generator = taskStore
         .getGenerators()
         .getById(dataGeneratorId)
         .get();

      Table table = taskStore.getTables().getById(dataTableId).get();

      Orchestration.modifyTable(table, tableDelta).get();
      Generator savedGenerator = taskStore
         .getGenerators()
         .getById(dataGeneratorId)
         .get();

      assertThat(savedGenerator)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(generator.adjustToSchema(tableDelta.getSchema()));
   }

   @Test
   void testModifyTableTasksAreSaved() {
      List<Task> expectedTasks = allTaskIds
         .asList()
         .stream()
         .map(id -> taskStore.getTasks().getById(id).get())
         .map(task -> task.withModification(taskDelta).get())
         .collect(Collectors.toList());

      Table table = taskStore.getTables().getById(dataTableId).get();

      Orchestration.modifyTable(table, tableDelta).get();

      List<Task> actualTasks = allTaskIds
         .asList()
         .stream()
         .map(id -> taskStore.getTasks().getById(id).get())
         .collect(Collectors.toList());

      assertThat(actualTasks)
         .usingElementComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(expectedTasks);
   }

   @Test
   void testModifyGenerator() {
      Generator generator = taskStore
         .getGenerators()
         .getById(dataGeneratorId)
         .get();

      Generator result = Orchestration.modifyGenerator(
         generator,
         generatorDelta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(generator.withModification(generatorDelta));
   }

   @Test
   void testModifyGeneratorGeneratorIsSaved() {
      Generator generator = taskStore
         .getGenerators()
         .getById(dataGeneratorId)
         .get();

      generator = Orchestration.modifyGenerator(
         generator,
         generatorDelta).get();
      Generator savedGenerator = taskStore
         .getGenerators()
         .getById(dataGeneratorId)
         .get();

      assertThat(generator)
         .usingComparator(TestComparators::compareGenerators)
         .isEqualTo(savedGenerator);
   }

   @Test
   void testModifyGeneratorTasksAreSaved() {
      List<Task> expectedTasks = generatedTaskIds
         .asList()
         .stream()
         .map(id -> taskStore.getTasks().getById(id).get())
         .map(task -> task.withModification(taskDelta).get())
         .collect(Collectors.toList());

      Generator generator = taskStore
         .getGenerators()
         .getById(dataGeneratorId)
         .get();

      Orchestration.modifyGenerator(generator, generatorDelta).get();

      List<Task> actualTasks = generatedTaskIds
         .asList()
         .stream()
         .map(id -> taskStore.getTasks().getById(id).get())
         .collect(Collectors.toList());
      assertThat(actualTasks)
         .usingElementComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(expectedTasks);
   }

   @Test
   void testModifyGeneratorStandaloneTaskIsUnchanged() {
      Task expectedTask = taskStore.getTasks().getById(dataTaskId).get();

      Generator generator = taskStore
         .getGenerators()
         .getById(dataGeneratorId)
         .get();

      Orchestration.modifyGenerator(generator, generatorDelta).get();

      Task actualTask = taskStore.getTasks().getById(dataTaskId).get();
      assertThat(actualTask)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(expectedTask);
   }

   // 7. modify series (from specified task)
   // 8. modify task (standalone)
   // Check that all tasks are saved
   // Check that all generators are saved
}
