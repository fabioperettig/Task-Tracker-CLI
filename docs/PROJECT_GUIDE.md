# Task Tracker CLI — Complete Project Guide

[Back to README](../README.md) ·
[Leia em Português](GUIA_DO_PROJETO_PT_BR.md)

## 1. Purpose of this guide

This document explains how the Task Tracker CLI was built, why each layer exists,
how data moves through the application, what each implementation stage teaches,
and which production libraries normally replace the manual code.

The project intentionally avoids external dependencies. That makes some parts
more verbose than a typical professional Java application, especially JSON
conversion, command parsing, testing, and packaging. The goal is to expose the
mechanics that frameworks and libraries usually hide.

## 2. Project goal and constraints

The application implements the
[roadmap.sh Task Tracker challenge](https://roadmap.sh/projects/task-tracker).
It supports:

- Creating tasks.
- Updating descriptions.
- Deleting tasks.
- Marking tasks as `in-progress` or `done`.
- Listing every task.
- Filtering tasks by `todo`, `in-progress`, or `done`.
- Persisting data in a JSON file in the current working directory.

The project uses:

- Java 17.
- Positional command-line arguments.
- The Java standard library only.
- A local `tasks.json` file.
- Java assertions for tests.
- The JDK's `javac`, `java`, and `jar` tools.

## 3. How one command flows through the application

For this command:

```bash
java -cp out tasktracker.Main add "Study Java"
```

the flow is:

```text
Operating system
└── Main.main(args)
    └── TaskCli.run(args)
        ├── JsonTaskRepository.initialize()
        ├── JsonTaskRepository.loadTasks()
        ├── validate and dispatch "add"
        ├── create Task
        ├── JsonTaskRepository.saveTasks()
        └── return exit code
```

Every terminal invocation starts a new JVM. Memory from a previous invocation no
longer exists, so `TaskCli` reloads the JSON file before executing the command.
After a mutation, it writes the complete task list back to disk.

## 4. Architecture and responsibilities

### `Main`: application boundary

`Main` creates the concrete dependencies:

```java
JsonTaskRepository repository =
        new JsonTaskRepository(Path.of("tasks.json"));
TaskCli cli = new TaskCli(repository);
```

It then forwards the exit code returned by `TaskCli` to the operating system.
`System.exit` stays at this outer boundary so the inner application logic can be
tested without terminating the test JVM.

### `TaskCli`: command orchestration

`TaskCli` is responsible for:

- Loading persisted tasks.
- Validating argument counts.
- Parsing positive numeric IDs.
- Converting status text into `TaskStatus`.
- Dispatching commands to handlers.
- Locating tasks by ID.
- Coordinating model changes and persistence.
- Formatting terminal output.
- Translating failures into process exit codes.

It does not know the details of JSON syntax. That responsibility belongs to the
repository.

### `Task`: domain entity

`Task` owns the state and rules of one task:

- `id` must be positive and cannot change.
- `description` cannot be null, empty, or blank.
- New tasks begin with `todo` status.
- `status` cannot be null.
- `createdAt` cannot change.
- Updating the description or status refreshes `updatedAt`.
- A restored task cannot have `updatedAt` before `createdAt`.

There are two construction scenarios:

1. A new task receives an ID and description. The model supplies the default
   status and current timestamps.
2. A persisted task is restored with all five stored properties.

This distinction prevents loading from silently replacing persisted timestamps
or statuses with new values.

### `TaskStatus`: valid state set

`TaskStatus` is an enum containing:

- `TODO("todo")`
- `IN_PROGRESS("in-progress")`
- `DONE("done")`

An enum is safer than unrestricted strings: invalid states cannot be stored in a
valid `Task` instance. `fromValue` handles conversion at the application and
storage boundaries.

### `JsonTaskRepository`: persistence boundary

The repository owns:

- Creating the storage file.
- Serializing tasks.
- Escaping JSON string characters.
- Reading and deserializing tasks.
- Validating the complete stored structure.
- Rejecting duplicate IDs.
- Preserving timestamps and statuses.
- Replacing the data file safely.

Keeping these details out of `TaskCli` makes command behavior easier to read and
test.

## 5. The data model

The file is a JSON array:

```json
[
  {
    "id": 1,
    "description": "Study Java",
    "status": "todo",
    "createdAt": "2026-08-16T12:00:00Z",
    "updatedAt": "2026-08-16T12:00:00Z"
  }
]
```

### Why `Instant`?

`java.time.Instant` represents an unambiguous moment in UTC and produces an
ISO-8601 value suitable for storage. It avoids locale-dependent strings such as
`16/08/2026 09:00`, which may be interpreted differently across systems.

### ID generation

Before adding a task, the CLI finds the highest persisted ID and uses the next
integer. `Math.addExact` detects integer overflow:

```text
highest ID -> add exactly 1 -> new ID or explicit error
```

The current strategy guarantees uniqueness among existing tasks. One consequence
is that deleting the highest task allows that ID to be reused later. A production
database sequence would normally keep IDs monotonic and avoid reuse.

## 6. JSON persistence in detail

### Initialization

If `tasks.json` does not exist, the repository creates it with an empty array:

```json
[]
```

Initialization never overwrites an existing file.

### Serialization

The serializer uses `StringBuilder` to convert every `Task` into a JSON object.
It writes commas between objects but not after the final object.

Descriptions escape:

- Backslash.
- Double quote.
- Newline.
- Carriage return.
- Tab.

Escaping the backslash first is important. Doing it last would escape the
backslashes introduced by the other replacements.

### Deserialization

The repository reads the complete file and checks the surrounding JSON array. A
regular expression extracts the five fields from each object. Each captured value
is converted to its Java type:

- ID -> `int`
- Description -> unescaped `String`
- Status -> `TaskStatus`
- Timestamps -> `Instant`

The parser tracks where every match ends. Text between objects must be exactly a
comma, and no unknown content may remain after the final object. This prevents
`Matcher.find()` from silently accepting a valid object hidden inside malformed
content.

A `HashSet<Integer>` detects duplicate IDs during loading.

### Safe file replacement

Writing directly to `tasks.json` could damage the old data if the process failed
after truncating the file. The repository instead:

1. Serializes the new list.
2. Writes it to a temporary file in the same directory.
3. Attempts an atomic move over `tasks.json`.
4. Falls back to a regular replacement when atomic moves are unsupported.
5. Removes a leftover temporary file in a `finally` block.

An exception reports a problem; this replacement strategy also reduces the chance
of losing the previously valid file.

### Important parser limitations

This is an educational, schema-specific parser, not a general JSON implementation.
It expects the fields generated by this application and in their known order.
It does not support every valid JSON representation, arbitrary field ordering,
unknown properties, every control-character escape, or Unicode escape sequences.

Those limitations are a primary reason production applications use a mature JSON
library.

## 7. Command behavior

### Add

```bash
task-cli add "description"
```

The CLI validates the description, finds the next ID, constructs a new `Task`,
adds it to the working list, and persists the list.

### Update

```bash
task-cli update <id> "description"
```

The CLI parses the ID, finds the task, delegates the change to
`Task.updateDescription`, and persists the result. The model updates
`updatedAt`.

### Delete

```bash
task-cli delete <id>
```

The CLI finds the exact object in the list, removes it, and persists the remaining
tasks.

### Change status

```bash
task-cli mark-in-progress <id>
task-cli mark-done <id>
```

Both handlers share one status-change method. This avoids duplicating the lookup,
model update, save, and output logic.

### List and filter

```bash
task-cli list
task-cli list todo
task-cli list in-progress
task-cli list done
```

Without a filter, every task is printed. With a filter, enum identity is used to
compare statuses. A clear message is printed when no task matches.

## 8. Exceptions, handling, and exit codes

Exceptions are the signaling mechanism, not the entire error-handling strategy.
The application also decides:

- Where validation belongs.
- Which layer should translate the error.
- What the user should see.
- Whether stored data remains protected.
- Which process code should be returned.

`TaskCli.run` catches:

- `IllegalArgumentException` for invalid commands, arguments, domain values, or
  stored task data.
- `IOException` for filesystem failures.

The process codes are:

| Code | Meaning |
| --- | --- |
| `0` | Success. |
| `1` | Invalid command, input, task data, or JSON content. |
| `2` | Storage access failure. |

Returning the code from `TaskCli` instead of calling `System.exit` there makes
the CLI directly testable.

## 9. The nine implementation stages

### Stage 1 — Domain model

Created `Task` and `TaskStatus`. The focus was encapsulation, invariants,
immutability for identity and creation time, and controlled state changes.

### Stage 2 — Command parsing

Introduced `TaskCli`, command dispatch, argument-count validation, ID parsing,
status parsing, usage output, and error messages. Handlers initially confirmed
parsing without changing data.

### Stage 3 — In-memory addition

Connected `add` to actual `Task` creation and a list. This exposed a process
lifetime problem: a separate CLI invocation starts with empty memory.

### Stage 4 — JSON persistence

Introduced `JsonTaskRepository`, storage initialization, serialization,
deserialization, restoration constructors, escaping, and persistent ID
calculation. JSON replaced memory as the source of truth between processes.

### Stage 5 — Listing and filtering

Connected `list` to the loaded task collection and added optional enum-based
status filtering.

### Stage 6 — Update and delete

Added reusable lookup by ID, delegated description changes to the model, persisted
updates, and removed tasks safely.

### Stage 7 — Status changes

Connected both status commands to `Task.updateStatus` and extracted their shared
workflow.

### Stage 8 — Hardening

Added complete JSON-consumption checks, duplicate-ID detection, atomic file
replacement, cleanup, overflow protection, user-facing errors, and process exit
codes.

### Stage 9 — Tests and packaging

Separated test sources, added model, repository, and CLI workflow tests, compiled
tests with assertions enabled, and created an executable JAR containing production
classes only.

## 10. Testing strategy

### Model tests

`TaskTest` verifies:

- Default description, status, and timestamps.
- Rejection of blank descriptions.
- Rejection of non-positive IDs.

### Repository integration tests

`JsonTaskRepositoryTest` uses temporary directories and verifies:

- Creation of empty storage.
- JSON round trips.
- Preservation of special characters.
- Preservation of status and timestamps.

Temporary paths keep tests isolated from the user's real `tasks.json`.

### CLI workflow test

`TaskCliTest` exercises:

```text
add -> add -> update -> mark done -> filtered list -> delete
```

It also checks persisted state and an invalid-status exit code. Standard output is
captured when list content needs to be asserted.

### Why `-ea`?

Java assertions are disabled by default. The tests must run with:

```bash
java -ea ...
```

This lightweight approach preserves the no-dependency constraint. A production
project would normally use a dedicated test framework.

## 11. What production libraries would replace

| Manual implementation in this project | Common production replacement | What it provides |
| --- | --- | --- |
| JSON building, escaping, regex parsing, type conversion | Jackson, Gson, or JSON-B | General JSON parsing, mapping, configuration, and mature edge-case handling. |
| Positional command switch and validation | picocli, JCommander, or Spring Shell | Declarative commands, options, help, conversion, and exit handling. |
| Rewriting a local JSON array | PostgreSQL/MySQL with JDBC, JPA/Hibernate, or Spring Data | Transactions, concurrency, querying, indexing, constraints, and generated IDs. |
| Repeated constructor and field validation | Jakarta Bean Validation / Hibernate Validator | Declarative constraints such as `@NotBlank` and `@Positive`. |
| Java `assert` test runners | JUnit 5, AssertJ, Mockito | Test discovery, lifecycle, parameterized tests, expressive assertions, and mocks. |
| Manual `javac` and `jar` commands | Maven or Gradle | Reproducible builds, dependency management, test execution, packaging, and plugins. |
| `System.out` and `System.err` | SLF4J with Logback or Log4j 2 | Levels, formatting, destinations, structured logs, and operational control. |
| Concrete dependency creation in `Main` | Spring, Guice, or Dagger | Dependency injection and lifecycle management for larger applications. |
| Temporary-file replacement | Database transactions or storage SDK guarantees | Stronger durability, concurrency control, and recovery behavior. |

### Jackson example

With Jackson, the repository conceptually becomes:

```java
objectMapper.writeValue(file.toFile(), tasks);

List<Task> tasks = objectMapper.readValue(
        file.toFile(),
        new TypeReference<List<Task>>() {}
);
```

Real configuration would also address `Instant`, constructor mapping, unknown
fields, naming rules, and error behavior. The manual project demonstrates why
those library features matter.

### JUnit example

The custom exception checks could become:

```java
assertThrows(
        IllegalArgumentException.class,
        () -> new Task(0, "Invalid")
);
```

JUnit would discover tests automatically, while Maven or Gradle would enable
assertions and run the suite as part of the build.

## 12. Current limitations and production evolution

The current design is appropriate for a learning project and a single local
process. It is not designed for concurrent writers or large datasets.

Important limitations:

- Every command reads and may rewrite the entire file.
- Two processes can read the same state and overwrite each other's changes.
- The parser accepts only the application's known JSON shape.
- IDs can be reused after deleting the highest existing ID.
- There is no transaction spanning memory mutation and file replacement.
- There is no service layer or repository interface.
- Output formatting and command orchestration live in the same CLI class.
- Tests rely on Java assertions and must run with `-ea`.

A typical evolution path would be:

1. Introduce a `TaskRepository` interface.
2. Move use-case logic into a `TaskService`.
3. Replace manual JSON with Jackson or replace the file with a database.
4. Let the storage layer generate IDs.
5. Use picocli for command definitions.
6. Add JUnit 5 and a build tool.
7. Add structured logging.
8. Add concurrency and transaction guarantees where required.

## 13. Suggested study exercises

1. Add a `mark-todo` command.
2. Add a `find <id>` command.
3. Sort list output by creation time or status.
4. Prevent `updatedAt` from changing when the new value equals the old value.
5. Add permanent tests for malformed JSON and duplicate IDs.
6. Extract a `TaskRepository` interface and test `TaskCli` with a fake
   repository.
7. Extract task operations into a `TaskService`.
8. Replace the manual JSON implementation with Jackson and compare code size and
   behavior.
9. Convert the project to Maven or Gradle and migrate tests to JUnit 5.
10. Replace JSON storage with SQLite or PostgreSQL.
11. Add file locking and investigate concurrent CLI processes.
12. Add pagination for large task lists.

## 14. Key lessons

- Domain objects should protect their own valid state.
- Parsing and validation belong at boundaries.
- Persistence is necessary because process memory is temporary.
- Serialization and deserialization form a round trip.
- Exceptions signal failure; graceful handling also requires translation,
  recovery strategy, data protection, and exit semantics.
- Dependency injection can be as simple as passing an object through a
  constructor.
- Tests are easier when side effects are isolated behind dependencies.
- Temporary directories prevent tests from damaging real user data.
- Build tools and libraries automate fundamentals that are still valuable to
  understand.

## 15. Related files

- [Project README](../README.md)
- [Portuguese study guide](GUIA_DO_PROJETO_PT_BR.md)
- [Challenge description](../challenge.md)
- [Main](../src/tasktracker/Main.java)
- [Task CLI](../src/tasktracker/cli/TaskCli.java)
- [Task model](../src/tasktracker/model/Task.java)
- [JSON repository](../src/tasktracker/repository/JsonTaskRepository.java)
