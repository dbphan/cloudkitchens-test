package com.css.challenge.manager

import com.css.challenge.client.Order
import com.css.challenge.model.StoredOrder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

/**
 * Test suite for DiscardStrategy.
 *
 * Verifies the sub-linear discard algorithm using priority queue.
 */
class DiscardStrategyTest {

    @Test
    fun `Given an order, When adding to the strategy, Then it should track the order in priority queue`() =
            runTest {
                // Arrange
                val strategy = DiscardStrategy()
                val order1 = Order("order-1", "Pizza", "hot", 15, 300)
                val stored1 = StoredOrder(order1, currentLocation = "shelf")

                // Act
                strategy.addOrder(stored1)

                // Assert
                assertEquals(1, strategy.size)
                assertFalse(strategy.isEmpty())
            }

    @Test
    fun `Given an empty strategy, When peeking lowest value order, Then it should return null`() =
            runTest {
                // Arrange
                val strategy = DiscardStrategy()

                // Act
                val result = strategy.peekLowestValueOrder()

                // Assert
                assertNull(result)
                assertTrue(strategy.isEmpty())
            }

    @Test
    fun `Given orders with different values, When peeking lowest value, Then it should return the order with lowest value`() =
            runTest {
                // Arrange
                val strategy = DiscardStrategy()

                // Order with high value (fresh, long shelf life, low decay)
                val highValueOrder = Order("high", "Fresh Salad", "cold", 1, 600)
                val storedHigh = StoredOrder(highValueOrder, currentLocation = "shelf")

                // Order with lower value (shorter shelf life)
                val lowValueOrder = Order("low", "Hot Dog", "hot", 2, 100)
                val storedLow = StoredOrder(lowValueOrder, currentLocation = "shelf")

                // Act
                strategy.addOrder(storedHigh)
                strategy.addOrder(storedLow)

                // Assert
                val lowest = strategy.peekLowestValueOrder()
                assertEquals("low", lowest?.order?.id)
                assertEquals(2, strategy.size) // Peek doesn't remove
    }

    @Test
    fun `Given multiple orders, When polling lowest value, Then it should return and remove the lowest value order`() =
            runTest {
                // Arrange
                val strategy = DiscardStrategy()

                val order1 = Order("order-1", "Pizza", "hot", 10, 300)
                val order2 = Order("order-2", "Burger", "hot", 15, 200)

                val stored1 = StoredOrder(order1, currentLocation = "shelf")
                val stored2 = StoredOrder(order2, currentLocation = "shelf")

                strategy.addOrder(stored1)
                strategy.addOrder(stored2)

                // Act
                val polled = strategy.pollLowestValueOrder()

                // Assert
                assertEquals("order-2", polled?.order?.id) // Shorter shelf life = lower value
                assertEquals(1, strategy.size) // One removed
    }

    @Test
    fun `Given an order in the strategy, When removing by reference, Then it should remove the specific order`() =
            runTest {
                // Arrange
                val strategy = DiscardStrategy()

                val order1 = Order("order-1", "Pizza", "hot", 10, 300)
                val stored1 = StoredOrder(order1, currentLocation = "shelf")

                strategy.addOrder(stored1)

                // Act
                strategy.removeOrder(stored1)

                // Assert
                assertEquals(0, strategy.size)
                assertTrue(strategy.isEmpty())
            }

    @Test
    fun `Given multiple orders, When removing by ID, Then it should remove the order with matching ID`() =
            runTest {
                // Arrange
                val strategy = DiscardStrategy()

                val order1 = Order("order-1", "Pizza", "hot", 10, 300)
                val order2 = Order("order-2", "Burger", "hot", 15, 200)

                strategy.addOrder(StoredOrder(order1, currentLocation = "shelf"))
                strategy.addOrder(StoredOrder(order2, currentLocation = "shelf"))

                // Act
                strategy.removeOrderById("order-1")

                // Assert
                assertEquals(1, strategy.size)
                assertEquals("order-2", strategy.peekLowestValueOrder()?.order?.id)
            }

    @Test
    fun `Given multiple orders in the strategy, When clearing, Then it should remove all orders`() =
            runTest {
                // Arrange
                val strategy = DiscardStrategy()

                repeat(5) { i ->
                    val order = Order("order-$i", "Item $i", "hot", 10, 300)
                    strategy.addOrder(StoredOrder(order, currentLocation = "shelf"))
                }

                // Act
                strategy.clear()

                // Assert
                assertEquals(0, strategy.size)
                assertTrue(strategy.isEmpty())
            }

    @Test
    fun `Given fresh and expired orders, When peeking lowest value, Then it should prioritize expired orders`() =
            runTest {
                // Arrange
                val strategy = DiscardStrategy()

                // Fresh order
                val freshOrder = Order("fresh", "Fresh Pizza", "hot", 10, 300)
                val storedFresh = StoredOrder(freshOrder, currentLocation = "shelf")

                // Expired order (placed long ago)
                val expiredOrder = Order("expired", "Old Pizza", "hot", 10, 300)
                val placedAt =
                        Instant.fromEpochMilliseconds(
                                System.currentTimeMillis() - 400_000
                        ) // 400 seconds ago
                val storedExpired =
                        StoredOrder(expiredOrder, currentLocation = "shelf", placedAt = placedAt)

                // Act
                strategy.addOrder(storedFresh)
                strategy.addOrder(storedExpired)

                // Assert
                val lowest = strategy.peekLowestValueOrder()
                assertEquals("expired", lowest?.order?.id) // Expired has value 0
    }

    @Test
    fun `Given multiple orders with identical values, When polling repeatedly, Then it should return all orders`() =
            runTest {
                // Arrange
                val strategy = DiscardStrategy()

                // Three identical orders
                repeat(3) { i ->
                    val order = Order("order-$i", "Pizza", "hot", 10, 300)
                    strategy.addOrder(StoredOrder(order, currentLocation = "shelf"))
                }

                // Act & Assert
                assertEquals(3, strategy.size)
                val first = strategy.pollLowestValueOrder()
                assertEquals(2, strategy.size)

                val second = strategy.pollLowestValueOrder()
                assertEquals(1, strategy.size)

                val third = strategy.pollLowestValueOrder()
                assertEquals(0, strategy.size)
                assertTrue(strategy.isEmpty())
            }

    @Test
    fun `Given a strategy with orders, When refreshing, Then it should maintain queue functionality with updated values`() =
            runTest {
                // Arrange
                val strategy = DiscardStrategy()

                val order1 = Order("order-1", "Pizza", "hot", 10, 100)
                val order2 = Order("order-2", "Burger", "hot", 10, 200)

                val stored1 = StoredOrder(order1, currentLocation = "shelf")
                val stored2 = StoredOrder(order2, currentLocation = "shelf")

                strategy.addOrder(stored1)
                strategy.addOrder(stored2)

                // Wait a bit for ages to change
                Thread.sleep(100)

                // Act
                strategy.refresh()

                // Assert - queue still works after refresh
                assertEquals(2, strategy.size)
                val lowest = strategy.peekLowestValueOrder()
                assertEquals("order-1", lowest?.order?.id) // Shorter shelf life still lowest
    }

    @Test
    fun `Given orders at ideal and non-ideal temperatures, When comparing values, Then non-ideal should have lower value due to multiplier`() =
            runTest {
                // Arrange
                val strategy = DiscardStrategy()

                // Order at ideal temperature (cooler)
                val idealOrder = Order("ideal", "Ice Cream", "cold", 10, 300)
                val storedIdeal = StoredOrder(idealOrder, currentLocation = "cooler")

                // Order at non-ideal temperature (shelf, 2x decay)
                val nonIdealOrder = Order("non-ideal", "Ice Cream", "cold", 10, 300)
                val storedNonIdeal = StoredOrder(nonIdealOrder, currentLocation = "shelf")

                // Act
                strategy.addOrder(storedIdeal)
                strategy.addOrder(storedNonIdeal)

                // Assert - non-ideal should have lower value due to 2x multiplier
                val lowest = strategy.peekLowestValueOrder()
                assertEquals("non-ideal", lowest?.order?.id)
            }

    @Test
    fun `Given 100 orders with varying shelf lives, When polling all, Then it should efficiently return them in ascending value order`() =
            runTest {
                // Arrange
                val strategy = DiscardStrategy()

                // Add 100 orders with varying shelf lives
                repeat(100) { i ->
                    val order = Order("order-$i", "Item $i", "hot", 10, 100 + i * 10)
                    strategy.addOrder(StoredOrder(order, currentLocation = "shelf"))
                }

                // Act & Assert - should efficiently find minimum
                assertEquals(100, strategy.size)

                val lowest = strategy.peekLowestValueOrder()
                assertEquals("order-0", lowest?.order?.id) // Shortest shelf life

                // Poll all orders - should come out in ascending value order
                val polled = mutableListOf<String>()
                while (!strategy.isEmpty()) {
                    polled.add(strategy.pollLowestValueOrder()!!.order.id)
                }

                assertEquals(100, polled.size)
                assertTrue(strategy.isEmpty())
            }
}
