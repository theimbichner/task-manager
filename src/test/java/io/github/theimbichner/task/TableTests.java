package io.github.theimbichner.task;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

import org.json.JSONObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.task.io.InMemoryDataStore;
import io.github.theimbichner.task.io.TaskStore;
import io.github.theimbichner.task.time.DatePattern;
import io.github.theimbichner.task.time.UniformDatePattern;

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
   void testLinkUnlinkTasks() {
      Table table = Table.createTable();
      Instant instant = Instant.now();

      assertThat(table.getAllTaskIds(instant).get()).isEqualTo(Set.of());

      table.linkTask("alpha");
      assertThat(table.getAllTaskIds(instant).get()).isEqualTo(Set.of("alpha"));

      table.linkTask("beta");
      assertThat(table.getAllTaskIds(instant).get()).isEqualTo(Set.of("alpha", "beta"));

      table.linkTask("alpha");
      assertThat(table.getAllTaskIds(instant).get()).isEqualTo(Set.of("alpha", "beta"));

      table.unlinkTask("gamma");
      assertThat(table.getAllTaskIds(instant).get()).isEqualTo(Set.of("alpha", "beta"));

      table.unlinkTask("alpha");
      assertThat(table.getAllTaskIds(instant).get()).isEqualTo(Set.of("beta"));
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
   void testGetAllTaskIdsWithGenerators() {
      Table table = Table.createTable();
      table.registerTaskStore(taskStore);

      Instant start = Instant.now();
      Instant end = start.plusSeconds(600);

      int numTasks = Stream.of(getDatePattern(7), getDatePattern(13))
         .peek(pattern -> {
            Generator generator = Generator.createGenerator(table, "", pattern);
            taskStore.getGenerators().save(generator).get();
            table.linkGenerator(generator.getId());
         })
         .mapToInt(pattern -> pattern.getDates(start, end).size())
         .sum();

      assertThat(table.getAllTaskIds(start).get()).isEmpty();
      assertThat(table.getAllTaskIds(end).get().size()).isEqualTo(numTasks);
   }

   @Test
   void testToFromJson() {
      Table table = Table.createTable();
      table.registerTaskStore(taskStore);

      Generator generator = Generator.createGenerator(table, "", getDatePattern(7));
      taskStore.getGenerators().save(generator).get();
      String generatorId = generator.getId();

      table.linkTask("alpha");
      table.linkTask("beta");
      table.linkGenerator(generatorId);

      JSONObject json = table.toJson();
      Table newTable = Table.fromJson(json);
      newTable.registerTaskStore(taskStore);

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

      assertThat(newTable.getAllTaskIds(Instant.now()).get()).isEqualTo(Set.of("alpha", "beta"));
      assertThat(newTable.getAllGeneratorIds()).isEqualTo(Set.of(generatorId));
   }

   @Test
   void testToFromJsonNoTasks() {
      Table table = Table.createTable();
      JSONObject json = table.toJson();
      Table newTable = Table.fromJson(json);

      assertThat(newTable.getAllTaskIds(Instant.now()).get()).isEqualTo(Set.of());
      assertThat(newTable.getAllGeneratorIds()).isEqualTo(Set.of());
   }
}
