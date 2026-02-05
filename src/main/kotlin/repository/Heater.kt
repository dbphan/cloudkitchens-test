package com.css.challenge.repository

import com.css.challenge.model.StoredOrder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe storage container for hot orders.
 *
 * Maintains hot temperature and holds up to 6 orders.
 */
class Heater : StorageContainer {
    override val capacity: Int = 6
    override val temperature: String = "hot"

    private val mutex = Mutex()
    private val orders = mutableMapOf<String, StoredOrder>()

    override val size: Int
        get() = orders.size

    override suspend fun add(order: StoredOrder): Boolean =
            mutex.withLock {
                if (isFull()) {
                    return@withLock false
                }
                orders[order.order.id] = order
                true
            }

    override suspend fun remove(orderId: String): StoredOrder? =
            mutex.withLock { orders.remove(orderId) }

    override suspend fun get(orderId: String): StoredOrder? = mutex.withLock { orders[orderId] }

    override suspend fun getAll(): List<StoredOrder> = mutex.withLock { orders.values.toList() }

    override suspend fun clear() = mutex.withLock { orders.clear() }
}
