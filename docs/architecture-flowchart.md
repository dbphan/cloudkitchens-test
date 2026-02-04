# Cloud Kitchens Application Architecture

## System Flow Diagram

```mermaid
flowchart TD
    Start([Application Start]) --> CLI[Main.kt - CLI Entry Point]
    CLI --> ParseArgs[Parse CLI Arguments<br/>--auth, --rate, --min, --max]
    ParseArgs --> FetchOrders[Client.newProblem<br/>Fetch Orders from Server]
    
    FetchOrders --> InitKitchen[Initialize KitchenManager<br/>Cooler, Heater, Shelf]
    
    InitKitchen --> ProcessLoop{For Each Order}
    
    ProcessLoop --> PlaceOrder[KitchenManager.placeOrder]
    
    PlaceOrder --> CheckTemp{Check Order<br/>Temperature}
    
    CheckTemp -->|Hot| TryHeater{Heater<br/>Has Space?}
    CheckTemp -->|Cold| TryCooler{Cooler<br/>Has Space?}
    CheckTemp -->|Room| TryShelf{Shelf<br/>Has Space?}
    
    TryHeater -->|Yes| AddHeater[Add to Heater<br/>Mutex.withLock]
    TryHeater -->|No| TryShelfHot{Shelf<br/>Has Space?}
    
    TryCooler -->|Yes| AddCooler[Add to Cooler<br/>Mutex.withLock]
    TryCooler -->|No| TryShelfCold{Shelf<br/>Has Space?}
    
    TryShelf -->|Yes| AddShelf[Add to Shelf<br/>Mutex.withLock]
    TryShelf -->|No| ShelfOverflow[Shelf Overflow Logic]
    
    TryShelfHot -->|Yes| AddShelf
    TryShelfHot -->|No| ShelfOverflow
    
    TryShelfCold -->|Yes| AddShelf
    TryShelfCold -->|No| ShelfOverflow
    
    ShelfOverflow --> TryMove{Can Move Shelf<br/>Order to Ideal<br/>Storage?}
    
    TryMove -->|Yes| MoveOrder[Move Order & Place New]
    TryMove -->|No| DiscardLowest[Discard Lowest Value Order<br/>Sub-linear Algorithm]
    
    AddHeater --> TrackPlace[Track Action: PLACE]
    AddCooler --> TrackPlace
    AddShelf --> TrackPlace
    MoveOrder --> TrackPlace
    DiscardLowest --> TrackDiscard[Track Action: DISCARD]
    
    TrackPlace --> SchedulePickup[Schedule Driver Pickup<br/>Random delay min-max]
    TrackDiscard --> SchedulePickup
    
    SchedulePickup --> WaitPickup[Coroutine Delay]
    
    WaitPickup --> PickupOrder[KitchenManager.pickupOrder]
    
    PickupOrder --> CheckFresh{Order Still<br/>Fresh?}
    
    CheckFresh -->|Yes| RemoveOrder[Remove from Storage<br/>O1 HashMap Lookup]
    CheckFresh -->|No| DiscardExpired[Discard Expired Order]
    
    RemoveOrder --> TrackPickup[Track Action: PICKUP]
    DiscardExpired --> TrackDiscardExpired[Track Action: DISCARD]
    
    TrackPickup --> MoreOrders{More<br/>Orders?}
    TrackDiscardExpired --> MoreOrders
    
    MoreOrders -->|Yes| ProcessLoop
    MoreOrders -->|No| WaitAll[Wait for All Pickups]
    
    WaitAll --> SubmitActions[Client.solve<br/>Submit Action Ledger]
    
    SubmitActions --> ValidationResult{Server<br/>Validation}
    
    ValidationResult -->|Pass| Success([Success - Exit])
    ValidationResult -->|Fail| Failure([Failure - Exit])

    style Start fill:#90EE90
    style Success fill:#90EE90
    style Failure fill:#FFB6C1
    style ShelfOverflow fill:#FFD700
    style DiscardLowest fill:#FF6B6B
    style CheckFresh fill:#87CEEB
    style ValidationResult fill:#DDA0DD
```

## Component Interaction Diagram

```mermaid
flowchart LR
    subgraph Client["API Client"]
        HTTP[Ktor HTTP Client]
        Client[Client.kt]
    end
    
    subgraph Models["Data Models"]
        Order[Order]
        StoredOrder[StoredOrder<br/>with Freshness]
        Action[Action]
        Problem[Problem]
    end
    
    subgraph Manager["Kitchen Manager"]
        KM[KitchenManager]
        ActionList[Actions List]
    end
    
    subgraph Storage["Thread-Safe Storage"]
        Cooler[Cooler<br/>6 Cold<br/>Mutex]
        Heater[Heater<br/>6 Hot<br/>Mutex]
        Shelf[Shelf<br/>12 Room<br/>Mutex]
    end
    
    Main[Main.kt<br/>CLI] --> Client
    Client --> Problem
    Problem --> Order
    
    Main --> KM
    KM --> StoredOrder
    StoredOrder --> Order
    
    KM --> Cooler
    KM --> Heater
    KM --> Shelf
    
    KM --> ActionList
    ActionList --> Action
    
    ActionList --> Client
    Client --> HTTP

    style Main fill:#4A90E2
    style KM fill:#F5A623
    style Cooler fill:#50E3C2
    style Heater fill:#FF6B6B
    style Shelf fill:#B8E986
```

## Freshness Calculation Flow

```mermaid
flowchart TD
    OrderPlaced[Order Placed] --> RecordTime[Record Placement Time<br/>Instant.now]
    
    RecordTime --> CheckLater{Pickup or<br/>Check Time}
    
    CheckLater --> CalcAge[Calculate Order Age<br/>currentTime - placedAt]
    
    CalcAge --> CheckLocation{Order at<br/>Ideal Temp?}
    
    CheckLocation -->|Yes Ideal| Decay1x[Decay Rate = 1x<br/>tempMultiplier = 1.0]
    CheckLocation -->|No Non-Ideal| Decay2x[Decay Rate = 2x<br/>tempMultiplier = 2.0]
    
    Decay1x --> CalcFresh[Freshness =<br/>shelfLife - decayRate × age × tempMultiplier<br/>÷ shelfLife]
    Decay2x --> CalcFresh
    
    CalcFresh --> CheckValue{Freshness > 0?}
    
    CheckValue -->|Yes| Fresh[Order is Fresh]
    CheckValue -->|No| Expired[Order Expired]
    
    Fresh --> AllowPickup[Allow Pickup]
    Expired --> DiscardOrder[Discard Order]

    style OrderPlaced fill:#90EE90
    style Fresh fill:#90EE90
    style Expired fill:#FFB6C1
    style Decay2x fill:#FFD700
```

## Concurrency Model

```mermaid
flowchart TD
    subgraph Main Thread
        MainCoroutine[Main Coroutine Scope]
    end
    
    subgraph Order Processing
        MainCoroutine --> OrderLoop[For Each Order<br/>Sequential Processing]
        OrderLoop --> Place1[Place Coroutine 1]
        OrderLoop --> Place2[Place Coroutine 2]
        OrderLoop --> PlaceN[Place Coroutine N]
    end
    
    subgraph Storage Access
        Place1 --> MutexC1[Mutex Lock<br/>Cooler]
        Place2 --> MutexH1[Mutex Lock<br/>Heater]
        PlaceN --> MutexS1[Mutex Lock<br/>Shelf]
    end
    
    subgraph Pickup Scheduling
        Place1 --> Pickup1[Pickup Coroutine 1<br/>Delay: Random min-max]
        Place2 --> Pickup2[Pickup Coroutine 2<br/>Delay: Random min-max]
        PlaceN --> PickupN[Pickup Coroutine N<br/>Delay: Random min-max]
    end
    
    subgraph Pickup Execution
        Pickup1 --> MutexC2[Mutex Lock<br/>Remove from Cooler]
        Pickup2 --> MutexH2[Mutex Lock<br/>Remove from Heater]
        PickupN --> MutexS2[Mutex Lock<br/>Remove from Shelf]
    end
    
    subgraph Synchronization
        MutexC2 --> Wait[WaitAll<br/>All Pickups Complete]
        MutexH2 --> Wait
        MutexS2 --> Wait
    end
    
    Wait --> Submit[Submit Actions]

    style MutexC1 fill:#FF6B6B
    style MutexH1 fill:#FF6B6B
    style MutexS1 fill:#FF6B6B
    style MutexC2 fill:#FF6B6B
    style MutexH2 fill:#FF6B6B
    style MutexS2 fill:#FF6B6B
    style Wait fill:#90EE90
```

## Key Design Decisions

### Thread Safety
- All storage containers use **Mutex.withLock** for atomic operations
- Suspend functions enable concurrent access without blocking threads
- HashMap provides O(1) order lookup by ID

### Freshness Tracking
- Real-time calculation using `kotlinx-datetime.Instant`
- Temperature multiplier: 1.0x at ideal temp, 2.0x at non-ideal
- Formula: `(shelfLife - decayRate × orderAge × tempMultiplier) / shelfLife`

### Placement Strategy
1. Try ideal temperature storage first
2. Fallback to shelf if ideal is full
3. If shelf full: attempt to move shelf order to now-available ideal storage
4. If no moves possible: discard order with lowest value using sub-linear algorithm

### Discard Algorithm (Planned)
- Priority queue (heap) sorted by order value
- O(log n) insertion, O(1) minimum retrieval
- Value = `(freshness × shelfLife × decayModifier) / (orderAge × tempMultiplier)`
