package organizer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SimpleWriter extends TaskWriter {
  boolean SortByDeadline;
  boolean SortByPriority;
  public SimpleWriter(boolean sortByDeadline, boolean sortByPriority) {
    SortByDeadline = sortByDeadline;
    SortByPriority = sortByPriority;
  }

  @Override
  public String serializeTasks(List<Task> tasks) {
    StringBuilder builder = new StringBuilder();
    if (SortByDeadline) {
      Collections.sort(tasks, new Comparator<>() {
        @Override
        public int compare(Task o1, Task o2) {
          if (o1.Deadline != null && o2.Deadline != null) {
            return o1.Deadline.compareTo(o2.Deadline);
          }
          if (o1.Deadline != null) {
            return -1;
          }
          if (o2.Deadline != null) {
            return 1;
          }
          return 0;
        }
      });
    }
    if (SortByPriority) {
      Collections.sort(tasks, new Comparator<>() {
        @Override
        public int compare(Task o1, Task o2) {
          if (o1.Priority != null && o2.Priority != null) {
            return -o1.Priority.compareTo(o2.Priority);
          }
          if (o1.Priority != null) {
            return -1;
          }
          if (o2.Priority != null) {
            return 1;
          }
          return 0;
        }
      });
    }
    for (Task task : tasks) {
      builder.append("TaskID=" + task.TaskID + ", Description=" + task.Description + ", Reminder=" + task.Reminder);
      if (task.Deadline != null) {
        builder.append(", Deadline=" + task.Deadline);
      }
      if (task.Priority != null) {
        builder.append(", Priority=" + task.Priority);
      }
      builder.append(", Tags=" + task.Tags.toString());
      builder.append("\n");
    }
    return builder.toString();
  }
}