package com.css.challenge.storage

import com.css.challenge.client.Order
import com.css.challenge.model.StoredOrder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** Test suite for storage containers (Cooler, Heater, Shelf). */
class StorageContainerTest {

    @Test
    fun `cooler should have capacity of 6 and cold temperature`() = runTest {
        // Arrange
        val cooler = Cooler()

        // Assert
        assertEquals(6, cooler.capacity)
        assertEquals("cold", cooler.temperature)
        assertEquals(0, cooler.size)
        assertTrue(cooler.isEmpty())
    }

    @Test
    fun `heater should have capacity of 6 and hot temperature`() = runTest {
        // Arrange
        val heater = Heater()

        // Assert
        assertEquals(6, heater.capacity)
        assertEquals("hot", heater.temperature)
        assertEquals(0, heater.size)
        assertTrue(heater.isEmpty())
    }

    @Test
    fun `shelf should have capacity of 12 and room temperature`() = runTest {
        // Arrange
        val shelf = Shelf()

        // Assert
        assertEquals(12, shelf.capacity)
        assertEquals("room", shelf.temperature)
        assertEquals(0, shelf.size)
        assertTrue(shelf.isEmpty())
    }

    @Test
    fun `should add orders to cooler`() = runTest {
        // Arrange
        val cooler = Cooler()
        val order = Order("test-1", "Ice Cream", "cold", 8, 60)
        val storedOrder = StoredOrder(order, currentLocation = "cooler")

        // Act
        val added = cooler.add(storedOrder)

        // Assert
        assertTrue(added)
        assertEquals(1, cooler.size)
        assertFalse(cooler.isEmpty())
    }

    @Test
    fun `should reject orders when cooler is full`() = runTest {
        // Arrange
        val cooler = Cooler()

        // Add 6 orders to fill it
        repeat(6) { i ->
            val order = Order("test-$i", "Item $i", "cold", 10, 60)
            val storedOrder = StoredOrder(order, currentLocation = "cooler")
            cooler.add(storedOrder)
        }

        // Act - try to add one more
        val order = Order("test-7", "Item 7", "cold", 10, 60)
        val storedOrder = StoredOrder(order, currentLocation = "cooler")
        val added = cooler.add(storedOrder)

        // Assert
        assertFalse(added)
        assertEquals(6, cooler.size)
        assertTrue(cooler.isFull())
    }

    @Test
    fun `should retrieve order by ID`() = runTest {
        // Arrange
        val cooler = Cooler()
        val order = Order("test-1", "Ice Cream", "cold", 8, 60)
        val storedOrder = StoredOrder(order, currentLocation = "cooler")
        cooler.add(storedOrder)

        // Act
        val retrieved = cooler.get("test-1")

        // Assert
        assertEquals("test-1", retrieved?.order?.id)
        assertEquals("Ice Cream", retrieved?.order?.name)
    }

    @Test
    fun `should return null for non-existent order`() = runTest {
        // Arrange
        val cooler = Cooler()

        // Act
        val retrieved = cooler.get("non-existent")

        // Assert
        assertNull(retrieved)
    }

    @Test
    fun `should remove order by ID`() = runTest {
        // Arrange
        val cooler = Cooler()
        val order = Order("test-1", "Ice Cream", "cold", 8, 60)
        val storedOrder = StoredOrder(order, currentLocation = "cooler")
        cooler.add(storedOrder)

        // Act
        val removed = cooler.remove("test-1")

        // Assert
        assertEquals("test-1", removed?.order?.id)
        assertEquals(0, cooler.size)
        assertTrue(cooler.isEmpty())
    }

    @Test
    fun `should return null when removing non-existent order`() = runTest {
        // Arrange
        val cooler = Cooler()

        // Act
        val removed = cooler.remove("non-existent")

        // Assert
        assertNull(removed)
    }

    @Test
    fun `should get all orders`() = runTest {
        // Arrange
        val cooler = Cooler()
        val order1 = Order("test-1", "Ice Cream", "cold", 8, 60)
        val order2 = Order("test-2", "Yogurt", "cold", 5, 90)
        cooler.add(StoredOrder(order1, currentLocation = "cooler"))
        cooler.add(StoredOrder(order2, currentLocation = "cooler"))

        // Act
        val all = cooler.getAll()

        // Assert
        assertEquals(2, all.size)
        assertTrue(all.any { it.order.id == "test-1" })
        assertTrue(all.any { it.order.id == "test-2" })
    }

    @Test
    fun `should clear all orders`() = runTest {
        // Arrange
        val cooler = Cooler()
        repeat(3) { i ->
            val order = Order("test-$i", "Item $i", "cold", 10, 60)
            cooler.add(StoredOrder(order, currentLocation = "cooler"))
        }

        // Act
        cooler.clear()

        // Assert
        assertEquals(0, cooler.size)
        assertTrue(cooler.isEmpty())
    }

    @Test
    fun `shelf should handle up to 12 orders`() = runTest {
        // Arrange
        val shelf = Shelf()

        // Act - add 12 orders
        repeat(12) { i ->
            val order = Order("test-$i", "Item $i", "room", 10, 60)
            val storedOrder = StoredOrder(order, currentLocation = "shelf")
            assertTrue(shelf.add(storedOrder), "Failed to add order $i")
        }

        // Assert
        assertEquals(12, shelf.size)
        assertTrue(shelf.isFull())

        // Try to add one more
        val extraOrder = Order("test-13", "Item 13", "room", 10, 60)
        assertFalse(shelf.add(StoredOrder(extraOrder, currentLocation = "shelf")))
    }
}
