package io.github.theimbichner.taskmanager.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Function;

import io.vavr.collection.Vector;
import io.vavr.control.Either;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JsonFileDataStore<K, V extends Storable<K>> implements DataStore<K, V> {
   private static final String INDEX_FILENAME = "index.json";
   private static final String TEMP_FILENAME = "temp";
   private static final String EXTENSION = ".json";

   private final File root;
   private final Function<V, JSONObject> toJson;
   private final Function<JSONObject, V> fromJson;

   private String activeTransactionId;

   public JsonFileDataStore(
      File root,
      Function<V, JSONObject> toJson,
      Function<JSONObject, V> fromJson
   ) throws IOException {
      this.root = root;
      this.toJson = toJson;
      this.fromJson = fromJson;

      root.mkdirs();
      if (!root.exists()) {
         throw new IOException("Failed to create root directory");
      }

      startNewTransaction();
   }

   @Override
   public Either<TaskAccessException, V> getById(K id) {
      try (FileInputStream stream = new FileInputStream(lookupById(id))) {
         JSONTokener tokener = new JSONTokener(stream);
         JSONObject json = new JSONObject(tokener);
         return Either.right(fromJson.apply(json));
      }
      catch (IOException|JSONException e) {
         return Either.left(new TaskAccessException(e));
      }
   }

   @Override
   public Either<TaskAccessException, V> save(V value) {
      K id = value.getId();
      File file = getTempFile();

      try (
         FileOutputStream stream = new FileOutputStream(file);
         OutputStreamWriter writer = new OutputStreamWriter(stream)
      ) {
         toJson.apply(value).write(writer);

         Path target = getUncommittedFile(id).toPath();
         Files.move(file.toPath(), target, StandardCopyOption.ATOMIC_MOVE);

         return Either.right(value);
      }
      catch (IOException|JSONException e) {
         return Either.left(new TaskAccessException(e));
      }
   }

   @Override
   public Either<TaskAccessException, Void> deleteById(K id) {
      try {
         lookupById(id);
      }
      catch (IOException e) {
         return Either.left(new TaskAccessException(e));
      }

      File file = getTempFile();

      file.delete();
      if (file.exists()) {
         String message = "Failed to delete file";
         return Either.left(new TaskAccessException(new IOException(message)));
      }

      try {
         file.createNewFile();

         Path target = getUncommittedFile(id).toPath();
         Files.move(file.toPath(), target, StandardCopyOption.ATOMIC_MOVE);

         return Either.right(null);
      }
      catch (IOException e) {
         return Either.left(new TaskAccessException(e));
      }
   }

   @Override
   public Either<TaskAccessException, Void> commit() {
      Vector<String> registeredFolders;
      try {
         registeredFolders = getRegisteredFolders().append(activeTransactionId);
         setRegisteredFolders(registeredFolders);
      }
      catch (IOException e) {
         cancelTransaction();
         return Either.left(new TaskAccessException(e));
      }

      cleanUpRegisteredFolders(registeredFolders);
      startNewTransaction();

      return Either.right(null);
   }

   @Override
   public void cancelTransaction() {
      cleanUpUnregisteredFolder();
      startNewTransaction();
   }

   private void startNewTransaction() {
      activeTransactionId = UUID.randomUUID().toString();
      getActiveFolder().mkdirs();
   }

   private File lookupById(K id) throws IOException {
      String filename = id.toString() + EXTENSION;

      Vector<String> folders = getRegisteredFolders();
      folders = folders.reverse().prepend(activeTransactionId);
      for (String s : folders) {
         File dir = new File(root, s);
         File potentialPath = new File(dir, filename);
         if (potentialPath.exists()) {
            if (isDeletion(potentialPath)) {
               throw new FileNotFoundException("File has been deleted");
            }
            return potentialPath;
         }
      }

      String message = "Cannot find file in any registered folder";
      throw new FileNotFoundException(message);
   }

   private File getActiveFolder() {
      return new File(root, activeTransactionId);
   }

   private File getUncommittedFile(K id) {
      String filename = id.toString() + EXTENSION;
      return new File(getActiveFolder(), filename);
   }

   private File getIndexFile() {
      return new File(root, INDEX_FILENAME);
   }

   private File getTempFile() {
      return new File(root, TEMP_FILENAME);
   }

   private boolean isDeletion(File file) throws IOException {
      Long size = (Long) Files.getAttribute(file.toPath(), "size");
      return size == 0;
   }

   @SuppressWarnings("unchecked")
   private Vector<String> getRegisteredFolders() throws IOException {
      File file = getIndexFile();
      if (!file.exists()) {
         return Vector.of("./");
      }

      try (FileInputStream stream = new FileInputStream(file)) {
         JSONTokener tokener = new JSONTokener(stream);
         Iterable<?> jsonArray = new JSONArray(tokener);
         return Vector.ofAll((Iterable<String>) jsonArray);
      }
      catch (JSONException e) {
         throw new IOException(e);
      }
   }

   private void setRegisteredFolders(Vector<String> vector) throws IOException {
      File file = getTempFile();

      JSONArray jsonArray = new JSONArray(vector);
      try (
         FileOutputStream stream = new FileOutputStream(file);
         OutputStreamWriter writer = new OutputStreamWriter(stream)
      ) {
         jsonArray.write(writer);
      }
      catch (JSONException e) {
         throw new IOException(e);
      }

      Path target = getIndexFile().toPath();
      Files.move(file.toPath(), target, StandardCopyOption.ATOMIC_MOVE);
   }

   private void cleanUpUnregisteredFolder() {
      File dir = getActiveFolder();
      try {
         boolean allFilesDeleted = Files.walk(dir.toPath())
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .allMatch(File::delete);
         if (!allFilesDeleted) {
            throw new IOException("Failed to delete files");
         }
      }
      catch (IOException e) {
         // It's fine to have unregistered files remain, so no action is needed
         e.printStackTrace();
      }
   }

   private void cleanUpRegisteredFolders(Vector<String> registeredFolders) {
      // TODO
      // also account for the fact that ./ is a registered folder
      // keep in mind that registeredFolders is ordered
   }
}
