package io.github.theimbichner.taskmanager.io;

public interface Storable<K> {
   K getId();
   void setTaskStore(TaskStore taskStore);
   TaskStore getTaskStore();
}
