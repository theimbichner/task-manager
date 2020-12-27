package io.github.theimbichner.task;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

      assertThat(table.getAllTaskIds().asList()).isEmpty();

      table.linkTask("alpha");
      assertThat(table.getAllTaskIds().asList()).isEqualTo(List.of("alpha"));

      table.linkTask("beta");
      assertThat(table.getAllTaskIds().asList()).isEqualTo(List.of("alpha", "beta"));

      table.linkTask("alpha");
      assertThat(table.getAllTaskIds().asList()).isEqualTo(List.of("alpha", "beta"));

      table.unlinkTask("gamma");
      assertThat(table.getAllTaskIds().asList()).isEqualTo(List.of("alpha", "beta"));

      table.unlinkTask("alpha");
      assertThat(table.getAllTaskIds().asList()).isEqualTo(List.of("beta"));
   }

   @Test
   void testLinkUnlinkGenerators() {
      Table table = Table.createTable();
      assertThat(table.getAllGeneratorIds().asList()).isEmpty();

      table.linkGenerator("alpha");
      assertThat(table.getAllGeneratorIds().asList()).isEqualTo(List.of("alpha"));

      table.linkGenerator("beta");
      assertThat(table.getAllGeneratorIds().asList()).isEqualTo(List.of("alpha", "beta"));

      table.linkGenerator("alpha");
      assertThat(table.getAllGeneratorIds().asList()).isEqualTo(List.of("alpha", "beta"));

      table.unlinkGenerator("gamma");
      assertThat(table.getAllGeneratorIds().asList()).isEqualTo(List.of("alpha", "beta"));

      table.unlinkGenerator("alpha");
      assertThat(table.getAllGeneratorIds().asList()).isEqualTo(List.of("beta"));
   }

   @Test
   void testToFromJson() {
      Table table = Table.createTable();
      table.setTaskStore(taskStore);

      Generator generator = Generator.createGenerator(table, "", getDatePattern(7));
      taskStore.getGenerators().save(generator).get();
      String generatorId = generator.getId();

      table.linkTask("alpha");
      table.linkTask("beta");
      table.linkGenerator(generatorId);

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

      assertThat(newTable.getAllTaskIds().asList()).isEqualTo(List.of("alpha", "beta"));
      assertThat(newTable.getAllGeneratorIds().asList()).isEqualTo(List.of(generatorId));
   }

   @Test
   void testToFromJsonNoTasks() {
      Table table = Table.createTable();
      JSONObject json = table.toJson();
      Table newTable = Table.fromJson(json);

      assertThat(newTable.getAllTaskIds().asList()).isEmpty();
      assertThat(newTable.getAllGeneratorIds().asList()).isEmpty();
   }
}
