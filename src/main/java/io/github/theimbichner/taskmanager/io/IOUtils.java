package io.github.theimbichner.taskmanager.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class IOUtils {
   private IOUtils() {}

   public static void writeEmptyfile(File file) throws IOException {
      Path target = file.toPath();
      Files.deleteIfExists(target);
      Files.createFile(target);
   }

   public static boolean isEmptyFile(File file) throws IOException {
      Long size = (Long) Files.getAttribute(file.toPath(), "size");
      return size == 0;
   }

   public static boolean isDirectory(File file) throws IOException {
      return (Boolean) Files.getAttribute(file.toPath(), "isDirectory");
   }

   public static void deleteFolder(File dir) throws IOException {
      try (Stream<Path> paths = Files.walk(dir.toPath())) {
         boolean allFilesDeleted = paths
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .allMatch(File::delete);
         if (!allFilesDeleted) {
            throw new IOException("Failed to delete files");
         }
      }
   }
}
