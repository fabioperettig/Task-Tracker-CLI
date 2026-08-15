package tasktracker.model;

public enum TaskStatus {
    TODO("todo"),
    IN_PROGRESS("in-progress"),
    DONE("done");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    public static TaskStatus fromValue(String value) {
        for (TaskStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException(
                "Invalid task status: " + value
        );
    }

    public String getValue() {
        return value;
    }
}
