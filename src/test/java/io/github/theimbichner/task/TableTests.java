package io.github.theimbichner.task;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.json.JSONObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.task.io.InMemoryDataStore;
import io.github.theimbichner.task.io.TaskAccessException;
import io.github.theimbichner.task.io.TaskStore;
import io.github.theimbichner.task.time.DatePattern;
import io.github.theimbichner.task.time.UniformDatePattern;

import static org.assertj.core.api.Assertions.*;

public class TableTests {
   static TaskStore inMemoryTaskStore;
   static DatePattern datePattern;

   @BeforeAll
   static void beforeAll() {
      inMemoryTaskStore = new TaskStore(
         new InMemoryDataStore<>(),
         new InMemoryDataStore<>(),
         new InMemoryDataStore<>());
      datePattern = new UniformDatePattern(
         Instant.ofEpochSecond(5),
         Duration.ofSeconds(7));
   }

   @Test
   void testNewTable() {
      Instant before = Instant.now();
      Table table = Table.createTable();
      Instant after = Instant.now();

      assertThat(table.getName()).isEqualTo("");

      assertThat(table.getDateCreated().getStart())
         .isAfterOrEqualTo(before)
         .isBeforeOrEqualTo(after)
         .isEqualTo(table.getDateCreated().getEnd());
      assertThat(table.getDateLastModified().getStart())
         .isEqualTo(table.getDateCreated().getStart())
         .isEqualTo(table.getDateLastModified().getEnd());
   }

   @Test
   void testLinkUnlinkTasks() throws TaskAccessException {
      Table table = Table.createTable();
      Instant instant = Instant.now();

      assertThat(table.getAllTaskIds(instant)).isEqualTo(Set.of());

      table.linkTask("alpha");
      assertThat(table.getAllTaskIds(instant)).isEqualTo(Set.of("alpha"));

      table.linkTask("beta");
      assertThat(table.getAllTaskIds(instant)).isEqualTo(Set.of("alpha", "beta"));

      table.linkTask("alpha");
      assertThat(table.getAllTaskIds(instant)).isEqualTo(Set.of("alpha", "beta"));

      table.unlinkTask("gamma");
      assertThat(table.getAllTaskIds(instant)).isEqualTo(Set.of("alpha", "beta"));

      table.unlinkTask("alpha");
      assertThat(table.getAllTaskIds(instant)).isEqualTo(Set.of("beta"));
   }

   @Test
   void testLinkUnlinkGenerators() {
      Table table = Table.createTable();
      assertThat(table.getAllGeneratorIds()).isEqualTo(Set.of());

      table.linkGenerator("alpha");
      assertThat(table.getAllGeneratorIds()).isEqualTo(Set.of("alpha"));

      table.linkGenerator("beta");
      assertThat(table.getAllGeneratorIds()).isEqualTo(Set.of("alpha", "beta"));

      table.linkGenerator("alpha");
      assertThat(table.getAllGeneratorIds()).isEqualTo(Set.of("alpha", "beta"));

      table.unlinkGenerator("gamma");
      assertThat(table.getAllGeneratorIds()).isEqualTo(Set.of("alpha", "beta"));

      table.unlinkGenerator("alpha");
      assertThat(table.getAllGeneratorIds()).isEqualTo(Set.of("beta"));
   }

   @Test
   void testToFromJson() throws TaskAccessException {
      Table table = Table.createTable();
      table.registerTaskStore(inMemoryTaskStore);

      Generator generator = Generator.createGenerator(table, "", datePattern);
      inMemoryTaskStore.getGenerators().save(generator);
      String generatorId = generator.getId();

      table.linkTask("alpha");
      table.linkTask("beta");
      table.linkGenerator(generatorId);

      JSONObject json = table.toJson();
      Table newTable = Table.fromJson(json);
      newTable.registerTaskStore(inMemoryTaskStore);

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

      assertThat(newTable.getAllTaskIds(Instant.now())).isEqualTo(Set.of("alpha", "beta"));
      assertThat(newTable.getAllGeneratorIds()).isEqualTo(Set.of(generatorId));
   }

   @Test
   void testToFromJsonNoTasks() throws TaskAccessException {
      Table table = Table.createTable();
      JSONObject json = table.toJson();
      Table newTable = Table.fromJson(json);

      assertThat(newTable.getAllTaskIds(Instant.now())).isEqualTo(Set.of());
      assertThat(newTable.getAllGeneratorIds()).isEqualTo(Set.of());
   }
}
