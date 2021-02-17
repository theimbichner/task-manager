package io.github.theimbichner.taskmanager.task;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import io.vavr.collection.HashMap;

import org.json.JSONObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.taskmanager.io.InMemoryDataStore;
import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.task.property.Schema;
import io.github.theimbichner.taskmanager.task.property.TypeDescriptor;
import io.github.theimbichner.taskmanager.time.DatePattern;
import io.github.theimbichner.taskmanager.time.DateTime;
import io.github.theimbichner.taskmanager.time.UniformDatePattern;

import static org.assertj.core.api.Assertions.*;

public class TableTests {
   static TaskStore taskStore;

   @BeforeAll
   static void beforeAll() {
      taskStore = InMemoryDataStore.createTaskStore();
   }

   private static DatePattern getDatePattern(int step) {
      return new UniformDatePattern(Instant.now().plusSeconds(1), Duration.ofSeconds(step));
   }

   @Test
   void testNewTable() {
      Instant before = Instant.now();
      Table table = Table.newTable();
      Instant after = Instant.now();

      assertThat(table.getName()).isEqualTo("");

      assertThat(table.getDateCreated().getStart())
         .isAfterOrEqualTo(before)
         .isBeforeOrEqualTo(after)
         .isEqualTo(table.getDateCreated().getEnd());
      assertThat(table.getDateLastModified().getStart())
         .isEqualTo(table.getDateCreated().getStart())
         .isEqualTo(table.getDateLastModified().getEnd());

      assertThat(table.getSchema().isEmpty()).isTrue();
   }

   @Test
   void testWithModification() {
      Table table = Table.newTable();

      Instant before = Instant.now();
      table = table.withModification(new TableDelta(
         Schema.empty()
            .withColumn("alpha", TypeDescriptor.fromTypeName("String"))
            .withColumn("beta", TypeDescriptor.fromTypeName("DateTime")),
         null));
      assertThat(table.getDateLastModified().getStart()).isAfterOrEqualTo(before);
      assertThat(table.getName()).isEqualTo("");
      assertThat(table.getSchema().asMap().mapValues(x -> x.getTypeName()))
         .isEqualTo(HashMap.of(
            "alpha", "String",
            "beta", "DateTime"));

      before = Instant.now();
      table = table.withModification(new TableDelta(
         Schema.empty()
            .withoutColumn("alpha")
            .withColumnRenamed("beta", "gamma"),
         "name 1"));
      assertThat(table.getDateLastModified().getStart()).isAfterOrEqualTo(before);
      assertThat(table.getName()).isEqualTo("name 1");
      assertThat(table.getSchema().asMap().mapValues(x -> x.getTypeName()))
         .isEqualTo(HashMap.of(
            "gamma", "DateTime"));

      before = Instant.now();
      table = table.withModification(new TableDelta(
         Schema.empty(),
         "name 2"));
      assertThat(table.getDateLastModified().getStart()).isAfterOrEqualTo(before);
      assertThat(table.getName()).isEqualTo("name 2");
      assertThat(table.getSchema().asMap().mapValues(x -> x.getTypeName()))
         .isEqualTo(HashMap.of(
            "gamma", "DateTime"));
   }

   @Test
   void testWithModificationEmpty() {
      Table table = Table.newTable();
      DateTime dateLastModified = table.getDateLastModified();

      table = table.withModification(new TableDelta(Schema.empty(), null));
      assertThat(table.getDateLastModified()).isEqualTo(dateLastModified);
      assertThat(table.getName()).isEqualTo("");
      assertThat(table.getSchema().isEmpty()).isTrue();
   }

   @Test
   void testLinkUnlinkTasks() {
      Table table = Table.newTable();

      assertThat(table.getAllTaskIds().asList()).isEmpty();

      table = table.withTasks(List.of(ItemId.of("alpha")));
      assertThat(table.getAllTaskIds().asList()).isEqualTo(List.of(ItemId.of("alpha")));

      table = table.withTasks(List.of(ItemId.of("beta")));
      assertThat(table.getAllTaskIds().asList()).isEqualTo(List.of(ItemId.of("alpha"), ItemId.of("beta")));

      table = table.withTasks(List.of(ItemId.of("alpha")));
      assertThat(table.getAllTaskIds().asList()).isEqualTo(List.of(ItemId.of("alpha"), ItemId.of("beta")));

      table = table.withoutTask(ItemId.of("gamma"));
      assertThat(table.getAllTaskIds().asList()).isEqualTo(List.of(ItemId.of("alpha"), ItemId.of("beta")));

      table = table.withoutTask(ItemId.of("alpha"));
      assertThat(table.getAllTaskIds().asList()).isEqualTo(List.of(ItemId.of("beta")));
   }

   @Test
   void testLinkUnlinkGenerators() {
      Table table = Table.newTable();
      assertThat(table.getAllGeneratorIds().asList()).isEmpty();

      ItemId<Generator> alpha = ItemId.randomId();
      ItemId<Generator> beta = ItemId.randomId();
      ItemId<Generator> gamma = ItemId.randomId();

      table = table.withGenerator(alpha);
      assertThat(table.getAllGeneratorIds().asList()).isEqualTo(List.of(alpha));

      table = table.withGenerator(beta);
      assertThat(table.getAllGeneratorIds().asList()).isEqualTo(List.of(alpha, beta));

      table = table.withGenerator(alpha);
      assertThat(table.getAllGeneratorIds().asList()).isEqualTo(List.of(alpha, beta));

      table = table.withoutGenerator(gamma);
      assertThat(table.getAllGeneratorIds().asList()).isEqualTo(List.of(alpha, beta));

      table = table.withoutGenerator(alpha);
      assertThat(table.getAllGeneratorIds().asList()).isEqualTo(List.of(beta));
   }

   @Test
   void testToFromJson() {
      Table table = Table.newTable();
      table.setTaskStore(taskStore);

      Generator generator = Generator.newGenerator(table, "", getDatePattern(7));
      taskStore.getGenerators().save(generator).get();
      ItemId<Generator> generatorId = generator.getId();

      List<ItemId<Task>> taskIds = List.of(
         ItemId.of("alpha"),
         ItemId.of("beta"));

      table = table
         .withTasks(taskIds)
         .withGenerator(generatorId);

      JSONObject json = table.toJson();
      Table newTable = Table.fromJson(json);
      newTable.setTaskStore(taskStore);

      assertThat(newTable.getId()).isEqualTo(table.getId());
      assertThat(newTable.getName()).isEqualTo(table.getName());

      assertThat(newTable.getDateCreated().getStart())
         .isEqualTo(table.getDateCreated().getStart());
      assertThat(newTable.getDateCreated().getEnd())
         .isEqualTo(table.getDateCreated().getEnd());
      assertThat(newTable.getDateLastModified().getStart())
         .isEqualTo(table.getDateLastModified().getStart());
      assertThat(newTable.getDateLastModified().getEnd())
         .isEqualTo(table.getDateLastModified().getEnd());

      assertThat(newTable.getAllTaskIds().asList()).isEqualTo(taskIds);
      assertThat(newTable.getAllGeneratorIds().asList()).isEqualTo(List.of(generatorId));
      assertThat(newTable.getSchema().isEmpty()).isTrue();
   }

   @Test
   void testToFromJsonNoTasks() {
      Table table = Table.newTable();
      JSONObject json = table.toJson();
      Table newTable = Table.fromJson(json);

      assertThat(newTable.getAllTaskIds().asList()).isEmpty();
      assertThat(newTable.getAllGeneratorIds().asList()).isEmpty();
      assertThat(newTable.getSchema().isEmpty()).isTrue();
   }
}
