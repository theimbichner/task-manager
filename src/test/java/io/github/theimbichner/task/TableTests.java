package io.github.theimbichner.task;

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
   void testToFromJson() {
      Table table = new Table();
      JSONObject json = table.toJson();
      Table newTable = Table.fromJson(json);

      assertThat(newTable.getName()).isEqualTo(table.getName());

      assertThat(newTable.getDateCreated().getStart())
         .isEqualTo(table.getDateCreated().getStart());
      assertThat(newTable.getDateCreated().getEnd())
         .isEqualTo(table.getDateCreated().getEnd());
      assertThat(newTable.getDateLastModified().getStart())
         .isEqualTo(table.getDateLastModified().getStart());
      assertThat(newTable.getDateLastModified().getEnd())
         .isEqualTo(table.getDateLastModified().getEnd());
   }
}
