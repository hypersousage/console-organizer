package organizer;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

enum PriorityEnum {
  LOW,
  MEDIUM,
  HIGH
}

public class Task {
  int TaskID;
  boolean Reminder;
  String Description;
  List<String> Tags;
  PriorityEnum Priority;
  Date Deadline;

  public Task(int taskID) {
    this.TaskID = taskID;
    this.Tags = new ArrayList<>();
  }

  public void setDescription(String description) {
    Description = description;
  }

  public void addTag(String tag) {
    Tags.add(tag);
  }

  public void setNeedReminder() {
    Reminder = true;
  }

  public boolean parsePriority(String priorityStr) {
    for (PriorityEnum priority : PriorityEnum.values()) {
      if (priority.name().equals(priorityStr)) {
        Priority = priority;
        return true;
      }
    }
    return false;
  }

  public boolean parseDeadline(String deadlineStr) {
    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    try {
      Date parsedTime = formatter.parse(deadlineStr);
      if (parsedTime == null) {
        return false;
      }
      Deadline = parsedTime;
    } catch (ParseException e) {
      return false;
    }
    return true;
  }

  public String taskToString() {
    HashMap<String, String> values = new HashMap<>();
    values.put("task_id", Integer.toString(TaskID));
    values.put("reminder", Boolean.toString(Reminder));
    values.put("description", Description);
    if (Priority != null) {
      values.put("priority", Priority.toString());
    }
    if (Deadline != null) {
      DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      values.put("deadline", formatter.format(Deadline));
    }
    values.put("tags", Tags.toString().replace(", ", ","));
    return values.toString();
  }

  public boolean fromString(String str) {
    Properties props = new Properties();
    try {
      props.load(new StringReader(str.substring(1, str.length() - 1).replace(", ", "\n")));
    } catch (IOException e) {
      return false;
    }
    Map<String, String> values = new HashMap<>();
    for (Map.Entry<Object, Object> e : props.entrySet()) {
      values.put((String)e.getKey(), (String)e.getValue());
    }
    TaskID = Integer.parseInt(values.get("task_id"));
    Reminder = Boolean.parseBoolean(values.get("reminder"));
    Description = values.get("description");
    String tagsStr = values.get("tags");
    Tags = Arrays.asList(tagsStr.substring(1, tagsStr.length() - 1).split(","));
    if (values.containsKey("priority")) {
      Priority = PriorityEnum.valueOf(values.get("priority"));
    }
    if (values.containsKey("deadline")) {
      try {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date parsed = formatter.parse(values.get("deadline"));
        if (parsed != null) {
          Deadline = parsed;
        }
      } catch (ParseException ignored) {}
    }
    return true;
  }

  public boolean compareTask(Task task) {
    if (this.Deadline != null && (task.Deadline == null || this.Deadline.compareTo(task.Deadline) != 0)) {
      return false;
    }
    if (this.Priority != null && (task.Priority == null || this.Priority.compareTo(task.Priority) != 0)) {
      return false;
    }
    for (var tag : this.Tags) {
      if (!task.Tags.contains(tag)) {
        return false;
      }
    }
    return true;
  }

  public void changeTask(Task task) {
    if (task.Deadline != null) {
      this.Deadline = task.Deadline;
    }
    if (task.Priority != null) {
      this.Priority = task.Priority;
    }
    if (task.Tags.size() > 0) {
      this.Tags = task.Tags;
    }
  }
}
