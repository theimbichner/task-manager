package io.github.theimbichner.task.collection;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class SetListTests {
   @Test
   void testEmpty() {
      assertThat(SetList.empty().asList()).isEqualTo(List.of());
   }
}
