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

/** Manages kitchen order placement, storage, and pickup operations. */
class KitchenService {
    private val cooler = Cooler()
    private val heater = Heater()
    private val shelf = Shelf()

    private val discardStrategy = DiscardStrategy()

    private val actions = mutableListOf<Action>()

    /** Places an order using ideal storage first, falling back to shelf if needed. */
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

    /** Handles shelf overflow by moving or discarding existing orders. */
    private suspend fun handleShelfOverflow(
            newOrder: Order,
            newStoredOrder: StoredOrder,
            idealStorage: com.css.challenge.repository.StorageContainer,
            idealLocation: String,
            timestamp: kotlinx.datetime.Instant
    ): Boolean {
        // See if we can shuffle things around to make room
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

    /** Picks up an order if it exists and hasn't expired. */
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

    fun getActions(): List<Action> = actions.toList()

    /** Schedules a driver to pick up an order after a random delay between min and max. */
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

            // Driver's on their way...
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
