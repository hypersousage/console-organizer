package organizer;

import java.util.List;

public abstract class TaskWriter {
  public abstract String serializeTasks(List<Task> tasks);
}
