![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Static Badge](https://img.shields.io/badge/Roadmap-black?style=for-the-badge&logo=roadmapdotsh)

# Task Tracker CLI

A command-line application for creating, updating, deleting, and tracking tasks.
The project is implemented with Java 17 and stores its data in a JSON file without
using external libraries or frameworks.

This project is based on the [roadmap.sh Task Tracker challenge](https://roadmap.sh/projects/task-tracker).

> Want the complete explanation?
> Read the [English project guide](docs/PROJECT_GUIDE.md) or the
> [guia completo em Português](docs/GUIA_DO_PROJETO_PT_BR.md).

## Features

- Add tasks with automatically generated positive IDs.
- Update task descriptions.
- Delete tasks.
- Mark tasks as `in-progress` or `done`.
- List every task or filter by `todo`, `in-progress`, or `done`.
- Preserve statuses and ISO-8601 timestamps between executions.
- Reject malformed JSON, duplicate IDs, invalid commands, and invalid input.
- Protect persisted data with temporary files and atomic replacement when
  supported by the filesystem.
- Return meaningful process exit codes for scripts and automation.
- Run automated model, repository, and CLI workflow tests.
- Package production classes as an executable JAR.

## Requirements

- JDK 17 or newer.
- No external dependencies, frameworks, or build tools.

Check your Java installation:

```bash
java -version
javac -version
```

## Quick start

Compile the application from the project root:

```bash
javac -d out \
  src/tasktracker/Main.java \
  src/tasktracker/cli/TaskCli.java \
  src/tasktracker/model/Task.java \
  src/tasktracker/model/TaskStatus.java \
  src/tasktracker/repository/JsonTaskRepository.java
```

Add and list tasks:

```bash
java -cp out tasktracker.Main add "Buy groceries"
java -cp out tasktracker.Main list
```

The application creates `tasks.json` in the current working directory when the
file does not exist. This runtime file is ignored by Git.

## Command reference

| Command | Description |
| --- | --- |
| `add "description"` | Create a task with `todo` status. |
| `update <id> "description"` | Change a task description. |
| `delete <id>` | Delete a task. |
| `mark-in-progress <id>` | Change a task status to `in-progress`. |
| `mark-done <id>` | Change a task status to `done`. |
| `list` | Display every task. |
| `list <status>` | Display tasks matching `todo`, `in-progress`, or `done`. |

Example workflow:

```bash
java -cp out tasktracker.Main add "Buy groceries"
java -cp out tasktracker.Main update 1 "Buy groceries and cook dinner"
java -cp out tasktracker.Main mark-in-progress 1
java -cp out tasktracker.Main list in-progress
java -cp out tasktracker.Main mark-done 1
java -cp out tasktracker.Main delete 1
```

## Data format

Each task contains an ID, description, status, creation timestamp, and update
timestamp:

```json
[
  {
    "id": 1,
    "description": "Buy groceries",
    "status": "todo",
    "createdAt": "2026-08-16T12:00:00Z",
    "updatedAt": "2026-08-16T12:00:00Z"
  }
]
```

## Architecture

```text
Main
└── TaskCli
    ├── Task / TaskStatus
    └── JsonTaskRepository
        └── tasks.json
```

- `Main` wires the application and forwards exit codes to the operating system.
- `TaskCli` validates commands and coordinates task operations.
- `Task` and `TaskStatus` protect domain rules.
- `JsonTaskRepository` owns file creation, JSON conversion, validation, and
  safe replacement.

See the study guides for the complete request flow and the reasoning behind each
layer.

## Project structure

```text
.
├── docs/
│   ├── GUIA_DO_PROJETO_PT_BR.md
│   └── PROJECT_GUIDE.md
├── src/
│   └── tasktracker/
│       ├── Main.java
│       ├── cli/
│       │   └── TaskCli.java
│       ├── model/
│       │   ├── Task.java
│       │   └── TaskStatus.java
│       └── repository/
│           └── JsonTaskRepository.java
└── test/
    └── tasktracker/
        ├── cli/
        │   └── TaskCliTest.java
        ├── model/
        │   └── TaskTest.java
        └── repository/
            └── JsonTaskRepositoryTest.java
```

## Running the tests

Compile production and test sources:

```bash
javac -d out/test \
  src/tasktracker/Main.java \
  src/tasktracker/cli/TaskCli.java \
  src/tasktracker/model/Task.java \
  src/tasktracker/model/TaskStatus.java \
  src/tasktracker/repository/JsonTaskRepository.java \
  test/tasktracker/model/TaskTest.java \
  test/tasktracker/repository/JsonTaskRepositoryTest.java \
  test/tasktracker/cli/TaskCliTest.java
```

Run the three test suites with Java assertions enabled:

```bash
java -ea -cp out/test tasktracker.model.TaskTest
java -ea -cp out/test tasktracker.repository.JsonTaskRepositoryTest
java -ea -cp out/test tasktracker.cli.TaskCliTest
```

## Building the executable JAR

Compile production classes and create the distribution:

```bash
javac -d out/package \
  src/tasktracker/Main.java \
  src/tasktracker/cli/TaskCli.java \
  src/tasktracker/model/Task.java \
  src/tasktracker/model/TaskStatus.java \
  src/tasktracker/repository/JsonTaskRepository.java

mkdir -p dist
jar --create \
  --file dist/task-tracker-cli.jar \
  --main-class tasktracker.Main \
  -C out/package .
```

Run the packaged application:

```bash
java -jar dist/task-tracker-cli.jar list
```

## Exit codes

| Code | Meaning |
| --- | --- |
| `0` | Command completed successfully. |
| `1` | Invalid command, argument, task data, or storage content. |
| `2` | The application could not access task storage. |

## Documentation

- [Complete project guide — English](docs/PROJECT_GUIDE.md)
- [Guia completo do projeto — Português (Brasil)](docs/GUIA_DO_PROJETO_PT_BR.md)

The guides document the nine implementation stages, architecture, persistence
strategy, error handling, tests, limitations, and the libraries commonly used to
replace the manual implementations in production projects.

## Project status

Complete. The application satisfies the requirements of the
[roadmap.sh Task Tracker challenge](https://roadmap.sh/projects/task-tracker).
The deliberately manual JSON, command parsing, testing, and packaging code exists
to expose the fundamentals normally hidden by libraries and build tools.
