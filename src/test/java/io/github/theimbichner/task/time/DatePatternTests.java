package io.github.theimbichner.task.time;

import org.json.JSONObject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class DatePatternTests {
   @Test
   void testInvalidFromJson() {
      JSONObject json = new JSONObject();
      json.put("patternType", "invalid");
      assertThatExceptionOfType(RuntimeException.class)
         .isThrownBy(() -> DatePattern.fromJson(json));
   }

   @Test
   void testInvalidFromEmptyJson() {
      JSONObject json = new JSONObject();
      assertThatExceptionOfType(RuntimeException.class)
         .isThrownBy(() -> DatePattern.fromJson(json));
   }
}
