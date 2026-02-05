package com.css.challenge.strategy

import com.css.challenge.model.StoredOrder
import java.util.PriorityQueue
import kotlinx.datetime.Clock

/** Uses a priority queue to efficiently track and discard lowest-value orders. */
class DiscardStrategy {
    private val orderQueue = PriorityQueue<StoredOrder>(compareBy { calculateOrderValue(it) })

    /** Calculates order value based on freshness, age, and temperature. Lower = discard first. */
    private fun calculateOrderValue(order: StoredOrder): Double {
        val currentFreshness = order.calculateFreshness()
        if (currentFreshness <= 0.0) {
            return 0.0 // Expired orders have no value
        }

        val freshnessLimit = order.order.freshness.toDouble()
        val orderAge = (Clock.System.now() - order.placedAt).inWholeSeconds.toDouble()
        val tempMultiplier = if (order.isAtIdealTemperature()) 1.0 else 2.0

        // Higher freshness = higher value, older age = lower value
        // +1 on age to avoid division by zero
        return (currentFreshness * freshnessLimit) / ((orderAge + 1) * tempMultiplier)
    }

    fun addOrder(order: StoredOrder) {
        orderQueue.offer(order)
    }

    fun removeOrder(order: StoredOrder) {
        orderQueue.remove(order)
    }

    fun removeOrderById(orderId: String) {
        orderQueue.removeIf { it.order.id == orderId }
    }

    fun peekLowestValueOrder(): StoredOrder? = orderQueue.peek()

    fun pollLowestValueOrder(): StoredOrder? = orderQueue.poll()

    fun clear() {
        orderQueue.clear()
    }

    val size: Int
        get() = orderQueue.size

    fun isEmpty(): Boolean = orderQueue.isEmpty()

    /** Rebuilds the queue when order values have changed over time. */
    fun refresh() {
        val orders = orderQueue.toList()
        orderQueue.clear()
        orders.forEach { orderQueue.offer(it) }
    }
}
