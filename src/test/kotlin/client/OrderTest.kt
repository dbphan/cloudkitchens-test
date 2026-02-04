package com.css.challenge.client

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * Test suite for the Order data class.
 *
 * Tests order creation, serialization, and property validation.
 */
class OrderTest {

    /** Tests creating an order with all properties. */
    @Test
    fun `should create order with all properties`() {
        // Arrange & Act
        val order =
                Order(id = "order-123", name = "Burger", temp = "hot", price = 15, freshness = 300)

        // Assert
        assertEquals("order-123", order.id)
        assertEquals("Burger", order.name)
        assertEquals("hot", order.temp)
        assertEquals(15, order.price)
        assertEquals(300, order.freshness)
    }

    /** Tests creating an order with default price. */
    @Test
    fun `should use default price when not specified`() {
        // Arrange & Act
        val order = Order(id = "order-456", name = "Salad", temp = "cold", freshness = 600)

        // Assert
        assertEquals(0, order.price)
    }

    /** Tests serialization of order to JSON. */
    @Test
    fun `should serialize order to JSON`() {
        // Arrange
        val order =
                Order(
                        id = "order-789",
                        name = "Ice Cream",
                        temp = "frozen",
                        price = 8,
                        freshness = 120
                )

        // Act
        val json = Json.encodeToString(order)

        // Assert
        assertNotNull(json)
        assert(json.contains("\"id\":\"order-789\""))
        assert(json.contains("\"name\":\"Ice Cream\""))
        assert(json.contains("\"temp\":\"frozen\""))
        assert(json.contains("\"price\":8"))
        assert(json.contains("\"freshness\":120"))
    }

    /** Tests deserialization of order from JSON. */
    @Test
    fun `should deserialize order from JSON`() {
        // Arrange
        val json =
                """
            {
                "id": "order-999",
                "name": "Pizza",
                "temp": "hot",
                "price": 20,
                "freshness": 450
            }
        """.trimIndent()

        // Act
        val order = Json.decodeFromString<Order>(json)

        // Assert
        assertEquals("order-999", order.id)
        assertEquals("Pizza", order.name)
        assertEquals("hot", order.temp)
        assertEquals(20, order.price)
        assertEquals(450, order.freshness)
    }

    /** Tests deserialization with missing optional price field. */
    @Test
    fun `should deserialize order with missing price`() {
        // Arrange
        val json =
                """
            {
                "id": "order-111",
                "name": "Soup",
                "temp": "hot",
                "freshness": 240
            }
        """.trimIndent()

        // Act
        val order = Json.decodeFromString<Order>(json)

        // Assert
        assertEquals("order-111", order.id)
        assertEquals(0, order.price)
    }

    /** Tests order with cold temperature. */
    @Test
    fun `should create order with cold temperature`() {
        // Arrange & Act
        val order =
                Order(
                        id = "order-cold",
                        name = "Smoothie",
                        temp = "cold",
                        price = 7,
                        freshness = 180
                )

        // Assert
        assertEquals("cold", order.temp)
    }

    /** Tests order with frozen temperature. */
    @Test
    fun `should create order with frozen temperature`() {
        // Arrange & Act
        val order =
                Order(
                        id = "order-frozen",
                        name = "Frozen Yogurt",
                        temp = "frozen",
                        price = 6,
                        freshness = 90
                )

        // Assert
        assertEquals("frozen", order.temp)
    }
}
