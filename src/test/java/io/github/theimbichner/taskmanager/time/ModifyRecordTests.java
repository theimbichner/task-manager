package io.github.theimbichner.taskmanager.time;

import java.time.Instant;

import org.json.JSONObject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class ModifyRecordTests {
   @Test
   public void testCreatedNow() {
      Instant before = Instant.now();
      ModifyRecord modifyRecord = ModifyRecord.createdNow();
      Instant after = Instant.now();

      assertThat(modifyRecord.getDateCreated())
         .isEqualTo(modifyRecord.getDateLastModified())
         .isAfterOrEqualTo(before)
         .isBeforeOrEqualTo(after);
   }

   @Test
   public void testUpdatedNow() throws InterruptedException {
      ModifyRecord modifyRecord = ModifyRecord.createdNow();
      Instant oldCreated = modifyRecord.getDateCreated();

      Thread.sleep(500);
      Instant before = Instant.now();
      modifyRecord = modifyRecord.updatedNow();
      Instant after = Instant.now();

      assertThat(modifyRecord.getDateCreated()).isEqualTo(oldCreated);
      assertThat(modifyRecord.getDateLastModified())
         .isAfterOrEqualTo(before)
         .isBeforeOrEqualTo(after);
   }

   @Test
   public void testToFromJson() {
      ModifyRecord modifyRecord = ModifyRecord.createdNow();
      JSONObject json = new JSONObject();
      modifyRecord.writeIntoJson(json);
      ModifyRecord newModifyRecord = ModifyRecord.readFromJson(json);

      assertThat(newModifyRecord.getDateCreated()).isEqualTo(modifyRecord.getDateCreated());
      assertThat(newModifyRecord.getDateLastModified()).isEqualTo(modifyRecord.getDateLastModified());
   }
}
