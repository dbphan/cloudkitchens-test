# GitHub Copilot Instructions

## Project Overview

This is a **Cloud Kitchens Take-Home Challenge** - a real-time food order fulfillment system for a delivery-only kitchen. The system manages concurrent order placement, storage with temperature requirements, freshness tracking with decay, and driver pickup simulation.

## Technology Stack

- **Language**: Kotlin 2.0.21
- **JVM**: Java 21
- **Build Tool**: Gradle 8.4 with Kotlin DSL
- **HTTP Client**: Ktor 3.0.1 (CIO engine)
- **CLI Framework**: Clikt 5.0.1
- **Serialization**: kotlinx-serialization-json 1.7.1
- **DateTime**: kotlinx-datetime 0.6.1
- **Concurrency**: Kotlin Coroutines with Mutex for thread safety
- **Testing**: JUnit Jupiter 5.11.3, kotlinx-coroutines-test, ktor-client-mock
- **Containerization**: Docker with Rancher Desktop

## Code Style & Conventions

- Follow **Kotlin official code style** conventions
- Use **4-space indentation** for Kotlin files
- Maximum line length: **120 characters**
- Use **immutability** (val) by default, only use var when necessary
- Prefer **trailing commas** in multi-line collections
- Use **explicit types** for public APIs, infer types for local variables

## Challenge Requirements

### Core Functionality

- **Thread-safe storage**: Cooler (6 cold), Heater (6 hot), Shelf (12 room temp)
- **Freshness tracking**: Orders decay at 1x at ideal temp, 2x at non-ideal temp
- **Sub-linear discard algorithm**: Must be better than O(n) when shelf is full
- **Concurrent operations**: Handle multiple orders arriving and pickups simultaneously
- **Driver simulation**: Random pickup timing within min-max interval
- **Server validation**: Solution must consistently pass challenge server validation

### Business Rules

- Orders have temperature requirement: "hot", "cold", or "room"
- Place orders at ideal temperature storage first, fallback to shelf if full
- When shelf is full: move shelf order to ideal storage if available, or discard order with lowest value
- Order value = (freshness _ shelfLife _ decayModifier) / (orderAge \* temperature_decay_multiplier)
- Track all actions (place, pickup, discard) for server submission

## Package Structure

```
com.css.challenge
├── client/         # API client for challenge server (Client, Order, Action, Problem)
├── model/          # Domain models (StoredOrder with freshness tracking)
├── storage/        # Storage containers (StorageContainer interface, Cooler, Heater, Shelf)
├── manager/        # Business logic (KitchenManager for orchestration)
└── Main.kt         # CLI entry point
```

## Architecture Patterns

- **Thread Safety**: Use Mutex for all shared mutable state (storage containers)
- **Suspend Functions**: All storage operations are suspend for async/concurrent access
- **O(1) Lookups**: Use HashMap for order storage to enable fast retrieval
- **Immutable Data Models**: Use data classes for Order, Action, Problem
- **Coroutines**: Structured concurrency with proper scope management
- **Environment Configuration**: Read sensitive data from .env file (AUTH_TOKEN, ENDPOINT)

## Coding Guidelines

### Kotlin Best Practices

- Use **data classes** for DTOs and models
- Use **suspend functions** for all async operations
- Leverage **scope functions** (let, run, apply, also, with) appropriately
- Use **nullable types** explicitly and handle them safely with safe calls (?.)
- Prefer **when expressions** over if-else chains
- Use **extension functions** to enhance readability

### Thread Safety & Concurrency

- **Always use Mutex** for mutable shared state (storage containers)
- Use **mutex.withLock { }** for atomic operations
- Prefer **suspend functions** over blocking calls
- Use **structured concurrency** - launch coroutines within appropriate scopes
- Handle **cancellation** properly in long-running operations
- **Never block the main thread** - use coroutines for I/O operations

### Storage System

- All storage containers implement **StorageContainer interface**
- Operations: add(), remove(), get(), getAll(), clear() - all are **suspend**
- Properties: capacity, size, temperature, isEmpty(), isFull()
- Use **Mutex.withLock** for all mutations
- Return **null** for missing items instead of throwing exceptions

### Freshness Calculations

- Use **kotlinx-datetime.Instant** for precise timestamps
- Calculate freshness: `(shelfLife - decayRate * orderAge * tempMultiplier) / shelfLife`
- Temperature multiplier: **1.0** at ideal temp, **2.0** at non-ideal temp
- Order is expired when freshness ≤ 0

### Testing

- Write **unit tests** for all models and business logic
- Use **kotlinx-coroutines-test.runTest** for testing suspend functions
- Follow **AAA pattern** (Arrange, Act, Assert)
- Test file naming: `<ClassName>Test.kt` in corresponding test package
- Use **assertEquals, assertTrue, assertFalse, assertNull** from kotlin.test
- Test edge cases: capacity limits, thread safety, expiration, temperature mismatches

## File Naming

- Classes: PascalCase (e.g., `KitchenManager.kt`)
- Functions: camelCase
- Constants: SCREAMING_SNAKE_CASE in companion objects
- Test files: `<ClassName>Test.kt`

## Comments & Documentation

- **Document all public classes and methods** with KDoc comments
- Use **KDoc format** (/\*_ ... _/) for public APIs
- Include:
  - Class/function purpose and responsibility
  - @param descriptions for all parameters
  - @return descriptions for return values
  - @throws for exceptions that may be thrown
- Document **why** not **what** for complex logic (e.g., discard algorithm)
- Keep comments concise, clear, and up-to-date

## Error Handling

- Use **Result type** or exceptions for error handling
- Log errors with meaningful context
- Always validate API responses before processing
- Handle network failures gracefully (Ktor client errors)
- Check for null values when retrieving orders from storage

## Environment & Configuration

- Store secrets in **.env** file (never commit to git)
- Use **System.getenv()** to read environment variables
- Provide sensible defaults for optional configuration
- Document required environment variables in .env.example

## Docker

- Base image: **gradle:jdk21-alpine**
- Build stage: Compile Kotlin to JAR
- Runtime stage: Run with java -jar
- Pass AUTH_TOKEN as environment variable
- Use scripts/run-docker.sh and scripts/stop-docker.sh for management

## Testing Strategy

- **Unit tests**: Models (StoredOrder), Storage (Cooler/Heater/Shelf), Manager logic
- **Integration tests**: Full workflow from order placement to pickup/discard
- **Server validation**: Run against challenge server to verify solution
- Aim for **100% pass rate** on all tests before pushing changes
- Test concurrent scenarios with multiple coroutines

## When Making Suggestions

- Prioritize **correctness** and **thread safety** for concurrent operations
- Consider **performance** - use efficient data structures (HashMap for O(1))
- Suggest **idiomatic Kotlin** solutions with coroutines
- Keep code **maintainable** and **testable**
- Follow the existing architecture and patterns
- **Always run tests** after making changes to verify correctness
- Update **PROGRESS.md** checklist when completing major features
- **Always review and update README.md** when adding new code, features, or architecture changes

## README Maintenance

**CRITICAL**: The README.md must always reflect the current state of the project.

When to update README:

- **New features added**: Document what it does and how to use it
- **Architecture changes**: Update diagrams, package structure, or design patterns
- **API changes**: Update code examples if public interfaces change
- **Dependencies added**: List new libraries and their purposes
- **Setup changes**: Update installation or configuration steps
- **New scripts**: Document what they do and how to run them

Always ensure:

- Code examples in README are executable and current
- Feature list shows completed features with ✅ checkmarks
- Architecture section matches actual package structure
- Setup instructions work for new developers

## Git Workflow

- Work on **develop** branch for feature development
- Commit messages: Follow conventional commits (feat:, fix:, test:, docs:, refactor:)
- Include detailed commit descriptions explaining what changed and why
- Push to remote after completing and testing features
- Exclude build artifacts (.gradle/, bin/, build/) from commits

### Commit Message Best Practice

For complex commits with multi-line messages:

1. **Check git status** to see all modified/untracked files
2. **Ask user which files to add** - show the list and confirm which files should be included in the commit
3. **Stage the approved files**: `git add <files>`
4. **Create temporary commit message file**: `COMMIT_MSG.md` in project root
5. **Write detailed commit message** in the file with proper formatting
6. **Use file for commit**: `git commit -F COMMIT_MSG.md`
7. **Remove file after successful commit**: `rm COMMIT_MSG.md`

This ensures user reviews what's being committed and avoids terminal issues with multi-line strings.

## Documentation

- Keep **README.md** up-to-date with current architecture and features
- Update **PROGRESS.md** checklist as tasks are completed
- Document **scripts/** usage for running and stopping the application
- Include setup instructions for new developers
