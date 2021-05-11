package organizer;

import redis.clients.jedis.Jedis;

import com.google.gson.Gson;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.Map;
import java.util.UUID;
import java.text.MessageFormat;

public class App {
  private Gson gson;

  public void App() {
    this.gson = new Gson();
  }

  public void run(String[] args) {
    CommandLine line = parseArguments(args);
    if (line.hasOption("add")) {
      addTask(line.getOptionValue("add"));
    } else if (line.hasOption("list")) {
      listTasks();
    } else if (line.hasOption("delete")) {
      deleteTask(line.getOptionValue("delete"));
    } else {
      System.err.println("Failed to parse command line arguments");
      printAppHelp();
    }
  }

  private void addTask(String description) {
    Jedis jedis = new Jedis();
    String new_uuid = generateUUID();
    var tasks = jedis.hgetAll("tasks");
    tasks.put(new_uuid, description);
    jedis.hmset("tasks", tasks);
    System.out.println("Assigned uuid " + new_uuid + " to new task \"" + description + "\"");
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
    for (Map.Entry<String, String> entry : tasks.entrySet()) {
      System.out.println(MessageFormat.format("{0} \t {1}", entry.getKey(), entry.getValue()));
    }
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


  private String generateUUID() {
    String uuid = UUID.randomUUID().toString().replace("-", "");
    return uuid.substring(0, 6);
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

  private Options getOptions() {

    var options = new Options();

    options.addOption("a", "add", true, "Add task");
    options.addOption("l", "list", false, "List tasks");
    options.addOption("d", "delete", true, "Delete task");

    return options;
  }

  private void printAppHelp() {

    Options options = getOptions();

    var formatter = new HelpFormatter();
    formatter.printHelp("ConsoleOrganizer", options, true);
  }
}
