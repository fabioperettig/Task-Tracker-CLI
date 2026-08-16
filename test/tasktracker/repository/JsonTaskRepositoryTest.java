package tasktracker.repository;

import tasktracker.model.Task;
import tasktracker.model.TaskStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JsonTaskRepositoryTest {
    public static void main(String[] args) throws Exception {
        shouldInitializeEmptyStorage();
        shouldSaveAndLoadTasks();

        System.out.println("JsonTaskRepositoryTest: all tests passed");
    }

    private static void shouldInitializeEmptyStorage() throws Exception {
        Path directory = Files.createTempDirectory("tasktracker-test-");
        Path file = directory.resolve("tasks.json");

        try {
            JsonTaskRepository repository = new JsonTaskRepository(file);
            repository.initialize();

            assert Files.exists(file) : "Storage file should be created";
            assert repository.loadTasks().isEmpty() : "New storage should contain no tasks";

        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static void shouldSaveAndLoadTasks() throws Exception {
        Path directory = Files.createTempDirectory("tasktracker-test-");
        Path file = directory.resolve("tasks.json");

        try {
            JsonTaskRepository repository = new JsonTaskRepository(file);

            Task original = new Task(7, "Quotes \" slash \\ and newline\n");
            original.updateStatus(TaskStatus.IN_PROGRESS);
            repository.saveTasks(List.of(original));

            List<Task> loadedTasks = repository.loadTasks();

            assert loadedTasks.size() == 1 : "Repository should load one task";

            Task loaded = loadedTasks.get(0);

            assert loaded.getId() == original.getId()
                    : "Task ID should survive persistence";

            assert loaded.getDescription().equals(original.getDescription())
                    : "Description should survive persistence";

            assert loaded.getStatus() == original.getStatus()
                    : "Status should survive persistence";

            assert loaded.getCreatedAt().equals(original.getCreatedAt())
                    : "Creation timestamp should survive persistence";

            assert loaded.getUpdatedAt().equals(original.getUpdatedAt())
                    : "Update timestamp should survive persistence";
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }
}
