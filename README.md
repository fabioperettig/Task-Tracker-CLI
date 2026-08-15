![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Static Badge](https://img.shields.io/badge/Roadmap-black?style=for-the-badge&logo=roadmapdotsh)
# Task Tracker CLI

A command-line application for creating, updating, deleting, and tracking tasks.
The project is implemented with Java 17 and stores its data in a JSON file without
using external libraries or frameworks.

This project is based on the [roadmap.sh Task Tracker challenge](https://roadmap.sh/projects/task-tracker).

## Project status

- [x] Step 1: Model tasks and their allowed statuses
- [x] Step 2: Parse commands and positional arguments
- [x] Step 3: Add tasks in memory
- [x] Step 4: Store tasks in a JSON file
- [ ] Step 5: List and filter tasks
- [ ] Step 6: Update and delete tasks
- [ ] Step 7: Change task statuses
- [ ] Step 8: Handle errors and edge cases
- [ ] Step 9: Test and package the application

## Completed: Step 1 — Domain model

The first step introduces the core domain objects:

- `Task` represents a task and protects its state through encapsulation.
- `TaskStatus` restricts task statuses to `todo`, `in-progress`, and `done`.
- New tasks start with the `todo` status.
- A task rejects empty descriptions and null statuses.
- `id` and `createdAt` cannot change after creation.
- Changing a description or status updates `updatedAt`.
- `createdAt` and `updatedAt` are identical when a task is first created.

The timestamps use `java.time.Instant`, which produces an ISO-8601 instant that
can later be stored consistently in JSON.

## Completed: Step 2 — Command-line parsing

The command-line interface now:

- Keeps `Main` as a small application entry point.
- Delegates command-line behavior to `TaskCli`.
- Recognizes `add`, `update`, `delete`, `mark-in-progress`, `mark-done`, and
  `list` commands.
- Accepts an optional status filter for `list`.
- Validates positional argument counts before accessing array elements.
- Converts task IDs from text to positive integers.
- Rejects missing descriptions, invalid IDs, unknown commands, and invalid
  status filters with clear error messages.
- Prints a usage guide when the application receives no command.

At this stage, the handlers confirm that each command was parsed successfully.
Except for `add`, they do not execute task operations yet.

## Completed: Step 3 — Add tasks in memory

The `add` command now:

- Creates a `Task` with an automatically generated positive ID.
- Stores the task in memory for the lifetime of the `TaskCli` instance.
- Assigns sequential IDs starting at `1`.
- Prints the ID of the newly created task.

This in-memory implementation provided the foundation for the JSON persistence
introduced in Step 4.

## Completed: Step 4 — JSON file storage

The application now persists tasks between separate executions:

- `JsonTaskRepository` creates `tasks.json` automatically when necessary.
- Tasks are serialized using only Java's standard library.
- Existing tasks are loaded before each command is executed.
- IDs continue from the highest persisted task ID.
- Task statuses and timestamps are preserved when tasks are loaded.
- Special characters in descriptions are escaped and restored.
- Empty task arrays are handled correctly.
- Invalid storage data produces a clear error instead of silently resetting tasks.

## Current project structure

```text
.
├── tasks.json
└── src/
    └── tasktracker/
        ├── Main.java
        ├── cli/
        │   └── TaskCli.java
        ├── model/
        │   ├── Task.java
        │   └── TaskStatus.java
        └── repository/
            └── JsonTaskRepository.java
```

The application creates `tasks.json` in the current working directory when the
file does not exist.

## Running the current version

Compile the source files from the project root:

```bash
javac -d out src/tasktracker/Main.java \
  src/tasktracker/cli/TaskCli.java \
  src/tasktracker/model/Task.java \
  src/tasktracker/model/TaskStatus.java \
  src/tasktracker/repository/JsonTaskRepository.java
```

Run the application:

```bash
java -cp out tasktracker.Main add "Buy groceries"
```

The current version persists added tasks and parses and validates the remaining
commands. Listing, updating, deleting, and changing statuses will be implemented
in the next steps.
