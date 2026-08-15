package tasktracker.model;

import java.time.Instant;

public class Task {
    private final int id;
    private String description;
    private TaskStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Task(int id, String description) {
        Instant now = Instant.now();

        if (id <= 0) {
            throw new IllegalArgumentException("Task ID must be positive");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Task description cannot be empty");
        }

        this.description = description;
        this.id = id;
        status = TaskStatus.TODO;
        createdAt = now;
        updatedAt = now;
    }

    public Task(int id, String description, TaskStatus status, Instant createdAt, Instant updatedAt) {
        if (id <= 0) {
            throw new IllegalArgumentException("Task ID must be positive");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Task description cannot be empty");
        }

        if (status == null) {
            throw new IllegalArgumentException("Task status cannot be null");
        }

        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Task timestamps cannot be null");
        }

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "Updated timestamp cannot be before creation timestamp"
            );
        }

        this.id = id;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDescription(String newDescription) {

        if (newDescription == null || newDescription.isBlank()) {
            throw new IllegalArgumentException("Task description cannot be empty");
        }

        description = newDescription;
        updatedAt = Instant.now();
    }

    public void updateStatus(TaskStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Task status cannot be null");
        }

        status = newStatus;
        updatedAt = Instant.now();
    }
}
