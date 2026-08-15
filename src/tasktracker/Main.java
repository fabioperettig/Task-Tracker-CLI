package tasktracker;

import tasktracker.cli.TaskCli;
import tasktracker.repository.JsonTaskRepository;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        JsonTaskRepository repository = new JsonTaskRepository(Path.of("tasks.json"));
        TaskCli cli = new TaskCli(repository);
        cli.run(args);
    }
}


