# Cloud Kitchens Take-Home Challenge

A real-time food order fulfillment system for a delivery-only kitchen. Manages concurrent order placement, temperature-controlled storage, freshness tracking with decay, and driver pickup simulation.

## Features

- ✅ **Thread-safe storage system**: Cooler (6 cold), Heater (6 hot), Shelf (12 room temp)
- ✅ **Freshness tracking**: Orders decay at 1x at ideal temp, 2x at non-ideal temp
- ✅ **Concurrent operations**: Multiple orders arriving and pickups simultaneously
- ✅ **O(1) storage operations**: HashMap-based order lookup and retrieval
- ✅ **Sub-linear discard algorithm**: PriorityQueue for O(1) minimum value lookup
- ✅ **Driver pickup simulation**: Random timing within min-max interval using coroutines
- ✅ **Full integration**: Complete execution flow from order placement to server validation
- ⏳ **Server validation**: Consistent passing of challenge server tests (in progress)

## Architecture

### Package Structure

```
com.css.challenge
├── client/         # HTTP client for challenge server
│   ├── Client.kt      # API communication (newProblem, solve)
│   ├── Order.kt       # Order data model
│   ├── Action.kt      # Action tracking (place, pickup, discard)
│   └── Problem.kt     # Problem wrapper
├── model/          # Domain models
│   └── StoredOrder.kt # Order with freshness tracking
├── storage/        # Storage containers
│   ├── StorageContainer.kt  # Interface for all storage
│   ├── Cooler.kt            # Cold storage (6 capacity)
│   ├── Heater.kt            # Hot storage (6 capacity)
│   └── Shelf.kt             # Room temp storage (12 capacity)
├── manager/        # Business logic
│   ├── KitchenManager.kt    # Order placement, pickup orchestration, driver scheduling
│   └── DiscardStrategy.kt   # Sub-linear discard algorithm with priority queue
└── Main.kt         # CLI entry point
```

### Key Components

**StoredOrder**: Wraps Order with placement timestamp and calculates real-time freshness
- Freshness formula: `(shelfLife - decayRate * orderAge * tempMultiplier) / shelfLife`
- Temperature multiplier: 1.0 at ideal temp, 2.0 at non-ideal temp
- Tracks order location and movement history

**Storage Containers**: Thread-safe implementations using Mutex
- All operations are suspend functions for async/concurrent access
- HashMap for O(1) lookup by order ID
- Properties: capacity, size, temperature, isEmpty(), isFull()

**DiscardStrategy**: Sub-linear order discard algorithm
- PriorityQueue (min-heap) for O(1) minimum value lookup
- Value calculation: `(freshness × freshnessLimit) / ((orderAge + 1) × tempMultiplier)`
- Automatically maintains lowest-value order for shelf overflow scenarios

**KitchenManager**: Orchestrates order placement and pickup
- Places orders at ideal temperature storage first, falls back to shelf
- Handles shelf overflow by moving orders or discarding lowest-value order
- Schedules driver pickups with random delays using coroutines
- Validates freshness during pickup (discards expired orders)
- Tracks all actions (place, move, discard, pickup) for server submission

## Technology Stack

- **Language**: Kotlin 2.0.21
- **JVM**: Java 21
- **Build Tool**: Gradle 8.4 with Kotlin DSL
- **HTTP Client**: Ktor 3.0.1 (CIO engine)
- **CLI**: Clikt 5.0.1
- **Serialization**: kotlinx-serialization-json 1.7.1
- **DateTime**: kotlinx-datetime 0.6.1
- **Concurrency**: Kotlin Coroutines with Mutex
- **Testing**: JUnit Jupiter 5.11.3, kotlinx-coroutines-test
- **Docker**: Rancher Desktop with gradle:jdk21-alpine

## Setup

### Prerequisites

- Java 21 or later
- Docker (optional, for containerized execution)

### Configuration

Create a `.env` file in the project root (never commit this):

```bash
AUTH_TOKEN=your_token_here
ENDPOINT=https://api.cloudkitchens.com
```

See `.env.example` for template.

## Execution Flow

The application executes the following workflow:

1. **Fetch Problem**: Retrieves order list from Cloud Kitchens API
2. **Initialize Kitchen**: Sets up thread-safe storage (Cooler, Heater, Shelf)
3. **Process Orders**: Places orders sequentially at configured rate (--rate)
   - Attempts ideal temperature storage first
   - Falls back to shelf if ideal storage is full
   - Handles shelf overflow with move-or-discard strategy
4. **Schedule Pickups**: Launches concurrent coroutines for each order
   - Random delay between --min and --max seconds
   - Non-blocking execution using Kotlin coroutines
5. **Wait for Completion**: Joins all pickup coroutines
6. **Submit Results**: Sends action ledger to server for validation
7. **Display Outcome**: Shows server validation result

All actions (PLACE, MOVE, DISCARD, PICKUP) are tracked with timestamps for server submission.

## How to Run

### Local Execution (Recommended for Development)

```bash
# Using environment variables from .env
./scripts/run.sh

# Or directly with Gradle
./gradlew run --args="--auth=<token>"

# Stop the application
./scripts/stop.sh
```

### Docker Execution

```bash
# Build and run with Docker
./scripts/run-docker.sh

# Or manually
docker build -t challenge .
docker run --rm -it challenge --auth=<token>

# Stop Docker container
./scripts/stop-docker.sh
```

### CLI Options

```bash
--auth=<token>        # Authentication token (required, or set AUTH_TOKEN env var)
--endpoint=<url>      # API endpoint (optional, defaults to env var or api.cloudkitchens.com)
--name=<problem>      # Problem name (optional)
--seed=<number>       # Seed for deterministic problems (optional)
--rate=<duration>     # Order rate, e.g., 500ms (optional, default: 500ms)
--min=<seconds>       # Min driver pickup time (optional)
--max=<seconds>       # Max driver pickup time (optional)
```

## Testing

Run all tests:

```bash
./gradlew test
```

View detailed test report:

```bash
open build/reports/tests/test/index.html
```

### Test Coverage

**72 tests total** (100% passing):
- **StoredOrderTest** (10 tests): Freshness calculations, temperature matching, expiration
- **StorageContainerTest** (13 tests): Cooler, Heater, Shelf capacity and operations
- **KitchenManagerTest** (7 tests): Order placement logic and action tracking
- **ClientTest** (6 tests): API client operations (fetch problem, submit ledger)
- **DiscardStrategyTest** (12 tests): Priority queue discard algorithm, value calculations
- **DriverPickupSimulationTest** (7 tests): Random pickup delays, concurrent operations
- **Integration tests** (17 tests): Complete workflows including placement, storage, and pickup
- **MainTest** (7 tests): CLI argument parsing and validation
- **ClientTest** (4 tests): HTTP client and API communication
- **OrderTest** (9 tests): Order serialization and validation
- **ActionTest** (5 tests): Action tracking and constants
- **ProblemTest** (5 tests): Problem wrapper and parsing

## Development

### Code Style

- Follow Kotlin official conventions
- 4-space indentation
- Maximum line length: 120 characters
- Use immutability (val) by default
- Document all public APIs with KDoc

### Concurrency Patterns

- All storage operations use `Mutex.withLock` for thread safety
- Suspend functions for all async operations
- Structured concurrency with proper scope management
- Never block the main thread

### Git Workflow

```bash
# Work on develop branch
git checkout develop

# Run tests before committing
./gradlew test

# Commit with conventional format
git add <files>
git commit -m "feat: description"
git push origin develop
```

## Discard Criteria

**Current Implementation**: When shelf is full, the system needs to discard the order with the lowest value.

**Value Formula**: 
```
value = (freshness * shelfLife * decayModifier) / (orderAge * tempMultiplier)
```

**Sub-linear Algorithm** (in progress):
- Maintain a priority queue (heap) of orders sorted by value
- O(log n) insertion and O(1) minimum value retrieval
- When shelf is full, check if any shelf orders can move to ideal storage
- If no space available, discard the order with lowest value
- Target: Better than O(n) linear scan

## Progress

See [PROGRESS.md](PROGRESS.md) for detailed checklist.

**Completed (~30%)**:
- Storage system foundation
- Freshness tracking with decay
- Basic order placement and pickup
- Comprehensive test suite

**In Progress**:
- Sub-linear discard algorithm
- Shelf overflow handling (move/discard)
- Driver pickup simulation

**Not Started**:
- Full integration in Main.kt
- Server validation and consistent passing
