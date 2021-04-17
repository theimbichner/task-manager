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

   private ItemId<Table> tableId;
   private ItemId<Task> taskId;
   private ItemId<Generator> generatorId;
   private GeneratorMutator generatorMutator;
   private SetList<ItemId<Task>> generatedTaskIds;
   private SetList<ItemId<Task>> allTaskIds;

   private DatePattern pattern;

   private GeneratorDelta generatorDelta;

   @BeforeEach
   void beforeEach() throws TaskAccessException {
      taskStore = InMemoryDataStore.createTaskStore();

      Instant patternStart = LocalDate.now(ZoneOffset.UTC)
         .plusDays(2)
         .atStartOfDay(ZoneOffset.UTC)
         .toInstant();
      Duration patternStep = Duration.parse("PT17M36.5S");
      pattern = new UniformDatePattern(patternStart, patternStep);

      tableId = TableMutator.createTable(taskStore).get().getId();
      TableMutator tableMutator = new TableMutator(taskStore, tableId);

      TableDelta dataTableDelta = new TableDelta(
         Schema.empty()
            .withColumn("alpha", TypeDescriptor.fromTypeName("DateTime"))
            .withColumn("beta", TypeDescriptor.fromTypeName("String")),
         null);
      tableMutator.modifyTable(dataTableDelta).checkError();

      taskId = TaskMutator.createTask(taskStore, tableId).get().getId();
      generatorId = GeneratorMutator
         .createGenerator(taskStore, tableId, "alpha", pattern)
         .get()
         .getId();
      generatorMutator = new GeneratorMutator(taskStore, generatorId);

      Instant lastGenerationTimestamp = patternStart.plus(Duration.parse("PT45M"));
      allTaskIds = tableMutator.getTasksFromTable(lastGenerationTimestamp).get();
      generatedTaskIds = allTaskIds.remove(taskId);

      generatorDelta = new GeneratorDelta(
         PropertyMap.empty().put("beta", Property.DELETE),
         null,
         null,
         null,
         null);
   }

   @Test
   void testCreateGenerator() throws TaskAccessException {
      Table table = getTable(tableId);

      Generator result = GeneratorMutator.createGenerator(
         taskStore,
         tableId,
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
         tableId,
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
      Table table = getTable(tableId);

      Generator generator = GeneratorMutator.createGenerator(
         taskStore,
         tableId,
         "alpha",
         pattern).get();
      Table expectedTable = table.withGenerator(generator.getId());

      taskStore.cancelTransaction();
      Table savedTable = getTable(tableId);

      assertThat(savedTable)
         .usingComparator(TestComparators::compareTables)
         .isEqualTo(expectedTable);
   }

   @Test
   void testModifyGenerator() throws TaskAccessException {
      Generator generator = getGenerator(generatorId);

      Generator result = generatorMutator.modifyGenerator(generatorDelta).get();

      assertThat(result)
         .usingComparator(TestComparators::compareGeneratorsIgnoringId)
         .isEqualTo(generator.withModification(generatorDelta));
   }

   @Test
   void testModifyGeneratorGeneratorIsSaved() throws TaskAccessException {
      Generator generator = generatorMutator.modifyGenerator(generatorDelta).get();

      taskStore.cancelTransaction();
      Generator savedGenerator = getGenerator(generatorId);

      assertThat(generator)
         .usingComparator(TestComparators::compareGenerators)
         .isEqualTo(savedGenerator);
   }

   @Test
   void testModifyGeneratorTasksAreSaved() throws TaskAccessException {
      Generator generator = getGenerator(generatorId);
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
      Task expectedTask = getTask(taskId);

      generatorMutator.modifyGenerator(generatorDelta).checkError();

      taskStore.cancelTransaction();
      Task savedTask = getTask(taskId);

      assertThat(savedTask)
         .usingComparator(TestComparators::compareTasks)
         .isEqualTo(expectedTask);
   }

   @Test
   void testModifyGeneratorTableIsUnchanged() throws TaskAccessException {
      Table expectedTable = getTable(tableId);

      generatorMutator.modifyGenerator(generatorDelta).checkError();

      taskStore.cancelTransaction();
      Table savedTable = getTable(tableId);

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
