package tasktracker.repository;

import tasktracker.model.Task;
import tasktracker.model.TaskStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonTaskRepository {
    private final Path filePath;

    private static final Pattern TASK_PATTERN = Pattern.compile(
            "\\{\\s*"
                    + "\"id\"\\s*:\\s*(\\d+)\\s*,\\s*"
                    + "\"description\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"\\s*,\\s*"
                    + "\"status\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*"
                    + "\"createdAt\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*"
                    + "\"updatedAt\"\\s*:\\s*\"([^\"]+)\"\\s*"
                    + "\\}"
    );

    public JsonTaskRepository(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        this.filePath = filePath;
    }

    public void initialize() throws IOException {
        if (Files.notExists(filePath)) {
            Files.writeString(filePath,"[]\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
        }
    }

    public void saveTasks(List<Task> tasks) throws IOException {
        if (tasks == null) {
            throw new IllegalArgumentException("Task list cannot be null");
        }

        String json = serializeTasks(tasks);

        Files.writeString(filePath, json, StandardCharsets.UTF_8);
    }

    public List<Task> loadTasks() throws IOException {
        String json = Files.readString(filePath,StandardCharsets.UTF_8).trim();

        if (!json.startsWith("[") || !json.endsWith("]")) {
            throw new IllegalArgumentException("Invalid tasks JSON");
        }

        String arrayContent = json
                .substring(1, json.length() - 1)
                .trim();

        if (arrayContent.isEmpty()) {
            return new ArrayList<>();
        }

        List<Task> tasks = new ArrayList<>();
        Matcher matcher = TASK_PATTERN.matcher(json);

        while (matcher.find()) {
            try {
                int id = Integer.parseInt(matcher.group(1));
                String description = unescapeJson(matcher.group(2));
                TaskStatus status = TaskStatus.fromValue(matcher.group(3));
                Instant createdAt = Instant.parse(matcher.group(4));
                Instant updatedAt = Instant.parse(matcher.group(5));

                tasks.add(new Task(
                        id,
                        description,
                        status,
                        createdAt,
                        updatedAt
                ));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException(
                        "Invalid task data in JSON",
                        exception
                );
            }
        }

        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("Invalid tasks JSON");
        }

        return tasks;
    }

    private String serializeTasks(List<Task> tasks) {
        StringBuilder json = new StringBuilder("[\n");

        for (int index = 0; index < tasks.size(); index++) {
            Task task = tasks.get(index);

            json.append("  {\n")
                    .append("    \"id\": ").append(task.getId())
                    .append(",\n")
                    .append("    \"description\": \"").append(escapeJson(task.getDescription()))
                    .append("\",\n")
                    .append("    \"status\": \"").append(task.getStatus().getValue())
                    .append("\",\n")
                    .append("    \"createdAt\": \"").append(task.getCreatedAt())
                    .append("\",\n")
                    .append("    \"updatedAt\": \"").append(task.getUpdatedAt())
                    .append("\"\n")
                    .append("  }");

            if (index < tasks.size() - 1) {
                json.append(",");
            }

            json.append("\n");
        }

        json.append("]\n");
        return json.toString();
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        StringBuilder result = new StringBuilder();

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);

            if (current != '\\') {
                result.append(current);
                continue;
            }

            if (index + 1 >= value.length()) {
                throw new IllegalArgumentException("Invalid JSON escape");
            }

            char escaped = value.charAt(++index);

            switch (escaped) {
                case '\\' -> result.append('\\');
                case '"' -> result.append('"');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                default -> throw new IllegalArgumentException(
                        "Invalid JSON escape: \\" + escaped
                );
            }
        }

        return result.toString();
    }
}
