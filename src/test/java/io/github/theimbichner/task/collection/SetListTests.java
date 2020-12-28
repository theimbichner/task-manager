package io.github.theimbichner.task.collection;

import java.util.List;
import java.util.NoSuchElementException;

import io.vavr.Tuple2;

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
      assertThat(setList.contains("alpha")).isFalse();
      setList = setList.add("alpha");
      assertThat(setList.contains("alpha")).isTrue();
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
      assertThat(setList.contains("alpha")).isTrue();
      assertThat(setList.contains("beta")).isTrue();
      assertThat(setList.contains("gamma")).isTrue();
      assertThat(setList.asList()).isEqualTo(List.of("alpha", "gamma", "beta"));
   }

   @Test
   void testRemove() {
      setList = setList.add("alpha");
      assertThat(setList.contains("alpha")).isTrue();
      setList = setList.remove("alpha");
      assertThat(setList.contains("alpha")).isFalse();
      assertThat(setList.asList()).isEmpty();
   }

   @Test
   void testRemoveAddRemove() {
      setList = setList.add("alpha").add("beta").remove("alpha").add("alpha").remove("alpha");
      assertThat(setList.contains("alpha")).isFalse();
      assertThat(setList.contains("beta")).isTrue();
      assertThat(setList.asList()).isEqualTo(List.of("beta"));
   }

   @Test
   void testRemoveAbsent() {
      setList = setList.add("alpha").add("beta");
      assertThat(setList.contains("gamma")).isFalse();
      setList = setList.remove("gamma");
      assertThat(setList.contains("gamma")).isFalse();
      assertThat(setList.asList()).isEqualTo(List.of("alpha", "beta"));
   }

   @Test
   void testAddAll() {
      setList = setList.addAll(List.of("alpha", "beta", "gamma", "beta"));
      assertThat(setList.asList()).isEqualTo(List.of("alpha", "beta", "gamma"));
   }

   @Test
   void testAddRemoveAddAll() {
      setList = setList
         .addAll(List.of("alpha", "beta", "gamma"))
         .remove("beta")
         .addAll(List.of("beta", "gamma"));
      assertThat(setList.asList()).isEqualTo(List.of("alpha", "gamma", "beta"));
   }

   @Test
   void testSplitBeginning() {
      setList = setList.addAll(List.of("alpha", "beta", "gamma"));
      Tuple2<List<String>, List<String>> split = setList.split("alpha");
      assertThat(split._1).isEqualTo(List.of());
      assertThat(split._2).isEqualTo(List.of("alpha", "beta", "gamma"));
   }

   @Test
   void testSplitMiddle() {
      setList = setList.addAll(List.of("alpha", "beta", "gamma"));
      Tuple2<List<String>, List<String>> split = setList.split("beta");
      assertThat(split._1).isEqualTo(List.of("alpha"));
      assertThat(split._2).isEqualTo(List.of("beta", "gamma"));
   }

   @Test
   void testSplitEnd() {
      setList = setList.addAll(List.of("alpha", "beta", "gamma"));
      Tuple2<List<String>, List<String>> split = setList.split("gamma");
      assertThat(split._1).isEqualTo(List.of("alpha", "beta"));
      assertThat(split._2).isEqualTo(List.of("gamma"));
   }

   @Test
   void testSplitInvalid() {
      setList = setList.addAll(List.of("alpha", "beta", "gamma"));
      assertThatExceptionOfType(NoSuchElementException.class)
         .isThrownBy(() -> setList.split("delta"));
   }
}
