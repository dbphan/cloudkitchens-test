# CloudKitchens Challenge Submission

**Author:** Bao Phan  
**Date:** February 4, 2026  
**Branch:** submission

## 📦 Submission Contents

This submission includes:
- ✅ Complete working solution for the CloudKitchens coding challenge
- ✅ Comprehensive test suite (95 tests total)
- ✅ Docker support for consistent execution environment
- ✅ Detailed documentation

## 🚀 Quick Start

### Option 1: Docker (Recommended)
```bash
docker build -t cloudkitchens-challenge .
docker run --rm -it cloudkitchens-challenge --auth=<token>
```

### Option 2: Gradle
```bash
./gradlew run --args="--auth=<token>"
```

### Option 3: Pre-built Distribution
```bash
unzip build/distributions/challenge-1.0.zip
./challenge-1.0/bin/challenge --auth=<token>
```

## ✨ Key Features

### Discard Strategy
- **Algorithm:** Priority Queue (Min-Heap) for sub-linear O(log n) performance
- **Value-based Selection:** Considers freshness, age, shelf life, and temperature mismatch
- **Fair and Predictable:** Automatically discards lowest-value orders

### Architecture
- **Thread-safe:** All operations protected by Mutex
- **Concurrent:** Driver pickups run asynchronously with structured coroutines
- **Temperature-aware:** Separate storage for hot (heater), cold (cooler), and room temperature (shelf)

### Storage Capacity
- Cooler: 6 cold orders at ideal temperature
- Heater: 6 hot orders at ideal temperature
- Shelf: 12 orders at room temperature (overflow storage)

## 🧪 Testing

### Test Coverage: 95 Tests
- **Unit Tests:** 72 tests covering individual components
- **Integration Tests:** 23 tests covering end-to-end workflows
  - 13 success scenarios
  - 10 failure scenarios (API errors, capacity constraints, concurrency)

### Test Style
All tests use **Gherkin BDD** format (Given-When-Then) for clarity:
```kotlin
@Test
fun `Given valid command-line options, When the application parses them, Then it should accept all options without errors`()
```

### Running Tests
```bash
# All tests
./gradlew clean build integrationTest

# Unit tests only
./gradlew test

# Integration tests only
./gradlew integrationTest
```

**Test Results:** ✅ All 95 tests passing

## 🛠️ Technology Stack

- **Kotlin** 2.0.21 with coroutines for async operations
- **Java** 21 LTS
- **Gradle** 8.4 build system
- **Ktor** 3.0.1 HTTP client
- **Clikt** 5.0.1 CLI framework
- **JUnit** 5.10.0 testing framework
- **Docker** multi-stage build for deployment

## 📋 Implementation Highlights

### 1. Sub-linear Discard Algorithm
Uses PriorityQueue (min-heap) for O(log n) insertion and O(1) minimum lookup, meeting the "better than O(n)" requirement.

### 2. Comprehensive Error Handling
- API authentication failures
- Network timeouts and errors
- Malformed JSON responses
- Concurrent access protection

### 3. Realistic Integration Tests
- Tests against real CloudKitchens API (when AUTH_TOKEN provided)
- Mock server for failure scenarios
- Virtual time testing for deterministic coroutine behavior

### 4. Clean Architecture
- Separation of concerns (client, manager, storage)
- Dependency injection ready
- Easy to test and maintain

## 📝 Code Quality

- **Type-safe:** Leverages Kotlin's strong type system
- **Documented:** Clear inline documentation and README
- **Tested:** 95 tests with high coverage
- **Idiomatic:** Follows Kotlin best practices and conventions
- **Maintainable:** Clean, readable code with clear abstractions

## 🐳 Docker Support

The Dockerfile provides a self-contained reference environment:
- Based on `gradle:jdk21-alpine`
- Multi-stage build for efficiency
- Includes all dependencies
- Ready for deployment

## 📊 Build Artifacts

Generated distribution packages:
- `build/distributions/challenge-1.0.zip` - Standard distribution
- `build/distributions/challenge-1.0.tar` - TAR archive
- `build/distributions/challenge-shadow-1.0.zip` - Fat JAR distribution

## ✅ Verification Checklist

- [x] Clean build passes: `./gradlew clean build`
- [x] All 72 unit tests pass
- [x] All 23 integration tests pass
- [x] Docker image builds successfully
- [x] Distribution packages created
- [x] README documentation complete
- [x] Code follows best practices
- [x] Git history clean and organized

## 📧 Contact

For questions or clarifications, please contact:
**Bao Phan** - dbphan@github

---

**Thank you for reviewing this submission!**
