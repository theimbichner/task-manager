package io.github.theimbichner.taskmanager.io;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.vavr.collection.HashSet;

import org.json.JSONObject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.theimbichner.taskmanager.task.TypeAdapter;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.vavr.api.VavrAssertions.*;

public class DataStoreTests {
   private static final File TEST_ROOT = new File("./DataStoreTests/");

   private static TypeAdapter<StringStorable, JSONObject> stringStorableJsonAdapter() {
      return new TypeAdapter<>(
         s -> {
            JSONObject json = new JSONObject();
            json.put("id", s.getId());
            json.put("value", s.getValue());

            return json;
         },
         json -> {
            String id = json.getString("id");
            String value = json.getString("value");

            return new StringStorable(id, value);
         });
   }

   public static StringStorable randomStringStorable() {
      return new StringStorable(
         UUID.randomUUID().toString(),
         UUID.randomUUID().toString());
   }

   private static File getRandomFolder() {
      return new File(TEST_ROOT, UUID.randomUUID().toString());
   }

   private static Stream<Arguments> provideData() throws IOException {
      InMemoryDataStore<String, StringStorable> inMemory = new InMemoryDataStore<>();
      FileDataStore fileBased = new FileDataStore(getRandomFolder(), ".txt");

      return Stream.of(
         Arguments.of(
            inMemory,
            inMemory.getChannel("alpha"),
            new StringStorable("alpha", "alpha"),
            Comparator.comparing(StringStorable::getValue),
            new StringStorable("alpha", "beta"),
            (Supplier<StringStorable>) DataStoreTests::randomStringStorable),
         Arguments.of(
            inMemory,
            new CachedDataStore<>(inMemory.getChannel("beta"), 5),
            new StringStorable("alpha", "alpha"),
            Comparator.comparing(StringStorable::getValue),
            new StringStorable("alpha", "beta"),
            (Supplier<StringStorable>) DataStoreTests::randomStringStorable),
         Arguments.of(
            fileBased,
            fileBased.getChannel("alpha"),
            new StringStorable("alpha", "alpha"),
            Comparator.comparing(StringStorable::getValue),
            new StringStorable("alpha", "beta"),
            (Supplier<StringStorable>) DataStoreTests::randomStringStorable),
         Arguments.of(
            inMemory,
            new JsonAdapterDataStore<>(
               inMemory.getChannel("gamma"),
               stringStorableJsonAdapter(),
               TypeAdapter.identity()),
            new StringStorable("alpha", "alpha"),
            Comparator.comparing(StringStorable::getValue),
            new StringStorable("alpha", "beta"),
            (Supplier<StringStorable>) DataStoreTests::randomStringStorable));
   }

   @AfterAll
   static void afterAll() throws IOException {
      IOUtils.deleteFolder(TEST_ROOT);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testListEmpty(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore
   ) {
      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.empty());
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWrite(
      MultiChannelDataStore<String, StringStorable> base,
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
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.getById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testDelete(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.deleteById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.of(value.getId()));
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(base.commit()).isRight();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.of(value.getId()));
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitCancelList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(base.commit()).isRight();
      base.cancelTransaction();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.of(value.getId()));
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      base.cancelTransaction();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.empty());
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelCommitList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      base.cancelTransaction();
      assertThat(base.commit()).isRight();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.empty());
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteRead(
      MultiChannelDataStore<String, StringStorable> base,
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
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(base.commit()).isRight();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitCancelRead(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(base.commit()).isRight();
      base.cancelTransaction();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelRead(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      base.cancelTransaction();

      assertThat(dataStore.getById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelCommitRead(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      base.cancelTransaction();
      assertThat(base.commit()).isRight();

      assertThat(dataStore.getById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDelete(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();

      assertThat(dataStore.deleteById(value.getId())).isRight();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitDelete(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(base.commit()).isRight();

      assertThat(dataStore.deleteById(value.getId())).isRight();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitCancelDelete(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(base.commit()).isRight();
      base.cancelTransaction();

      assertThat(dataStore.deleteById(value.getId())).isRight();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelDelete(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      base.cancelTransaction();

      assertThat(dataStore.deleteById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelCommitDelete(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      base.cancelTransaction();
      assertThat(base.commit()).isRight();

      assertThat(dataStore.deleteById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.empty());
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteCommitCancelList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      assertThat(base.commit()).isRight();
      base.cancelTransaction();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.empty());
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteCancelList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      base.cancelTransaction();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.empty());
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitDeleteCommitCancelList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(base.commit()).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      assertThat(base.commit()).isRight();
      base.cancelTransaction();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.empty());
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitDeleteCancelList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(base.commit()).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      base.cancelTransaction();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.of(value.getId()));
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteDelete(
      MultiChannelDataStore<String, StringStorable> base,
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
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      assertThat(base.commit()).isRight();
      base.cancelTransaction();

      assertThat(dataStore.deleteById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteCancelDelete(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      base.cancelTransaction();

      assertThat(dataStore.deleteById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteRead(
      MultiChannelDataStore<String, StringStorable> base,
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
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      assertThat(base.commit()).isRight();
      base.cancelTransaction();

      assertThat(dataStore.getById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteCancelRead(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      base.cancelTransaction();

      assertThat(dataStore.getById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteOverwrite(
      MultiChannelDataStore<String, StringStorable> base,
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
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(base.commit()).isRight();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitCancelOverwrite(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(base.commit()).isRight();
      base.cancelTransaction();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelOverwrite(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      base.cancelTransaction();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelCommitOverwrite(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      base.cancelTransaction();
      assertThat(base.commit()).isRight();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteOverwriteList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.save(overwrite)).isRight();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.of(value.getId()));
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteOverwriteCommitCancelList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.save(overwrite)).isRight();
      assertThat(base.commit()).isRight();
      base.cancelTransaction();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.of(value.getId()));
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteOverwriteCancelList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.save(overwrite)).isRight();
      base.cancelTransaction();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.empty());
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitOverwriteCommitCancelList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(base.commit()).isRight();
      assertThat(dataStore.save(overwrite)).isRight();
      assertThat(base.commit()).isRight();
      base.cancelTransaction();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.of(value.getId()));
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCommitOverwriteCancelList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(base.commit()).isRight();
      assertThat(dataStore.save(overwrite)).isRight();
      base.cancelTransaction();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.of(value.getId()));
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteOverwriteRead(
      MultiChannelDataStore<String, StringStorable> base,
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
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(base.commit()).isRight();
      base.cancelTransaction();
      assertThat(dataStore.save(overwrite)).isRight();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteCancelOverwriteRead(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      base.cancelTransaction();
      assertThat(dataStore.save(overwrite)).isRight();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteOverwriteCommitCancelRead(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.save(overwrite)).isRight();
      assertThat(base.commit()).isRight();
      base.cancelTransaction();

      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteOverwriteCancelRead(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.save(overwrite)).isRight();
      base.cancelTransaction();

      assertThat(dataStore.getById(value.getId())).isLeft();
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteOverwrite(
      MultiChannelDataStore<String, StringStorable> base,
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
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      assertThat(base.commit()).isRight();
      base.cancelTransaction();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteCancelOverwrite(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      base.cancelTransaction();

      assertThat(dataStore.save(overwrite))
         .usingValueComparator(comparator)
         .containsOnRight(overwrite);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteOverwriteList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite
   ) {
      assertThat(dataStore.save(value)).isRight();
      assertThat(dataStore.deleteById(value.getId())).isRight();
      assertThat(dataStore.save(overwrite)).isRight();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.of(value.getId()));
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testWriteDeleteOverwriteRead(
      MultiChannelDataStore<String, StringStorable> base,
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
   <K, V extends Storable<K>> void testMultiWriteList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite,
      Supplier<V> supplier
   ) {
      HashSet<K> expected = HashSet.empty();
      for (int i = 0; i < 100; i++) {
         V v = supplier.get();
         expected = expected.add(v.getId());
         assertThat(dataStore.save(v)).isRight();
      }

      assertThat(dataStore.listIds())
         .containsOnRight(expected);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testMultiWriteCommitCancelList(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite,
      Supplier<V> supplier
   ) {
      HashSet<K> expected = HashSet.empty();
      for (int i = 0; i < 100; i++) {
         V v = supplier.get();
         expected = expected.add(v.getId());
         assertThat(dataStore.save(v)).isRight();
      }
      assertThat(base.commit()).isRight();

      for (int i = 0; i < 100; i++) {
         V v = supplier.get();
         assertThat(dataStore.save(v)).isRight();
      }
      base.cancelTransaction();

      assertThat(dataStore.listIds())
         .containsOnRight(expected);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testFailureStateInvalidRead(
      MultiChannelDataStore<String, StringStorable> base,
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

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.of(value.getId()));
      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testFailureStateInvalidDelete(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite,
      Supplier<V> supplier
   ) {
      assertThat(dataStore.save(value)).isRight();

      V v = supplier.get();
      assertThat(dataStore.deleteById(v.getId())).isLeft();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.of(value.getId()));
      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testFailureRollbackInvalidRead(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite,
      Supplier<V> supplier
   ) {
      assertThat(dataStore.save(value)).isRight();
      V v1 = supplier.get();
      assertThat(dataStore.save(v1)).isRight();
      assertThat(base.commit()).isRight();
      assertThat(dataStore.save(overwrite)).isRight();

      V v2 = supplier.get();
      assertThat(dataStore.getById(v2.getId())).isLeft();
      base.cancelTransaction();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.of(v1.getId(), value.getId()));
      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }

   @ParameterizedTest
   @MethodSource("provideData")
   <K, V extends Storable<K>> void testFailureRollbackInvalidDelete(
      MultiChannelDataStore<String, StringStorable> base,
      DataStore<K, V> dataStore,
      V value,
      Comparator<V> comparator,
      V overwrite,
      Supplier<V> supplier
   ) {
      assertThat(dataStore.save(value)).isRight();
      V v1 = supplier.get();
      assertThat(dataStore.save(v1)).isRight();
      assertThat(base.commit()).isRight();
      assertThat(dataStore.save(overwrite)).isRight();

      V v2 = supplier.get();
      assertThat(dataStore.save(v2)).isRight();
      assertThat(dataStore.deleteById(v2.getId())).isRight();
      assertThat(dataStore.deleteById(v2.getId())).isLeft();
      base.cancelTransaction();

      assertThat(dataStore.listIds())
         .containsOnRight(HashSet.of(v1.getId(), value.getId()));
      assertThat(dataStore.getById(value.getId()))
         .usingValueComparator(comparator)
         .containsOnRight(value);
   }
}
