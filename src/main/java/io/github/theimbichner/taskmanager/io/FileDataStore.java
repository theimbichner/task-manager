package io.github.theimbichner.taskmanager.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;

import io.vavr.collection.Vector;
import io.vavr.control.Either;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

public class FileDataStore implements MultiChannelDataStore<String, StringStorable> {
   private static final String INDEX_FILENAME = "index.json";
   private static final String TEMP_FILENAME = "temp";

   private final File root;
   private final String extension;

   private String activeTransactionId;

   public FileDataStore(File root, String extension) throws IOException {
      this.root = root;
      this.extension = extension;

      root.mkdirs();
      if (!root.exists()) {
         throw new IOException("Failed to create root directory");
      }

      startNewTransaction();
   }

   @Override
   public DataStore<String, StringStorable> getChannel(String channelId) {
      getActiveFolder(channelId).mkdirs();
      return new DataStore<>() {
         @Override
         public Either<TaskAccessException, StringStorable> getById(String id) {
            try {
               File file = lookupById(id);
               String fileContents = Files.readString(file.toPath());
               return Either.right(new StringStorable(id, fileContents));
            }
            catch (IOException e) {
               return Either.left(new TaskAccessException(e));
            }
         }

         @Override
         public Either<TaskAccessException, StringStorable> save(StringStorable s) {
            File file = getTempFile();

            try (
               FileOutputStream stream = new FileOutputStream(file);
               OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)
            ) {
               writer.write(s.getValue());

               Path target = getUncommittedFile(s.getId()).toPath();
               Files.move(file.toPath(), target, StandardCopyOption.ATOMIC_MOVE);

               return Either.right(s);
            }
            catch (IOException e) {
               return Either.left(new TaskAccessException(e));
            }
         }

         @Override
         public Either<TaskAccessException, Void> deleteById(String id) {
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

         private File getUncommittedFile(String id) {
            String filename = id + extension;
            return new File(getActiveFolder(channelId), filename);
         }

         private File lookupById(String id) throws IOException {
            String filename = id + extension;
            File channelRoot = new File(root, channelId);

            Vector<String> folders = getRegisteredFolders();
            folders = folders.reverse().prepend(activeTransactionId);
            for (String s : folders) {
               File dir = new File(channelRoot, s);
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
      };
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
      try {
         for (String channelId : getChannelIds()) {
            getActiveFolder(channelId).mkdirs();
         }
      }
      catch (IOException e) {
         // If we ever fail to create any of these folders, this will simply
         // cause errors later when trying to save to those folders. Hence, no
         // action needs to be taken now.
         e.printStackTrace();
      }
   }

   private File getIndexFile() {
      return new File(root, INDEX_FILENAME);
   }

   private File getTempFile() {
      return new File(root, TEMP_FILENAME);
   }

   private File getActiveFolder(String channelId) {
      File channelRoot = new File(root, channelId);
      return new File(channelRoot, activeTransactionId);
   }

   private boolean isDeletion(File file) throws IOException {
      Long size = (Long) Files.getAttribute(file.toPath(), "size");
      return size == 0;
   }

   private Vector<String> getChannelIds() throws IOException {
      String[] filenames = root.list();
      if (filenames == null) {
         throw new IOException("Could not list files in root");
      }

      Vector<String> result = Vector.empty();
      for (String filename : filenames) {
         Path target = new File(root, filename).toPath();
         Boolean isDirectory = (Boolean) Files.getAttribute(target, "isDirectory");
         if (isDirectory) {
            result = result.append(filename);
         }
      }
      return result;
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

      JSONArray jsonArray = new JSONArray(vector.asJava());
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

   private void deleteFolder(File dir) throws IOException {
      boolean allFilesDeleted = Files.walk(dir.toPath())
         .sorted(Comparator.reverseOrder())
         .map(Path::toFile)
         .allMatch(File::delete);
      if (!allFilesDeleted) {
         throw new IOException("Failed to delete files");
      }
   }

   private void cleanUpUnregisteredFolder() {
      Vector<String> channelIds;
      try {
         channelIds = getChannelIds();
      }
      catch (IOException e) {
         // It's fine to have unregistered files remain, so no action is needed
         e.printStackTrace();
         return;
      }

      for (String channelId : channelIds) {
         try {
            deleteFolder(getActiveFolder(channelId));
         }
         catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   private void cleanUpRegisteredFolders(Vector<String> registeredFolders) {
      // TODO
      // also account for the fact that ./ is a registered folder
      // keep in mind that registeredFolders is ordered
   }
}
