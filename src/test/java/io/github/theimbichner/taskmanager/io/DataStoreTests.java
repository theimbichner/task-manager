package io.github.theimbichner.taskmanager.io;

import java.util.Comparator;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.vavr.api.VavrAssertions.*;

public class DataStoreTests {
   private static class StringStorable implements Storable<String> {
      private final String id;
      private final String value;

      public StringStorable(String id, String value) {
         this.id = id;
         this.value = value;
      }

      public static StringStorable random() {
         return new StringStorable(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString());
      }

      @Override
      public String getId() {
         return id;
      }

      public String getValue() {
         return value;
      }
   }

   private static Stream<Arguments> provideData() {
      return Stream.of(
         Arguments.of(
            new InMemoryDataStore<String, StringStorable>(),
            new StringStorable("alpha", "alpha"),
            Comparator.comparing(StringStorable::getValue),
            new StringStorable("alpha", "beta"),
            (Supplier<StringStorable>) StringStorable::random));
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWrite(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator
   ) {
      assertThat(dataStore.save(value))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testRead(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.getById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testDelete(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.deleteById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteRead(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator
   ) {
      assertThat(dataStore.save(value)).isRight();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitRead(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.commit()).isRight();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitCancelRead(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.commit()).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelRead(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.getById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelCommitRead(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      dataStore.cancelTransaction();
      assertThat(dataStore.commit()).isRight();

      assertThat(dataStore.getById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDelete(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();

      assertThat(dataStore.deleteById(value.getId())).isRight();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitDelete(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.commit()).isRight();

      assertThat(dataStore.deleteById(value.getId())).isRight();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitCancelDelete(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.commit()).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.deleteById(value.getId())).isRight();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelDelete(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.deleteById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelCommitDelete(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      dataStore.cancelTransaction();
      assertThat(dataStore.commit()).isRight();

      assertThat(dataStore.deleteById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteDelete(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();

      assertThat(dataStore.deleteById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteCommitCancelDelete(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      assertThat(dataStore.commit()).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.deleteById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteCancelDelete(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.deleteById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteRead(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();

      assertThat(dataStore.getById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteCommitCancelRead(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      assertThat(dataStore.commit()).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.getById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteCancelRead(
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.getById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteOverwrite(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitOverwrite(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.commit()).isRight();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitCancelOverwrite(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.commit()).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelOverwrite(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelCommitOverwrite(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      dataStore.cancelTransaction();
      assertThat(dataStore.commit()).isRight();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteOverwriteRead(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.save(overwrite)).isRight();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitCancelOverwriteRead(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.commit()).isRight();
      dataStore.cancelTransaction();
      assertThat(dataStore.save(overwrite)).isRight();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelOverwriteRead(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      dataStore.cancelTransaction();
      assertThat(dataStore.save(overwrite)).isRight();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteOverwriteCommitCancelRead(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.save(overwrite)).isRight();
      assertThat(dataStore.commit()).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteOverwriteCancelRead(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.save(overwrite)).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.getById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteOverwrite(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteCommitCancelOverwrite(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      assertThat(dataStore.commit()).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteCancelOverwrite(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      dataStore.cancelTransaction();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteOverwriteRead(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      assertThat(dataStore.save(overwrite)).isRight();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testFailureStateInvalidRead(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite,
      Supplier<V> supplier
   ) {
      assertThat(dataStore.save(value)).isRight();

      V v = supplier.get();
      assertThat(dataStore.save(v)).isRight();
      assertThat(dataStore.deleteById(v.getId())).isRight();
      assertThat(dataStore.getById(v.getId())).isLeft();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testFailureStateInvalidDelete(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite,
      Supplier<V> supplier
   ) {
      assertThat(dataStore.save(value)).isRight();

      V v = supplier.get();
      assertThat(dataStore.deleteById(v.getId())).isLeft();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testFailureRollbackInvalidRead(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite,
      Supplier<V> supplier
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.commit()).isRight();
      assertThat(dataStore.save(overwrite)).isRight();

      V v = supplier.get();
      assertThat(dataStore.getById(v.getId())).isLeft();
      dataStore.cancelTransaction();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testFailureRollbackInvalidDelete(
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite,
      Supplier<V> supplier
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.commit()).isRight();
      assertThat(dataStore.save(overwrite)).isRight();

      V v = supplier.get();
      assertThat(dataStore.save(v)).isRight();
      assertThat(dataStore.deleteById(v.getId())).isRight();
      assertThat(dataStore.deleteById(v.getId())).isLeft();
      dataStore.cancelTransaction();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }
}
