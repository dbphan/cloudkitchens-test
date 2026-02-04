package com.css.challenge.client

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

/**
 * Test suite for the Problem data class.
 *
 * Tests problem creation and property validation.
 */
class ProblemTest {

    /** Tests creating a problem with orders. */
    @Test
    fun `should create problem with test ID and orders`() {
        // Arrange
        val testId = "test-123"
        val orders =
                listOf(
                        Order("order-1", "Burger", "hot", 15, 300),
                        Order("order-2", "Salad", "cold", 10, 600)
                )

        // Act
        val problem = Problem(testId, orders)

        // Assert
        assertEquals(testId, problem.testId)
        assertEquals(2, problem.orders.size)
        assertEquals("order-1", problem.orders[0].id)
        assertEquals("order-2", problem.orders[1].id)
    }

    /** Tests creating a problem with empty order list. */
    @Test
    fun `should create problem with empty order list`() {
        // Arrange
        val testId = "test-456"
        val orders = emptyList<Order>()

        // Act
        val problem = Problem(testId, orders)

        // Assert
        assertEquals(testId, problem.testId)
        assertTrue(problem.orders.isEmpty())
    }

    /** Tests creating a problem with single order. */
    @Test
    fun `should create problem with single order`() {
        // Arrange
        val testId = "test-789"
        val order = Order("order-1", "Pizza", "hot", 20, 450)
        val orders = listOf(order)

        // Act
        val problem = Problem(testId, orders)

        // Assert
        assertEquals(1, problem.orders.size)
        assertEquals("Pizza", problem.orders[0].name)
    }

    /** Tests creating a problem with multiple orders of different temperatures. */
    @Test
    fun `should create problem with mixed temperature orders`() {
        // Arrange
        val testId = "test-mixed"
        val orders =
                listOf(
                        Order("order-hot", "Soup", "hot", 12, 300),
                        Order("order-cold", "Ice Cream", "frozen", 8, 120),
                        Order("order-cold2", "Salad", "cold", 10, 600)
                )

        // Act
        val problem = Problem(testId, orders)

        // Assert
        assertEquals(3, problem.orders.size)
        assertEquals("hot", problem.orders[0].temp)
        assertEquals("frozen", problem.orders[1].temp)
        assertEquals("cold", problem.orders[2].temp)
    }

    /** Tests problem properties are immutable (data class behavior). */
    @Test
    fun `should have immutable properties`() {
        // Arrange
        val testId = "test-999"
        val orders = listOf(Order("order-1", "Burger", "hot", 15, 300))
        val problem1 = Problem(testId, orders)

        // Act
        val problem2 = problem1.copy()

        // Assert
        assertEquals(problem1.testId, problem2.testId)
        assertEquals(problem1.orders, problem2.orders)
    }

    /** Tests problem with large number of orders. */
    @Test
    fun `should handle large number of orders`() {
        // Arrange
        val testId = "test-large"
        val orders = (1..100).map { i -> Order("order-$i", "Item $i", "hot", i * 10, i * 30) }

        // Act
        val problem = Problem(testId, orders)

        // Assert
        assertEquals(100, problem.orders.size)
        assertEquals("order-1", problem.orders.first().id)
        assertEquals("order-100", problem.orders.last().id)
    }
}
