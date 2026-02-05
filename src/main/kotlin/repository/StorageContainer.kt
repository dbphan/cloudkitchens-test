package com.css.challenge.repository

import com.css.challenge.model.StoredOrder

/**
 * Interface for storage containers in the kitchen.
 *
 * Defines common operations for managing orders in different storage locations (cooler, heater,
 * shelf). All implementations must be thread-safe to support concurrent order placement and
 * removal.
 */
interface StorageContainer {
    /** Maximum capacity of this storage container. */
    val capacity: Int

    /** Current number of orders in this storage. */
    val size: Int

    /** Temperature type this storage maintains. */
    val temperature: String

    /**
     * Checks if the storage is at full capacity.
     *
     * @return true if no more orders can be added
     */
    fun isFull(): Boolean = size >= capacity

    /**
     * Checks if the storage is empty.
     *
     * @return true if no orders are stored
     */
    fun isEmpty(): Boolean = size == 0

    /**
     * Adds an order to this storage.
     *
     * @param order The stored order to add
     * @return true if successfully added, false if storage is full
     */
    suspend fun add(order: StoredOrder): Boolean

    /**
     * Removes an order by its ID.
     *
     * @param orderId The ID of the order to remove
     * @return The removed order, or null if not found
     */
    suspend fun remove(orderId: String): StoredOrder?

    /**
     * Retrieves an order by its ID without removing it.
     *
     * @param orderId The ID of the order to find
     * @return The order if found, null otherwise
     */
    suspend fun get(orderId: String): StoredOrder?

    /**
     * Gets all orders currently in storage.
     *
     * @return List of all stored orders
     */
    suspend fun getAll(): List<StoredOrder>

    /** Removes all orders from storage. */
    suspend fun clear()
}
