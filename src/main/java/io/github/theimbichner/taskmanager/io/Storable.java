package io.github.theimbichner.taskmanager.io;

public interface Storable {
   String getId();
   void setTaskStore(TaskStore taskStore);
   TaskStore getTaskStore();
}
