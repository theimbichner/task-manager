package io.github.theimbichner.task.collection;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class SetListTests {
   SetList<String> setList;

   @BeforeEach
   void beforeEach() {
      setList = SetList.empty();
   }

   @Test
   void testEmpty() {
      assertThat(SetList.empty().asList()).isEqualTo(List.of());
   }

   @Test
   void testAdd() {
      setList = setList.add("alpha");
      assertThat(setList.asList()).isEqualTo(List.of("alpha"));
   }

   @Test
   void testAddDuplicate() {
      setList = setList.add("alpha").add("alpha");
      assertThat(setList.asList()).isEqualTo(List.of("alpha"));
   }

   @Test
   void testAddDuplicateOrder() {
      setList = setList.add("alpha").add("beta").add("gamma").add("beta");
      assertThat(setList.asList()).isEqualTo(List.of("alpha", "beta", "gamma"));
   }

   @Test
   void testAddRemoveAdd() {
      setList = setList.add("alpha").add("beta").add("gamma").remove("beta").add("beta");
      assertThat(setList.asList()).isEqualTo(List.of("alpha", "gamma", "beta"));
   }

   @Test
   void testRemove() {
      setList = setList.add("alpha").remove("alpha");
      assertThat(setList.asList()).isEmpty();
   }

   @Test
   void testRemoveAddRemove() {
      setList = setList.add("alpha").add("beta").remove("alpha").add("alpha").remove("alpha");
      assertThat(setList.asList()).isEqualTo(List.of("beta"));
   }

   @Test
   void testRemoveAbsent() {
      setList = setList.add("alpha").add("beta").remove("gamma");
      assertThat(setList.asList()).isEqualTo(List.of("alpha", "beta"));
   }
}
