package tasktracker.cli;

import tasktracker.model.TaskStatus;

public final class TaskCli {

    public void run(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        try {
            switch (args[0]) {
                case "add" -> handleAdd(args);
                case "update" -> handleUpdate(args);
                case "delete" -> handleDelete(args);
                case "mark-in-progress" -> handleMarkInProgress(args);
                case "mark-done" -> handleMarkDone(args);
                case "list" -> handleList(args);
                default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
            }

        } catch (IllegalArgumentException exception) {
            System.err.println("Error: " + exception.getMessage());
        }
    }

    /// Handler methods
    private void handleAdd(String[] args) {
        requireArgumentCount(args,2,"Usage: task-cli add \"description\"");

        String description = requireDescription(args[1]);
        System.out.printf("Add command parsed: %s%n",description);
    }

    private void handleUpdate(String[] args) {
        requireArgumentCount(args,3,"Usage: task-cli update <id> \"description\"");

        int id = parseId(args[1]);
        String description = requireDescription(args[2]);
        System.out.printf("Update command parsed for task ID: %d, description: %s%n", id, description);
    }

    private void handleDelete(String[] args) {
        requireArgumentCount(args, 2, "Usage: task-cli delete <id>");

        int id = parseId(args[1]);
        System.out.printf("Delete command parsed for task ID: %d%n", id);
    }

    private void handleMarkInProgress(String[] args) {
        requireArgumentCount(args, 2, "Usage: task-cli mark-in-progress <id>");

        int id = parseId(args[1]);
        System.out.printf("Mark-in-progress command parsed for task ID: %d%n", id);
    }

    private void handleMarkDone(String[] args) {
        requireArgumentCount(args, 2, "Usage: task-cli mark-done <id>");

        int id = parseId(args[1]);
        System.out.printf("Mark-done command parsed for task ID: %d%n", id);
    }

    private void handleList(String[] args) {
        if (args.length == 1) {
            System.out.println("List all tasks command parsed");
            return;
        }

        if (args.length == 2) {
            TaskStatus status = TaskStatus.fromValue(args[1]);

            System.out.printf("List tasks by status command parsed: %s%n", status.getValue());
            return;
        }

        throw new IllegalArgumentException(
                "Usage: task-cli list [todo|in-progress|done]"
        );
    }

    /// Validation helpers
    private String requireDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Task description cannot be empty");
        }
        return description;
    }

    private int parseId(String rawId) {
        try {
            int id = Integer.parseInt(rawId);

            if (id <= 0) {
                throw new IllegalArgumentException("Task ID must be positive");
            }

            return id;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Task ID must be a number");
        }
    }

    private void requireArgumentCount(String[] args, int expected, String usage) {
        if (args.length != expected) {
            throw new IllegalArgumentException(usage);
        }
    }

    private void printUsage() {
        System.out.println("""
                Task Tracker CLI

                Usage:
                  task-cli add "description"
                  task-cli update <id> "description"
                  task-cli delete <id>
                  task-cli mark-in-progress <id>
                  task-cli mark-done <id>
                  task-cli list
                  task-cli list <todo|in-progress|done>
                """);
    }
}
