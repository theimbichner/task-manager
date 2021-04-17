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
import io.github.theimbichner.taskmanager.time.UniformDatePattern;

import static org.assertj.core.api.Assertions.*;

public class GeneratorMutatorTests {
   private TaskStore taskStore;

   private ItemId<Table> dataTableId;
   private ItemId<Task> dataTaskId;
   private ItemId<Generator> dataGeneratorId;
   private GeneratorMutator generatorMutator;
   private SetList<ItemId<Task>> generatedTaskIds;
   private SetList<ItemId<Task>> allTaskIds;

   private Instant patternStart;
   private Duration patternStep;
   private DatePattern pattern;

   private Instant lastGenerationTimestamp;

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
      dataGeneratorId = GeneratorMutator
         .createGenerator(taskStore, dataTableId, "alpha", pattern)
         .get()
         .getId();
      generatorMutator = new GeneratorMutator(taskStore, dataGeneratorId);

      lastGenerationTimestamp = patternStart.plus(Duration.parse("PT45M"));
      allTaskIds = tableMutator.getTasksFromTable(lastGenerationTimestamp).get();
      generatedTaskIds = allTaskIds.remove(dataTaskId);

      generatorDelta = new GeneratorDelta(
         PropertyMap.empty().put("beta", Property.DELETE),
         null,
         null,
         null,
         null);
   }

   @Test
   void testCreateGenerator() throws TaskAccessException {
      Table table = getTable(dataTableId);

      Generator result = GeneratorMutator.createGenerator(
         taskStore,
         dataTableId,
         "alpha",
         pattern).get();
      assertThat(result)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(Generator.newGenerator(table, "alpha", pattern));
   }

   @Test
   void testCreateGeneratorResultIsSaved() throws TaskAccessException {
      Generator generator = GeneratorMutator.createGenerator(
         taskStore,
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
   void testCreateGeneratorTableIsSaved() throws TaskAccessException {
      Table table = getTable(dataTableId);

      Generator generator = GeneratorMutator.createGenerator(
         taskStore,
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
   void testModifyGenerator() throws TaskAccessException {
      Generator generator = getGenerator(dataGeneratorId);

      Generator result = generatorMutator.modifyGenerator(generatorDelta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(generator.withModification(generatorDelta));
   }

   @Test
   void testModifyGeneratorGeneratorIsSaved() throws TaskAccessException {
      Generator generator = generatorMutator.modifyGenerator(generatorDelta).get();

      taskStore.cancelTransaction();
      Generator savedGenerator = getGenerator(dataGeneratorId);

      assertThat(generator)
         .usingComparator(TestComparators::compareGenerators)
         .isEqualTo(savedGenerator);
   }

   @Test
   void testModifyGeneratorTasksAreSaved() throws TaskAccessException {
      Generator generator = getGenerator(dataGeneratorId);
      Vector<Task> expectedTasks = generatedTaskIds
         .asList()
         .map(this::getTask)
         .map(task -> task.withSeriesModification(generatorDelta, generator));

      generatorMutator.modifyGenerator(generatorDelta).checkError();

      taskStore.cancelTransaction();
      Vector<Task> actualTasks = generatedTaskIds.asList().map(this::getTask);

      assertThat(actualTasks)
         .usingElementComparator(TestComparators::compareTasksIgnoringId)
         .isEqualTo(expectedTasks);
   }

   @Test
   void testModifyGeneratorStandaloneTaskIsUnchanged() throws TaskAccessException {
      Task expectedTask = getTask(dataTaskId);

      generatorMutator.modifyGenerator(generatorDelta).checkError();

      taskStore.cancelTransaction();
      Task savedTask = getTask(dataTaskId);

      assertThat(savedTask)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(expectedTask);
   }

   @Test
   void testModifyGeneratorTableIsUnchanged() throws TaskAccessException {
      Table expectedTable = getTable(dataTableId);

      generatorMutator.modifyGenerator(generatorDelta).checkError();

      taskStore.cancelTransaction();
      Table savedTable = getTable(dataTableId);

      assertThat(savedTable)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(expectedTable);
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
