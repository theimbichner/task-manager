package io.github.theimbichner.task;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.theimbichner.task.time.DatePattern;
import io.github.theimbichner.task.time.UniformDatePattern;

import static org.assertj.core.api.Assertions.*;

public class GeneratorTests {
   static Table table;
   static String generationField;
   static DatePattern datePattern;
   static Instant start;
   static Instant end;

   @BeforeAll
   static void beforeAll() {
      table = Table.createTable();
      generationField = "";
      datePattern = new UniformDatePattern(
         Instant.ofEpochSecond(5),
         Duration.ofSeconds(7));
      start = Instant.ofEpochSecond(0);
      end = Instant.ofEpochSecond(1000);
   }

   @Test
   void testNewGenerator() {
      Instant before = Instant.now();
      Generator generator = Generator.createGenerator(table, generationField, datePattern);
      Instant after = Instant.now();

      assertThat(generator.getName()).isEqualTo("");
      assertThat(generator.getTemplateName()).isEqualTo("");
      assertThat(generator.getTemplateMarkup()).isNull();
      assertThat(generator.getTemplateDuration()).isEqualTo(0);
      assertThat(generator.getGenerationField()).isEqualTo(generationField);

      assertThat(generator.getDateCreated().getStart())
         .isAfterOrEqualTo(before)
         .isBeforeOrEqualTo(after)
         .isEqualTo(generator.getDateCreated().getEnd());
      assertThat(generator.getDateLastModified().getStart())
         .isEqualTo(generator.getDateCreated().getStart())
         .isEqualTo(generator.getDateLastModified().getStart());

      assertThat(generator.getGenerationDatePattern()).isEqualTo(datePattern);
   }

   @Test
   void testToFromJson() {
      Generator generator = Generator.createGenerator(table, generationField, datePattern);
      Generator newGenerator = Generator.fromJson(generator.toJson());

      assertThat(newGenerator.getId()).isEqualTo(generator.getId());
      assertThat(newGenerator.getName()).isEqualTo(generator.getName());
      assertThat(newGenerator.getTemplateName()).isEqualTo(generator.getTemplateName());
      assertThat(newGenerator.getTemplateMarkup()).isEqualTo(generator.getTemplateMarkup());
      assertThat(newGenerator.getTemplateDuration()).isEqualTo(generator.getTemplateDuration());
      assertThat(newGenerator.getGenerationField()).isEqualTo(generator.getGenerationField());

      assertThat(newGenerator.getDateCreated().getStart())
         .isEqualTo(generator.getDateCreated().getStart());
      assertThat(newGenerator.getDateCreated().getEnd())
         .isEqualTo(generator.getDateCreated().getEnd());
      assertThat(newGenerator.getDateLastModified().getStart())
         .isEqualTo(generator.getDateLastModified().getStart());
      assertThat(newGenerator.getDateLastModified().getEnd())
         .isEqualTo(generator.getDateLastModified().getEnd());

      assertThat(newGenerator.getGenerationDatePattern().getDates(start, end))
         .isEqualTo(generator.getGenerationDatePattern().getDates(start, end));
   }
}
