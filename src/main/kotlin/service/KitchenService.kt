package com.css.challenge.service

import com.css.challenge.client.*
import com.css.challenge.model.StoredOrder
import com.css.challenge.repository.Cooler
import com.css.challenge.repository.Heater
import com.css.challenge.repository.Shelf
import com.css.challenge.strategy.DiscardStrategy
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Service for managing kitchen operations including order placement, storage, and removal.
 *
 * Coordinates between different storage containers (cooler, heater, shelf) and implements placement
 * logic according to temperature requirements. Uses a sub-linear discard algorithm when shelf is
 * full.
 */
class KitchenService {
    private val cooler = Cooler()
    private val heater = Heater()
    private val shelf = Shelf()

    private val discardStrategy = DiscardStrategy()

    private val actions = mutableListOf<Action>()

    /**
     * Places an order in the appropriate storage location.
     *
     * Placement logic:
     * 1. Try ideal temperature storage first
     * 2. If full, use shelf
     * 3. If shelf full, attempt to move existing shelf order to ideal storage
     * 4. If no move possible, discard an order from shelf
     *
     * @param order The order to place
     * @return true if successfully placed, false otherwise
     */
    suspend fun placeOrder(order: com.css.challenge.client.Order): Boolean {
        val timestamp = Clock.System.now()

        // Determine ideal storage based on temperature
        val (idealStorage, idealLocation) =
                when (order.temp) {
                    "hot" -> Pair(heater, HEATER)
                    "cold" -> Pair(cooler, COOLER)
                    else -> Pair(shelf, SHELF)
                }

        val storedOrder = StoredOrder(order, timestamp, idealLocation)

        // Try ideal storage first
        if (idealStorage.add(storedOrder)) {
            actions.add(Action(timestamp, order.id, PLACE, idealLocation))
            println("[PLACE] Order ${order.id} (${order.name}) placed in $idealLocation")
            return true
        }

        // If ideal storage full, try shelf
        if (order.temp != "room" && !shelf.isFull()) {
            storedOrder.currentLocation = SHELF
            if (shelf.add(storedOrder)) {
                discardStrategy.addOrder(storedOrder) // Track in discard strategy
                actions.add(Action(timestamp, order.id, PLACE, SHELF))
                println(
                        "[PLACE] Order ${order.id} (${order.name}) placed in shelf (${idealLocation} full)"
                )
                return true
            }
        }

        // Shelf is full - try to move an existing shelf order to ideal storage
        if (shelf.isFull() && order.temp != "room") {
            if (handleShelfOverflow(order, storedOrder, idealStorage, idealLocation, timestamp)) {
                return true
            }
        }

        println("[FAILED] Could not place order ${order.id} - all storage full")
        return false
    }

    /**
     * Handles shelf overflow when both ideal storage and shelf are full.
     *
     * Strategy:
     * 1. Try to move a shelf order to its ideal storage if space becomes available
     * 2. If no moves possible, discard the order with lowest value from shelf
     * 3. Place the new order in the freed space
     *
     * @param newOrder The original order to place
     * @param newStoredOrder The stored order wrapper
     * @param idealStorage The ideal storage for the new order
     * @param idealLocation The location name of ideal storage
     * @param timestamp Current timestamp
     * @return true if overflow was handled and order was placed
     */
    private suspend fun handleShelfOverflow(
            newOrder: Order,
            newStoredOrder: StoredOrder,
            idealStorage: com.css.challenge.repository.StorageContainer,
            idealLocation: String,
            timestamp: kotlinx.datetime.Instant
    ): Boolean {
        // Try to move a shelf order to cooler or heater if space available
        val shelfOrders = shelf.getAll()
        for (shelfOrder in shelfOrders) {
            val moveTarget =
                    when (shelfOrder.order.temp) {
                        "hot" -> if (!heater.isFull()) Pair(heater, HEATER) else null
                        "cold" -> if (!cooler.isFull()) Pair(cooler, COOLER) else null
                        else -> null
                    }

            if (moveTarget != null) {
                val (targetStorage, targetLocation) = moveTarget
                shelf.remove(shelfOrder.order.id)
                discardStrategy.removeOrderById(shelfOrder.order.id)

                shelfOrder.currentLocation = targetLocation
                targetStorage.add(shelfOrder)

                actions.add(Action(timestamp, shelfOrder.order.id, MOVE, targetLocation))
                println("[MOVE] Order ${shelfOrder.order.id} moved from shelf to $targetLocation")

                // Now place new order on shelf
                newStoredOrder.currentLocation = SHELF
                shelf.add(newStoredOrder)
                discardStrategy.addOrder(newStoredOrder)
                actions.add(Action(timestamp, newOrder.id, PLACE, SHELF))
                println(
                        "[PLACE] Order ${newOrder.id} (${newOrder.name}) placed in shelf after move"
                )
                return true
            }
        }

        // No moves possible - discard lowest value order from shelf
        val lowestValueOrder = discardStrategy.pollLowestValueOrder()
        if (lowestValueOrder != null) {
            shelf.remove(lowestValueOrder.order.id)
            actions.add(Action(timestamp, lowestValueOrder.order.id, DISCARD, SHELF))
            println(
                    "[DISCARD] Order ${lowestValueOrder.order.id} (${lowestValueOrder.order.name}) discarded (lowest value)"
            )

            // Place new order in freed space
            newStoredOrder.currentLocation = SHELF
            shelf.add(newStoredOrder)
            discardStrategy.addOrder(newStoredOrder)
            actions.add(Action(timestamp, newOrder.id, PLACE, SHELF))
            println("[PLACE] Order ${newOrder.id} (${newOrder.name}) placed in shelf after discard")
            return true
        }

        return false
    }

    /**
     * Picks up an order by its ID.
     *
     * Searches all storage locations and removes the order if found. If the order has expired, it
     * is discarded instead of picked up.
     *
     * @param orderId The ID of the order to pick up
     * @return true if order was picked up, false if not found or expired
     */
    suspend fun pickupOrder(orderId: String): Boolean {
        val timestamp = Clock.System.now()

        // Search all storage locations
        val storedOrder = cooler.remove(orderId) ?: heater.remove(orderId) ?: shelf.remove(orderId)

        if (storedOrder == null) {
            println("[PICKUP] Order $orderId not found")
            return false
        }

        // Remove from discard strategy if it was on shelf
        if (storedOrder.currentLocation == SHELF) {
            discardStrategy.removeOrderById(orderId)
        }

        // Check if order is still fresh
        if (!storedOrder.isFresh(timestamp)) {
            actions.add(Action(timestamp, orderId, DISCARD, storedOrder.currentLocation))
            println("[DISCARD] Order $orderId (${storedOrder.order.name}) expired at pickup")
            return false
        }

        actions.add(Action(timestamp, orderId, PICKUP, storedOrder.currentLocation))
        println(
                "[PICKUP] Order $orderId (${storedOrder.order.name}) picked up from ${storedOrder.currentLocation}"
        )
        return true
    }

    /**
     * Gets all captured actions.
     *
     * @return List of all actions performed
     */
    fun getActions(): List<Action> = actions.toList()

    /**
     * Schedules a driver pickup for an order after a random delay.
     *
     * The pickup is scheduled to occur within the specified min-max interval using a random delay.
     * Uses coroutines for concurrent pickup simulation.
     *
     * @param orderId The ID of the order to pick up
     * @param minPickupTime Minimum pickup time in seconds
     * @param maxPickupTime Maximum pickup time in seconds
     * @param scope The coroutine scope to launch the pickup task
     */
    fun scheduleDriverPickup(
            orderId: String,
            minPickupTime: Int,
            maxPickupTime: Int,
            scope: CoroutineScope
    ) {
        scope.launch {
            // Calculate random delay in milliseconds
            val delaySeconds = Random.nextInt(minPickupTime, maxPickupTime + 1)
            val delayMillis = delaySeconds * 1000L

            println("[DRIVER] Driver scheduled to pick up order $orderId in ${delaySeconds}s")

            // Wait for the random delay
            delay(delayMillis)

            // Attempt to pick up the order
            pickupOrder(orderId)
        }
    }

    /** Clears all storage and actions (for testing). */
    suspend fun clear() {
        cooler.clear()
        heater.clear()
        shelf.clear()
        actions.clear()
    }
}
