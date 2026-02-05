# Cloud Kitchens Take-Home Challenge

Author: Bao Phan

## How to run

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

### Algorithm: Priority Queue (Min-Heap) with Value-Based Selection

When the overflow shelf is full and no existing shelf orders can be moved to their ideal storage, the system discards the order with the **lowest calculated value**.

### Value Calculation Formula

```kotlin
value = (freshness × shelfLife) / (orderAge × temperatureMultiplier)
```

**Where:**
- `freshness`: Current freshness ratio (0.0 to 1.0)
- `shelfLife`: Maximum shelf life in seconds
- `orderAge`: Time elapsed since placement (seconds + 1 to avoid division by zero)
- `temperatureMultiplier`: 1.0 at ideal temperature, 2.0 at non-ideal temperature

### Rationale

**Why this approach:**

1. **Sub-linear Time Complexity**: Uses a PriorityQueue (min-heap) for O(log n) insertion and O(1) minimum value lookup, meeting the "better than O(n)" requirement.

2. **Balances Multiple Factors**: The formula considers:
   - **Freshness**: Lower freshness = lower value (more likely to discard)
   - **Shelf Life**: Orders with longer shelf life have higher value (keep longer-lasting items)
   - **Age**: Older orders have lower value (prefer discarding old items)
   - **Temperature Mismatch**: Orders at non-ideal temperature decay faster, lowering their value

3. **Fair and Predictable**: Orders that are:
   - Nearly expired (low freshness)
   - Stored at wrong temperature (high multiplier)
   - Already old (high age)
   - Have short shelf life
   
   ...will naturally have the lowest value and be discarded first.

4. **Automatic Prioritization**: The PriorityQueue automatically maintains the order, so we always know which order has the lowest value without scanning all orders.

### Implementation

```kotlin
class DiscardStrategy {
    private val orderQueue = PriorityQueue<StoredOrder>(
        compareBy { calculateValue(it) }
    )
    
    private fun calculateValue(order: StoredOrder): Double {
        val freshness = order.freshness()
        val age = order.age().inWholeSeconds + 1
        val tempMultiplier = order.temperatureMultiplier()
        val shelfLife = order.order.shelfLife.toDouble()
        
        return (freshness * shelfLife) / (age * tempMultiplier)
    }
    
    fun pollLowestValueOrder(): StoredOrder? = orderQueue.poll()
}
```

### Performance Characteristics

- **Add Order**: O(log n) - heap insertion
- **Get Lowest Value**: O(log n) - poll from heap
- **Remove by ID**: O(n) - requires linear search (acceptable for rare operation)

### Example Scenario

If the shelf contains:
1. Fresh pizza (90% fresh, 5 min old, wrong temp) → value ≈ 9.0
2. Aging salad (40% fresh, 10 min old, correct temp) → value ≈ 2.4
3. Old burger (20% fresh, 15 min old, wrong temp) → value ≈ 0.67

**Result**: The old burger (#3) would be discarded first due to its lowest value.

## Architecture

### Storage System
- **Cooler**: 6 cold orders at ideal temperature
- **Heater**: 6 hot orders at ideal temperature  
- **Shelf**: 12 orders at room temperature (overflow storage)

### Concurrency
- **Thread-safe**: All storage operations protected by Mutex
- **Coroutines**: Driver pickups run concurrently with random delays
- **Structured Concurrency**: All operations properly scoped and cleaned up

### Placement Strategy
1. Try ideal temperature storage first (cooler/heater)
2. Fall back to shelf if ideal is full
3. If shelf is also full:
   - Try moving a shelf order to its ideal storage
   - If no moves possible, discard lowest-value order using PriorityQueue
   - Place new order in freed space

### Technology Stack
- **Kotlin** 2.0.21 with coroutines
- **Java** 21
- **Gradle** 8.4
- **Ktor** 3.0.1 (HTTP client)
- **Clikt** 5.0.1 (CLI framework)
