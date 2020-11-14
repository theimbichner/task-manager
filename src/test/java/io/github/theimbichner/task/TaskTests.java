package io.github.theimbichner.task;

import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class TaskTests {
   static Table table;

   @BeforeAll
   static void beforeAll() {
      table = Table.createTable();
   }

   @Test
   void testNewTask() {
      Instant before = Instant.now();
      Task task = Task.createTask(table);
      Instant after = Instant.now();

      assertThat(task.getName()).isEqualTo("");

      assertThat(task.getDateCreated().getStart())
         .isAfterOrEqualTo(before)
         .isBeforeOrEqualTo(after)
         .isEqualTo(task.getDateCreated().getEnd());
      assertThat(task.getDateLastModified().getStart())
         .isEqualTo(task.getDateCreated().getStart())
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(task.getMarkup()).isNull();
   }

   @Test
   void testToFromJson() {
      Task task = Task.createTask(table);
      Task newTask = Task.fromJson(task.toJson());

      assertThat(newTask.getId()).isEqualTo(task.getId());
      assertThat(newTask.getName()).isEqualTo(task.getName());

      assertThat(newTask.getDateCreated().getStart())
         .isEqualTo(task.getDateCreated().getStart());
      assertThat(newTask.getDateCreated().getEnd())
         .isEqualTo(task.getDateCreated().getEnd());
      assertThat(newTask.getDateLastModified().getStart())
         .isEqualTo(task.getDateLastModified().getStart());
      assertThat(newTask.getDateLastModified().getEnd())
         .isEqualTo(task.getDateLastModified().getEnd());

      assertThat(newTask.getMarkup()).isEqualTo(task.getMarkup());
   }
}
