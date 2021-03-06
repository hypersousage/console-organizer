package organizer;

import org.apache.commons.cli.*;
import redis.clients.jedis.Jedis;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.lang.Thread;
import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

public class App {
  private Gson gson;

  public App() {
    this.gson = new Gson();
  }

  public void run(String[] args) {
    CommandLine line = parseArguments(args);
    Task currentTask = new Task(generateTaskID());
    if (line.hasOption("deadline")) {
      if (!currentTask.parseDeadline(line.getOptionValue("deadline"))) {
        System.out.println("Deadline format is not correct! It should be yyyy-MM-dd HH:mm:ss");
        return;
      }
    }
    if (line.hasOption("priority")) {
      if (!currentTask.parsePriority(line.getOptionValue("priority"))) {
        System.out.println("Priority format is not correct! It should be one of {LOW, MEDIUM, HIGH}");
        return;
      }
    }
    if (line.hasOption("tag")) {
      var tags = line.getOptionValues("tag");
      for (String tag : tags) {
        currentTask.addTag(tag);
      }
    }
    if (line.hasOption("remind")) {
      currentTask.setNeedReminder();
    }
    if (line.hasOption("task-id")) {
      currentTask.TaskID = Integer.parseInt(line.getOptionValue("task-id"));
    }

    if (line.hasOption("add")) {
      currentTask.setDescription(line.getOptionValue("add"));
      addTask(currentTask);
    } else if (line.hasOption("list")) {
      String type = line.getOptionValue("list");
      String sortBy = "default";
      if (line.hasOption("sort-by")) {
        sortBy = line.getOptionValue("sort-by");
      }
      listTasks(type, currentTask, sortBy);
    } else if (line.hasOption("delete")) {
      deleteTask(line.getOptionValue("delete"));
    } else if (line.hasOption("change")) {
      changeTask(currentTask);
    } else if (line.hasOption("notify")) {
      notifyAboutTasks();
    } else {
      System.err.println("Failed to parse command line arguments");
      printAppHelp();
    }
  }

  private String getRedisHost() {
    String host = "localhost";
    if (System.getenv("ORGANIZER_DOCKER_BUILD") != null) {
      host = "host.docker.internal";
    }
    return host;
  }

  private void addTask(Task task) {
    Jedis jedis = new Jedis(getRedisHost());
    var tasks = jedis.hgetAll("tasks");
    tasks.put(Integer.toString(task.TaskID), task.taskToString());
    jedis.hmset("tasks", tasks);
    System.out.println("Put task with id " + task.TaskID + " with description \"" + task.Description + "\"");
  }

  private void listTasks(String type, Task filterTask, String sortBy) {
    if (!type.equals("all") && !type.equals("today") && !type.equals("filter")) {
      System.out.println("Invalid argument value of 'list', should be either 'today', 'all' or 'filter'");
      return;
    }
    if (!sortBy.equals("priority") && !sortBy.equals("deadline") && !sortBy.equals("default")) {
      System.out.println("Invalid argument value of 'sort-by', should be either 'priority' or 'deadline'");
      return;
    }
    boolean sortByPriority = sortBy.equals("priority");
    boolean sortByDeadline = sortBy.equals("deadline");
    TaskWriter TaskWrite = new SimpleWriter(sortByDeadline, sortByPriority);
    Jedis jedis = new Jedis(getRedisHost());
    var tasks = jedis.hgetAll("tasks");
    int size = tasks.size();
    if (size == 0) {
      System.out.println("You don't have tasks in storage");
      return;
    }
    Calendar today = Calendar.getInstance();
    ArrayList<Task> tasksArray = new ArrayList<>();
    for (Map.Entry<String, String> entry : tasks.entrySet()) {
      Task task = new Task(0);
      if (task.fromString(entry.getValue())) {
        if (type.equals("today")) {
          if (task.Deadline == null) {
            continue;
          }
          Calendar deadline = Calendar.getInstance();
          deadline.setTime(task.Deadline);
          if (deadline.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR)
                  || deadline.get(Calendar.YEAR) != today.get(Calendar.YEAR))
          {
            continue;
          }
        } else if (type.equals("filter") && !filterTask.compareTask(task)) {
          continue;
        }
        tasksArray.add(task);
      }
    }
    System.out.println(MessageFormat.format("You have {0} tasks in storage:", tasksArray.size()));
    System.out.print(TaskWrite.serializeTasks(tasksArray));
  }

  private void deleteTask(String taskID) {
    Jedis jedis = new Jedis(getRedisHost());
    var tasks = jedis.hgetAll("tasks");
    if (tasks.containsKey(taskID)) {
      jedis.hdel("tasks", taskID);
      System.out.println("Deleted successfully!");
    } else {
      System.out.println("No task found with such task_id");
    }
  }

  private void changeTask(Task updateTask) {
    String taskID = String.valueOf(updateTask.TaskID);
    Jedis jedis = new Jedis();
    var tasks = jedis.hgetAll("tasks");
    if (tasks.containsKey(taskID)) {
      Task targetTask = new Task(Integer.parseInt(taskID));
      targetTask.fromString(tasks.get(taskID));
      targetTask.changeTask(updateTask);
      tasks.put(Integer.toString(targetTask.TaskID), targetTask.taskToString());
      jedis.hmset("tasks", tasks);
      System.out.println("Task changed successfully!");
    } else {
      System.out.println("No task found with such task_id " + Integer.toString(updateTask.TaskID));
    }
  }

  public void notifyAboutTasks() {
    Calendar tomorrow = Calendar.getInstance();
    Calendar today = Calendar.getInstance();
    tomorrow.add(Calendar.DATE, 1);
    Jedis jedis = new Jedis();
    var tasks = jedis.hgetAll("tasks");
    for (Map.Entry<String, String> entry : tasks.entrySet()) {
      Task task = new Task(0);
      if (task.fromString(entry.getValue()) && task.Reminder) {
        Calendar curDate = Calendar.getInstance();
        curDate.setTime(task.Deadline);
        if (today.compareTo(curDate) < 0 && tomorrow.compareTo(curDate) > 0) {
          try {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String deadlineStr = formatter.format(task.Deadline);
            var command = String.format(
                    "display notification \"%s is due on %s\" with title \"%s\" sound name \"Purr\"",
                    task.Description, deadlineStr, "ConsoleOrganizer notification");
            var runtime = Runtime.getRuntime();
            String[] code = {"osascript", "-e", command};
            var process = runtime.exec(code);
            Thread.sleep(2000);
            //var sleed_process = runtime.exec(code_sleep);
          } catch (IOException ex) {}
          catch (InterruptedException ex1) {}
        }
      }
    }
  }

  private CommandLine parseArguments(String[] args) {
    Options options = getOptions();
    CommandLine line = null;

    CommandLineParser parser = new DefaultParser();

    try {
      line = parser.parse(options, args);

    } catch (ParseException ex) {

      System.err.println("Failed to parse command line arguments");
      System.err.println(ex.toString());
      printAppHelp();

      System.exit(1);
    }

    return line;
  }

  private int generateTaskID() {
    return ThreadLocalRandom.current().nextInt(0, 10000 + 1);
  }

  private Options getOptions() {

    var options = new Options();

    options.addOption("a", "add", true, "Add task");
    options.addOption("l", "list", true, "List tasks");
    options.addOption("d", "delete", true, "Delete task");
    options.addOption("dl", "deadline", true, "Task deadline, format 2021-01-01 00:00:00");
    options.addOption("pr", "priority", true, "Task priority, one of {LOW, MEDIUM, HIGH}");
    options.addOption("t", "tag", true, "Task tag, may be multiple");
    options.addOption("s", "sort-by", true, "Value to sort tasks by, supported 'priority', 'deadline'");
    options.addOption("r", "remind", false, "Remind about task");
    options.addOption("tid", "task-id", true, "TaskID");
    options.addOption("c", "change", false, "Change task by task_id");
    options.addOption("n", "notify", false, "Notify about tasks");

    return options;
  }

  private void printAppHelp() {

    Options options = getOptions();

    var formatter = new HelpFormatter();
    formatter.printHelp("ConsoleOrganizer", options, true);
  }
}

