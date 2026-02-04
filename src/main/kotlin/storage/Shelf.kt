package com.css.challenge.storage

import com.css.challenge.model.StoredOrder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe storage container for room temperature orders.
 *
 * Maintains room temperature and holds up to 12 orders. Acts as overflow storage when cooler or
 * heater are full.
 */
class Shelf : StorageContainer {
    override val capacity: Int = 12
    override val temperature: String = "room"

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
