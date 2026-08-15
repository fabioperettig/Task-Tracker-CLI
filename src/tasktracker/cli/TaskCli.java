package tasktracker.cli;

import tasktracker.model.Task;
import tasktracker.model.TaskStatus;
import tasktracker.repository.JsonTaskRepository;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public final class TaskCli {
    private final JsonTaskRepository repository;
    private final List<Task> tasks = new ArrayList<>();
    private int nextTaskId = 1;

    public TaskCli(JsonTaskRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("Task repository cannot be null");
        }

        this.repository = repository;
    }

    public void run(String[] args) {
        try {
            repository.initialize();
            tasks.clear();
            tasks.addAll(repository.loadTasks());

            nextTaskId = tasks.stream()
                    .mapToInt(Task::getId)
                    .max().orElse(0) + 1;

            if (args.length == 0) {
                printUsage();
                return;
            }

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
        } catch (IOException exception) {
            System.err.println("Error: Could not access task storage: " + exception.getMessage());
        }
    }

    /// Handler methods
    private void handleAdd(String[] args) throws IOException {
        requireArgumentCount(args, 2, "Usage: task-cli add \"description\"");

        String description = requireDescription(args[1]);
        Task task = new Task(nextTaskId, description);
        tasks.add(task);

        repository.saveTasks(tasks);

        nextTaskId++;
        System.out.printf("Task added successfully (ID: %d)%n", task.getId());
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
