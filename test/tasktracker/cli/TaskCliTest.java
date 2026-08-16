package tasktracker.cli;

import tasktracker.model.Task;
import tasktracker.model.TaskStatus;
import tasktracker.repository.JsonTaskRepository;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class TaskCliTest {

    public static void main(String[] args) throws Exception {
        shouldExecuteTaskWorkflow();

        System.out.println("TaskCliTest: all tests passed");
    }

    private static void shouldExecuteTaskWorkflow()
            throws Exception {
        Path directory = Files.createTempDirectory(
                "tasktracker-cli-test-"
        );
        Path file = directory.resolve("tasks.json");

        try {
            JsonTaskRepository repository = new JsonTaskRepository(file);
            TaskCli cli = new TaskCli(repository);

            assert cli.run(new String[]{"add", "First task"}) == 0 : "First add should succeed";
            assert cli.run(new String[]{"add", "Second task"}) == 0 : "Second add should succeed";
            assert cli.run(new String[]{"update", "1", "Updated first task"}) == 0 : "Update should succeed";
            assert cli.run(new String[]{"mark-done", "2"}) == 0 : "Status change should succeed";

            String doneOutput = runAndCaptureOutput(cli, "list", "done");

            assert doneOutput.contains("Second task")
                    : "Done list should contain second task";

            assert !doneOutput.contains("Updated first task")
                    : "Done list should exclude todo task";

            assert cli.run(new String[]{"delete", "1"}) == 0 : "Delete should succeed";

            List<Task> storedTasks = repository.loadTasks();

            assert storedTasks.size() == 1
                    : "One task should remain";

            assert storedTasks.get(0).getId() == 2
                    : "Task 2 should remain";

            assert storedTasks.get(0).getStatus() == TaskStatus.DONE
                    : "Remaining task should be done";

            assert cli.run(new String[]{"list", "invalid"}) == 1
                    : "Invalid status should return code 1";
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static String runAndCaptureOutput(TaskCli cli, String... args) {
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        PrintStream capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);

        try {
            System.setOut(capturedOutput);
            int exitCode = cli.run(args);

            assert exitCode == 0 : "Captured command should succeed";

        } finally {
            System.setOut(originalOutput);
            capturedOutput.close();
        }

        return output.toString(StandardCharsets.UTF_8);
    }
}
