package io.github.theimbichner.task.io;

public interface Storable {
   String getId();
   void registerTaskStore(TaskStore taskStore);
   TaskStore getTaskStore();
}
