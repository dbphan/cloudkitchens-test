package com.css.challenge.manager

import com.css.challenge.client.*
import com.css.challenge.model.StoredOrder
import com.css.challenge.storage.Cooler
import com.css.challenge.storage.Heater
import com.css.challenge.storage.Shelf
import kotlinx.datetime.Clock

/**
 * Manages kitchen operations including order placement, storage, and removal.
 *
 * Coordinates between different storage containers (cooler, heater, shelf) and implements placement
 * logic according to temperature requirements.
 */
class KitchenManager {
    private val cooler = Cooler()
    private val heater = Heater()
    private val shelf = Shelf()

    private val actions = mutableListOf<Action>()

    /**
     * Places an order in the appropriate storage location.
     *
     * Placement logic:
     * 1. Try ideal temperature storage first
     * 2. If full, use shelf
     * 3. If shelf full, attempt to move existing shelf order to ideal storage
     * 4. If no move possible, discard an order from shelf (TODO: implement)
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
                actions.add(Action(timestamp, order.id, PLACE, SHELF))
                println(
                        "[PLACE] Order ${order.id} (${order.name}) placed in shelf (${idealLocation} full)"
                )
                return true
            }
        }

        // TODO: Implement shelf full logic
        // - Try to move an existing shelf order to cooler/heater
        // - If no move possible, discard an order

        println("[FAILED] Could not place order ${order.id} - all storage full")
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

    /** Clears all storage and actions (for testing). */
    suspend fun clear() {
        cooler.clear()
        heater.clear()
        shelf.clear()
        actions.clear()
    }
}
