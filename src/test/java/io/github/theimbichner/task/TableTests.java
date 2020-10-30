package io.github.theimbichner.task;

import java.util.Set;

import org.json.JSONObject;

import org.junit.jupiter.api.Test;

import io.github.theimbichner.task.time.DateTime;

import static org.assertj.core.api.Assertions.*;

public class TableTests {
   @Test
   void testNewTable() {
      DateTime before = new DateTime();
      Table table = new Table();
      DateTime after = new DateTime();

      assertThat(table.getName()).isEqualTo("");

      assertThat(before.getStart()).isBeforeOrEqualTo(table.getDateCreated().getStart());
      assertThat(after.getStart()).isAfterOrEqualTo(table.getDateCreated().getEnd());

      assertThat(before.getStart()).isBeforeOrEqualTo(table.getDateLastModified().getStart());
      assertThat(after.getStart()).isAfterOrEqualTo(table.getDateLastModified().getEnd());
   }

   @Test
   void testLinkUnlinkTasks() {
      Table table = new Table();
      assertThat(table.getAllTaskIds()).isEqualTo(Set.of());

      table.linkTask("alpha");
      assertThat(table.getAllTaskIds()).isEqualTo(Set.of("alpha"));

      table.linkTask("beta");
      assertThat(table.getAllTaskIds()).isEqualTo(Set.of("alpha", "beta"));

      table.linkTask("alpha");
      assertThat(table.getAllTaskIds()).isEqualTo(Set.of("alpha", "beta"));

      table.unlinkTask("gamma");
      assertThat(table.getAllTaskIds()).isEqualTo(Set.of("alpha", "beta"));

      table.unlinkTask("alpha");
      assertThat(table.getAllTaskIds()).isEqualTo(Set.of("beta"));
   }

   @Test
   void testLinkUnlinkGenerators() {
      Table table = new Table();
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
   void testToFromJson() {
      Table table = new Table();
      table.linkTask("alpha");
      table.linkTask("beta");
      table.linkGenerator("gamma");

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

      assertThat(newTable.getAllTaskIds()).isEqualTo(Set.of("alpha", "beta"));
      assertThat(newTable.getAllGeneratorIds()).isEqualTo(Set.of("gamma"));
   }

   @Test
   void testToFromJsonNoTasks() {
      Table table = new Table();
      JSONObject json = table.toJson();
      Table newTable = Table.fromJson(json);

      assertThat(newTable.getAllTaskIds()).isEqualTo(Set.of());
      assertThat(newTable.getAllGeneratorIds()).isEqualTo(Set.of());
   }
}
