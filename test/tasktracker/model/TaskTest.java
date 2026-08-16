package tasktracker.model;

public class TaskTest {

    public static void main(String[] args) {
        shouldCreateTaskWithDefaultValues();
        shouldRejectBlankDescription();
        shouldRejectInvalidId();

        System.out.println("TaskTest: all tests passed");
    }

    private static void shouldCreateTaskWithDefaultValues() {
        Task task = new Task(1, "Study Java");

        assert task.getId() == 1
                : "Task ID should be preserved";

        assert task.getDescription().equals("Study Java")
                : "Task description should be preserved";

        assert task.getStatus() == TaskStatus.TODO
                : "New task should start as todo";

        assert task.getCreatedAt().equals(task.getUpdatedAt())
                : "New task timestamps should be equal";
    }

    private static void shouldRejectBlankDescription() {
        boolean exceptionThrown = false;

        try {
            new Task(1, " ");
        } catch (IllegalArgumentException exception) {
            exceptionThrown = true;
        }

        assert exceptionThrown
                : "Blank description should be rejected";
    }

    private static void shouldRejectInvalidId() {
        boolean exceptionThrown = false;

        try {
            new Task(0, "Invalid task");
        } catch (IllegalArgumentException exception) {
            exceptionThrown = true;
        }

        assert exceptionThrown
                : "Non-positive ID should be rejected";
    }
}
