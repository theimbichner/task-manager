package io.github.theimbichner.taskmanager.io.datastore.impl;

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
import java.util.UUID;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.collection.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import io.github.theimbichner.taskmanager.io.IOUtils;
import io.github.theimbichner.taskmanager.io.TaskAccessException;
import io.github.theimbichner.taskmanager.io.TaskAccessResult;
import io.github.theimbichner.taskmanager.io.datastore.DataStore;
import io.github.theimbichner.taskmanager.io.datastore.MultiChannelDataStore;
import io.github.theimbichner.taskmanager.io.datastore.StringStorable;

public class FileDataStore extends MultiChannelDataStore<String, StringStorable> {
   private class Channel extends DataStore<String, StringStorable> {
      private final String channelId;
      private final File channelRoot;

      public Channel(String channelId) {
         this.channelId = channelId;
         channelRoot = new File(root, channelId);
      }

      @Override
      public TaskAccessResult<Set<String>> listIds() {
         HashSet<String> result = HashSet.empty();

         try {
            Vector<String> registeredFolders = getRegisteredFolders();
            for (String s : registeredFolders.append(activeTransactionId)) {
               File dir = new File(channelRoot, s);
               String[] filenames = dir.list();
               if (filenames == null) {
                  throw new IOException("Could not list files");
               }

               for (String filename : filenames) {
                  File file = new File(dir, filename);
                  if (IOUtils.isDirectory(file)) {
                     continue;
                  }

                  int idSize = filename.length() - extension.length();
                  String id = filename.substring(0, idSize);
                  if (IOUtils.isEmptyFile(new File(dir, filename))) {
                     result = result.remove(id);
                  }
                  else {
                     result = result.add(id);
                  }
               }
            }

            return TaskAccessResult.ofRight(result);
         }
         catch (IOException e) {
            return TaskAccessResult.ofLeft(new TaskAccessException(e));
         }
      }

      @Override
      public TaskAccessResult<StringStorable> getById(String id) {
         try {
            File file = lookupById(id);
            String fileContents = Files.readString(file.toPath());
            return TaskAccessResult.ofRight(new StringStorable(id, fileContents));
         }
         catch (IOException e) {
            return TaskAccessResult.ofLeft(new TaskAccessException(e));
         }
      }

      @Override
      public TaskAccessResult<StringStorable> save(StringStorable s) {
         File file = getTempFile();

         try (
            FileOutputStream stream = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)
         ) {
            writer.write(s.getValue());

            Path target = getUncommittedFile(s.getId()).toPath();
            Files.move(file.toPath(), target, StandardCopyOption.ATOMIC_MOVE);

            return TaskAccessResult.ofRight(s);
         }
         catch (IOException e) {
            return TaskAccessResult.ofLeft(new TaskAccessException(e));
         }
      }

      @Override
      public TaskAccessResult<Void> deleteById(String id) {
         try {
            lookupById(id);
         }
         catch (IOException e) {
            return TaskAccessResult.ofLeft(new TaskAccessException(e));
         }

         File file = getTempFile();
         try {
            IOUtils.writeEmptyfile(file);

            Path target = getUncommittedFile(id).toPath();
            Files.move(file.toPath(), target, StandardCopyOption.ATOMIC_MOVE);

            return TaskAccessResult.ofRight(null);
         }
         catch (IOException e) {
            return TaskAccessResult.ofLeft(new TaskAccessException(e));
         }
      }

      private File getUncommittedFile(String id) {
         String filename = id + extension;
         return new File(getActiveFolder(channelId), filename);
      }

      private File lookupById(String id) throws IOException {
         String filename = id + extension;

         Vector<String> folders = getRegisteredFolders();
         folders = folders.reverse().prepend(activeTransactionId);
         for (String s : folders) {
            File dir = new File(channelRoot, s);
            File potentialPath = new File(dir, filename);
            if (potentialPath.exists()) {
               if (IOUtils.isEmptyFile(potentialPath)) {
                  throw new FileNotFoundException("File has been deleted");
               }
               return potentialPath;
            }
         }

         String message = "Cannot find file in any registered folder";
         throw new FileNotFoundException(message);
      }
   }

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
   protected DataStore<String, StringStorable> createChannel(String channelId) {
      getActiveFolder(channelId).mkdirs();
      return new Channel(channelId);
   }

   @Override
   protected TaskAccessResult<Void> performCommit() {
      Vector<String> registeredFolders;
      try {
         registeredFolders = getRegisteredFolders().append(activeTransactionId);
         setRegisteredFolders(registeredFolders);
      }
      catch (IOException e) {
         cancelTransaction();
         return TaskAccessResult.ofLeft(new TaskAccessException(e));
      }

      cleanUpRegisteredFolders(registeredFolders);
      startNewTransaction();

      return TaskAccessResult.ofRight(null);
   }

   @Override
   protected void performCancel() {
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

   private Vector<String> getChannelIds() throws IOException {
      String[] filenames = root.list();
      if (filenames == null) {
         throw new IOException("Could not list files in root");
      }

      Vector<String> result = Vector.empty();
      for (String filename : filenames) {
         if (IOUtils.isDirectory(new File(root, filename))) {
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
            IOUtils.deleteFolder(getActiveFolder(channelId));
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
