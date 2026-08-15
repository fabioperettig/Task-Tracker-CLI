package tasktracker;

import tasktracker.cli.TaskCli;

public class Main {

    public static void main(String[] args) {
        TaskCli cli = new TaskCli();
        cli.run(args);
    }
}


