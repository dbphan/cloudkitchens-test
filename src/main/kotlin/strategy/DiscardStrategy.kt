package com.css.challenge.strategy

import com.css.challenge.model.StoredOrder
import java.util.PriorityQueue
import kotlinx.datetime.Clock

/**
 * Strategy for selecting which order to discard when shelf is full.
 *
 * Implements a sub-linear algorithm using a priority queue (min-heap) to efficiently find the order
 * with the lowest value. The value represents how valuable an order is based on its remaining
 * freshness, shelf life, and decay characteristics.
 *
 * Time Complexity:
 * - Finding minimum value order: O(1)
 * - Adding an order: O(log n)
 * - Removing an order: O(log n)
 *
 * This is significantly better than the naive O(n) linear scan approach.
 */
class DiscardStrategy {
    /**
     * Priority queue maintaining orders sorted by value (lowest first). Uses a min-heap so the
     * order with the lowest value is always at the front.
     */
    private val orderQueue = PriorityQueue<StoredOrder>(compareBy { calculateOrderValue(it) })

    /**
     * Calculates the value of an order.
     *
     * Orders with higher value are more important to keep. The value formula considers:
     * - Freshness: How much shelf life remains (0.0 to 1.0)
     * - Order freshness limit: Total expected lifetime in seconds
     * - Order age: How long the order has been stored
     * - Temperature multiplier: 1.0 at ideal temp, 2.0 at non-ideal temp
     *
     * @param order The stored order to evaluate
     * @return The calculated value (higher = more important to keep)
     */
    private fun calculateOrderValue(order: StoredOrder): Double {
        val currentFreshness = order.calculateFreshness()
        if (currentFreshness <= 0.0) {
            return 0.0 // Expired orders have no value
        }

        val freshnessLimit = order.order.freshness.toDouble()
        val orderAge = (Clock.System.now() - order.placedAt).inWholeSeconds.toDouble()
        val tempMultiplier = if (order.isAtIdealTemperature()) 1.0 else 2.0

        // Value formula: (currentFreshness * freshnessLimit) / ((orderAge + 1) * tempMultiplier)
        // Higher current freshness and longer freshness limit increase value
        // Higher age and non-ideal temperature decrease value
        // Add 1 to age to avoid division by zero for brand new orders
        return (currentFreshness * freshnessLimit) / ((orderAge + 1) * tempMultiplier)
    }

    /**
     * Adds an order to the tracking queue.
     *
     * @param order The order to track
     */
    fun addOrder(order: StoredOrder) {
        orderQueue.offer(order)
    }

    /**
     * Removes an order from the tracking queue.
     *
     * @param order The order to stop tracking
     */
    fun removeOrder(order: StoredOrder) {
        orderQueue.remove(order)
    }

    /**
     * Removes an order by its ID from the tracking queue.
     *
     * @param orderId The ID of the order to remove
     */
    fun removeOrderById(orderId: String) {
        orderQueue.removeIf { it.order.id == orderId }
    }

    /**
     * Gets the order with the lowest value without removing it.
     *
     * Time Complexity: O(1)
     *
     * @return The lowest value order, or null if queue is empty
     */
    fun peekLowestValueOrder(): StoredOrder? {
        return orderQueue.peek()
    }

    /**
     * Gets and removes the order with the lowest value.
     *
     * Time Complexity: O(log n)
     *
     * @return The lowest value order, or null if queue is empty
     */
    fun pollLowestValueOrder(): StoredOrder? {
        return orderQueue.poll()
    }

    /** Clears all orders from the tracking queue. */
    fun clear() {
        orderQueue.clear()
    }

    /**
     * Gets the number of orders currently being tracked.
     *
     * @return The size of the queue
     */
    val size: Int
        get() = orderQueue.size

    /**
     * Checks if the queue is empty.
     *
     * @return true if no orders are being tracked
     */
    fun isEmpty(): Boolean = orderQueue.isEmpty()

    /**
     * Rebuilds the priority queue.
     *
     * Call this when order values may have changed significantly (e.g., after time passes). This
     * ensures the heap property is maintained with updated values.
     *
     * Time Complexity: O(n)
     */
    fun refresh() {
        val orders = orderQueue.toList()
        orderQueue.clear()
        orders.forEach { orderQueue.offer(it) }
    }
}
