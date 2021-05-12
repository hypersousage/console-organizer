package organizer;

import redis.clients.jedis.Jedis;

import com.google.gson.Gson;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

public class App {
  private Gson gson;
  private TaskWriter TaskWrite;

  public App() {
    this.gson = new Gson();
    this.TaskWrite = new SimpleWriter(true);
  }

  public void run(String[] args) {
    CommandLine line = parseArguments(args);
    if (line.hasOption("add")) {
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
      if (line.hasOption("tags")) {
        var tags = line.getOptionValues("tags");
        for (String tag : tags) {
          currentTask.addTag(tag);
        }
      }
      if (line.hasOption("remind")) {
        currentTask.setNeedReminder();
      }
      currentTask.setDescription(line.getOptionValue("add"));
      addTask(currentTask);
    } else if (line.hasOption("list")) {
      listTasks();
    } else if (line.hasOption("delete")) {
      deleteTask(line.getOptionValue("delete"));
    } else {
      System.err.println("Failed to parse command line arguments");
      printAppHelp();
    }
  }

  private void addTask(Task task) {
    Jedis jedis = new Jedis();
    var tasks = jedis.hgetAll("tasks");
    tasks.put(Integer.toString(task.TaskID), task.taskToString());
    jedis.hmset("tasks", tasks);
    System.out.println("Task serialized is " + task.taskToString());
    System.out.println("Put task with id " + task.TaskID + " with description \"" + task.Description + "\"");
  }

  private void listTasks() {
    Jedis jedis = new Jedis();
    var tasks = jedis.hgetAll("tasks");
    int size = tasks.size();
    if (size == 0) {
      System.out.println("You don't have tasks in storage");
      return;
    }
    System.out.println(MessageFormat.format("You have {0} tasks in storage:", tasks.size()));
    ArrayList<Task> tasksArray = new ArrayList<>();
    for (Map.Entry<String, String> entry : tasks.entrySet()) {
      Task task = new Task(0);
      if (task.fromString(entry.getValue())) {
        tasksArray.add(task);
      }
    }
    System.out.print(TaskWrite.serializeTasks(tasksArray));
  }

  private void deleteTask(String uuid) {
    Jedis jedis = new Jedis();
    var tasks = jedis.hgetAll("tasks");
    if (tasks.containsKey(uuid)) {
      jedis.hdel("tasks", uuid);
      System.out.println("Deleted successfully!");
    } else {
      System.out.println("No task found with such uuid");
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
    options.addOption("l", "list", false, "List tasks");
    options.addOption("d", "delete", true, "Delete task");
    options.addOption("dl", "deadline", true, "Task deadline, format 2021-01-01 00:00:00");
    options.addOption("pr", "priority", true, "Task priority, one of {LOW, MEDIUM, HIGH}");

    return options;
  }

  private void printAppHelp() {

    Options options = getOptions();

    var formatter = new HelpFormatter();
    formatter.printHelp("ConsoleOrganizer", options, true);
  }
}

