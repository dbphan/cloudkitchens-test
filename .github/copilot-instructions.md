# GitHub Copilot Instructions

## Project Overview

This is a **Restaurant Order Simulator** written in **Kotlin** that demonstrates the producer-consumer pattern for restaurant order management using Kotlin coroutines.

## Technology Stack

- **Language**: Kotlin 1.9.20
- **JVM**: Java 17
- **Build Tool**: Gradle with Kotlin DSL
- **Concurrency**: Kotlin Coroutines 1.7.3
- **Testing**: JUnit Platform with Kotlin Test

## Code Style & Conventions

- Follow **Kotlin official code style** conventions
- Use **4-space indentation** for Kotlin files
- Prefer **trailing commas** in multi-line collections and parameter lists
- Use **explicit types** for public APIs, infer types for local variables
- Maximum line length: **120 characters**
- Use **immutability** (val) by default, only use var when necessary

## Architecture Patterns

- **Producer-Consumer Pattern**: Kitchen (producer) and DriverDispatcher (consumer) with ShelfManager as shared state
- **Thread Safety**: Use proper synchronization for shared mutable state
- **Coroutines**: Use structured concurrency with proper scope management
- **Model-based**: Separate data models from business logic

## Package Structure

```
com.cloudkitchens.simulator
├── model/          # Data models (Order, Shelf)
├── producer/       # Producer components (Kitchen)
├── consumer/       # Consumer components (DriverDispatcher)
├── shelf/          # Shared state management (ShelfManager)
└── Main.kt         # Application entry point
```

## Coding Guidelines

### Kotlin Best Practices

- Use **data classes** for DTOs and models
- Prefer **sealed classes** for representing restricted hierarchies
- Use **extension functions** to enhance readability
- Leverage **scope functions** (let, run, apply, also, with) appropriately
- Use **nullable types** explicitly and handle them safely

### Coroutines

- Use **suspend functions** for asynchronous operations
- Prefer **Flow** for streams of data
- Use **Mutex** or **synchronized blocks** for thread-safe operations
- Always use **structured concurrency** - launch coroutines within appropriate scopes
- Handle **cancellation** properly in long-running operations

### Testing

- Write **unit tests** for models and business logic
- Use **kotlinx-coroutines-test** for testing coroutines
- Follow **AAA pattern** (Arrange, Act, Assert)
- Test file naming: `<ClassName>Test.kt`

## File Naming

- Classes: PascalCase (e.g., `ShelfManager.kt`)
- Functions: camelCase
- Constants: SCREAMING_SNAKE_CASE in companion objects

## Comments & Documentation

- **Document all classes and methods** with KDoc comments
- Use **KDoc** format for public APIs
- Include parameter descriptions, return values, and exceptions thrown
- Document **why** not **what** for complex logic
- Keep comments concise and up-to-date

## Dependencies

When suggesting new dependencies:

- Prefer **official Kotlin/JetBrains libraries**
- Use **stable versions** from Maven Central
- Consider **lightweight alternatives** first

## Error Handling

- Use **Result type** or exceptions for error handling
- Prefer **specific exceptions** over generic ones
- Always clean up resources in finally blocks or use `use` function

## Documentation

- **Always review and update the README.md** when new code or features are added
- Ensure README accurately reflects current architecture, features, and usage
- Update code examples if API changes affect documentation
- Add new features to the Features section with appropriate checkmarks

## When Making Suggestions

- Prioritize **readability** and **maintainability**
- Consider **performance** for concurrent operations
- Suggest **idiomatic Kotlin** solutions
- Keep the existing architecture pattern consistent
