package io.github.theimbichner.taskmanager.task;

import java.time.Instant;

import io.vavr.collection.HashMap;
import io.vavr.collection.Vector;

import org.json.JSONObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.taskmanager.io.TaskStore;
import io.github.theimbichner.taskmanager.io.datastore.impl.InMemoryDataStore;
import io.github.theimbichner.taskmanager.task.property.Schema;
import io.github.theimbichner.taskmanager.task.property.TypeDescriptor;
import io.github.theimbichner.taskmanager.time.DateTime;

import static org.assertj.core.api.Assertions.*;

public class TableTests {
   static TaskStore taskStore;

   @BeforeAll
   static void beforeAll() {
      taskStore = InMemoryDataStore.createTaskStore();
   }

   @Test
   void testNewTable() {
      Instant before = Instant.now();
      Table table = Table.newTable();
      Instant after = Instant.now();

      assertThat(table.getName()).isEmpty();

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
      assertThat(table.getName()).isEmpty();
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
      assertThat(table.getName()).isEmpty();
      assertThat(table.getSchema().isEmpty()).isTrue();
   }

   @Test
   void testLinkUnlinkTasks() {
      Table table = Table.newTable();

      assertThat(table.getAllTaskIds().asList()).isEmpty();

      table = table.withTasks(Vector.of(ItemId.of("alpha")));
      assertThat(table.getAllTaskIds().asList())
         .isEqualTo(Vector.of(ItemId.of("alpha")));

      table = table.withTasks(Vector.of(ItemId.of("beta")));
      assertThat(table.getAllTaskIds().asList())
         .isEqualTo(Vector.of(ItemId.of("alpha"), ItemId.of("beta")));

      table = table.withTasks(Vector.of(ItemId.of("alpha")));
      assertThat(table.getAllTaskIds().asList())
         .isEqualTo(Vector.of(ItemId.of("alpha"), ItemId.of("beta")));

      table = table.withoutTask(ItemId.of("gamma"));
      assertThat(table.getAllTaskIds().asList())
         .isEqualTo(Vector.of(ItemId.of("alpha"), ItemId.of("beta")));

      table = table.withoutTask(ItemId.of("alpha"));
      assertThat(table.getAllTaskIds().asList())
         .isEqualTo(Vector.of(ItemId.of("beta")));
   }

   @Test
   void testLinkUnlinkGenerators() {
      Table table = Table.newTable();
      assertThat(table.getAllGeneratorIds().asList()).isEmpty();

      ItemId<Generator> alpha = ItemId.randomId();
      ItemId<Generator> beta = ItemId.randomId();
      ItemId<Generator> gamma = ItemId.randomId();

      table = table.withGenerator(alpha);
      assertThat(table.getAllGeneratorIds().asList())
         .isEqualTo(Vector.of(alpha));

      table = table.withGenerator(beta);
      assertThat(table.getAllGeneratorIds().asList())
         .isEqualTo(Vector.of(alpha, beta));

      table = table.withGenerator(alpha);
      assertThat(table.getAllGeneratorIds().asList())
         .isEqualTo(Vector.of(alpha, beta));

      table = table.withoutGenerator(gamma);
      assertThat(table.getAllGeneratorIds().asList())
         .isEqualTo(Vector.of(alpha, beta));

      table = table.withoutGenerator(alpha);
      assertThat(table.getAllGeneratorIds().asList())
         .isEqualTo(Vector.of(beta));
   }

   @Test
   void testToFromJson() {
      Table table = Table.newTable();

      ItemId<Generator> generatorId = ItemId.randomId();

      Vector<ItemId<Task>> taskIds = Vector.of(
         ItemId.randomId(),
         ItemId.randomId());

      table = table
         .withTasks(taskIds)
         .withGenerator(generatorId);

      JSONObject json = table.toJson();
      Table newTable = Table.fromJson(json);

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
      assertThat(newTable.getAllGeneratorIds().asList()).isEqualTo(Vector.of(generatorId));
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
