package io.github.theimbichner.taskmanager.collection;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

import io.vavr.Tuple2;
import io.vavr.collection.Vector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.vavr.api.VavrAssertions.*;

public class SetListTests {
   SetList<String> setList;

   @BeforeEach
   void beforeEach() {
      setList = SetList.empty();
   }

   @Test
   void testEmpty() {
      assertThat(SetList.empty().asList()).isEmpty();
   }

   @Test
   void testAdd() {
      assertThat(setList.contains("alpha")).isFalse();
      setList = setList.add("alpha");
      assertThat(setList.contains("alpha")).isTrue();
      assertThat(setList.asList()).isEqualTo(Vector.of("alpha"));
   }

   @Test
   void testAddDuplicate() {
      setList = setList.add("alpha").add("alpha");
      assertThat(setList.asList()).isEqualTo(Vector.of("alpha"));
   }

   @Test
   void testAddDuplicateOrder() {
      setList = setList.add("alpha").add("beta").add("gamma").add("beta");
      assertThat(setList.asList()).isEqualTo(Vector.of("alpha", "beta", "gamma"));
   }

   @Test
   void testAddRemoveAdd() {
      setList = setList.add("alpha").add("beta").add("gamma").remove("beta").add("beta");
      assertThat(setList.contains("alpha")).isTrue();
      assertThat(setList.contains("beta")).isTrue();
      assertThat(setList.contains("gamma")).isTrue();
      assertThat(setList.asList()).isEqualTo(Vector.of("alpha", "gamma", "beta"));
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
      assertThat(setList.asList()).isEqualTo(Vector.of("beta"));
   }

   @Test
   void testRemoveAbsent() {
      setList = setList.add("alpha").add("beta");
      assertThat(setList.contains("gamma")).isFalse();
      setList = setList.remove("gamma");
      assertThat(setList.contains("gamma")).isFalse();
      assertThat(setList.asList()).isEqualTo(Vector.of("alpha", "beta"));
   }

   @Test
   void testAddAll() {
      setList = setList.addAll(Vector.of("alpha", "beta", "gamma", "beta"));
      assertThat(setList.asList()).isEqualTo(Vector.of("alpha", "beta", "gamma"));
   }

   @Test
   void testRemoveAll() {
      setList = setList
         .addAll(Vector.of("alpha", "beta", "gamma"))
         .removeAll(Vector.of("alpha", "gamma", "delta"));
      assertThat(setList.asList()).isEqualTo(Vector.of("beta"));
   }

   @Test
   void testAddRemoveAddAll() {
      setList = setList
         .addAll(Vector.of("alpha", "beta", "gamma"))
         .remove("beta")
         .addAll(Vector.of("beta", "gamma"));
      assertThat(setList.asList()).isEqualTo(Vector.of("alpha", "gamma", "beta"));
   }

   @Test
   void testRemoveAddRemoveAll() {
      setList = setList
         .addAll(Vector.of("alpha", "beta", "gamma"))
         .removeAll(Vector.of("beta", "gamma"))
         .add("gamma")
         .removeAll(Vector.of("gamma", "delta"));
      assertThat(setList.asList()).isEqualTo(Vector.of("alpha"));
   }

   @ParameterizedTest
   @MethodSource
   void testEquals(Object left, Object right, boolean equals) {
      if (equals) {
         assertThat(left).isEqualTo(right);
         assertThat(left).hasSameHashCodeAs(right);
      }
      else {
         assertThat(left).isNotEqualTo(right);
      }
   }

   private static Stream<Arguments> testEquals() {
      SetList<String> list = SetList.<String>empty().add("alpha").add("beta");
      SetList<String> listCopy = SetList.<String>empty().add("alpha").remove("gamma").add("beta");

      return Stream.of(
         Arguments.of(SetList.empty(), SetList.empty(), true),
         Arguments.of(SetList.empty(), list, false),
         Arguments.of(list, SetList.empty(), false),
         Arguments.of(SetList.empty(), null, false),
         Arguments.of(SetList.empty(), "alpha", false),
         Arguments.of(list, list, true),
         Arguments.of(list, listCopy, true),
         Arguments.of(listCopy, list, true));
   }

   @Test
   void testSplitBeginning() {
      setList = setList.addAll(Vector.of("alpha", "beta", "gamma"));
      Tuple2<Vector<String>, Vector<String>> split = setList.split("alpha");
      assertThat(split._1).isEqualTo(Vector.of());
      assertThat(split._2).isEqualTo(Vector.of("alpha", "beta", "gamma"));
   }

   @Test
   void testSplitMiddle() {
      setList = setList.addAll(Vector.of("alpha", "beta", "gamma"));
      Tuple2<Vector<String>, Vector<String>> split = setList.split("beta");
      assertThat(split._1).isEqualTo(Vector.of("alpha"));
      assertThat(split._2).isEqualTo(Vector.of("beta", "gamma"));
   }

   @Test
   void testSplitEnd() {
      setList = setList.addAll(Vector.of("alpha", "beta", "gamma"));
      Tuple2<Vector<String>, Vector<String>> split = setList.split("gamma");
      assertThat(split._1).isEqualTo(Vector.of("alpha", "beta"));
      assertThat(split._2).isEqualTo(Vector.of("gamma"));
   }

   @Test
   void testSplitInvalid() {
      setList = setList.addAll(Vector.of("alpha", "beta", "gamma"));
      assertThatExceptionOfType(NoSuchElementException.class)
         .isThrownBy(() -> setList.split("delta"));
   }
}
