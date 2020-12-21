package io.github.theimbichner.task.io;

public interface Storable {
   String getId();
   void setTaskStore(TaskStore taskStore);
   TaskStore getTaskStore();
}
