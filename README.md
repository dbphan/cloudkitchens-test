# Cloud Kitchens Take-Home Challenge

Author: Bao Phan

## How to run

### Environment Setup

The application requires an authentication token to access the CloudKitchens API. You can provide this in two ways:

**Option 1: Environment File (Recommended for local development)**

Create a `.env` file in the project root:

```bash
# .env file
AUTH_TOKEN=your_token_here
ENDPOINT=https://api.cloudkitchens.com  # Optional, this is the default
```

Then run the application:

```bash
# Load environment variables and run
set -a && source .env && set +a && ./gradlew run
```

**Option 2: Command Line Arguments**

Pass the token directly via command line:

```bash
./gradlew run --args="--auth=<token>"
```

### Using Docker (Recommended)

The `Dockerfile` defines a self-contained Java/Gradle reference environment.
Build and run the program using [Docker](https://docs.docker.com/get-started/get-docker/):

```bash
# Build the Docker image
docker build -t challenge .

# Run with authentication token
docker run --rm -it challenge --auth=<token>

# Run with custom parameters
docker run --rm -it challenge \
  --auth=<token> \
  --rate=500ms \
  --min=4s \
  --max=8s
```

### Using Gradle (Local Development)

If Java 21 or later is installed locally, run the program directly:

```bash
# Run with Gradle wrapper
./gradlew run --args="--auth=<token>"

# Or build a standalone distribution
./gradlew installDist
./build/install/challenge/bin/challenge --auth=<token>
```

### CLI Options

```
--auth=<token>        Authentication token (required)
--endpoint=<url>      API endpoint (optional, default: https://api.cloudkitchens.com)
--name=<problem>      Problem name (optional)
--seed=<number>       Seed for deterministic problems (optional)
--rate=<duration>     Order rate (optional, default: 500ms)
--min=<duration>      Minimum pickup time (optional, default: 4s)
--max=<duration>      Maximum pickup time (optional, default: 8s)
```

## Discard Criteria

When the shelf is full and we can't move anything to ideal storage, we need to discard something. The system picks the order with the **lowest value**.

### How it works

The value formula looks at a few things:

```kotlin
value = (freshness × shelfLife) / (orderAge × temperatureMultiplier)
```

- Fresh orders with long shelf life → higher value (keep these)
- Old orders or wrong temperature → lower value (discard these)
- Temperature multiplier is 2x if stored in the wrong place

I'm using a priority queue (min-heap) to track this, which means finding the lowest value is fast - O(log n) instead of scanning everything.

### Example

If the shelf has:
- Fresh pizza (90% fresh, 5 min old, wrong temp) → value ~9
- Aging salad (40% fresh, 10 min old, right temp) → value ~2.4
- Old burger (20% fresh, 15 min old, wrong temp) → value ~0.67

The old burger gets tossed first.

## Architecture

The storage system has three containers:
- **Cooler**: 6 cold orders
- **Heater**: 6 hot orders
- **Shelf**: 12 orders (overflow storage)

All storage operations are thread-safe with Mutex locks. Driver pickups run concurrently using coroutines with random delays.

**Placement logic:**
1. Try ideal temperature storage first
2. Fall back to shelf if full
3. If shelf is also full, try moving a shelf order to its ideal spot
4. If that doesn't work, discard the lowest-value order
5. Place new order in the freed space

### Project Structure

```
src/main/kotlin/
├── client/          API client
├── service/         Business logic
├── strategy/        Discard algorithm
├── repository/      Storage containers
└── model/           Domain models
```

**Tech stack:** Kotlin 2.0.21, Java 21, Gradle 8.4, Ktor 3.0.1, Clikt 5.0.1

## Testing

**95 tests total** - 72 unit tests + 23 integration tests

Run them with:
```bash
./gradlew test                    # unit tests
./gradlew integrationTest         # integration tests
./gradlew test integrationTest    # all tests
```

Unit tests cover individual components. Integration tests verify full workflows including concurrency, API interactions, and edge cases like capacity limits.
