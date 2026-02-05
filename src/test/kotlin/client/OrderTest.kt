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

    /**
     * Given all order properties, When creating an order, Then it should have all properties
     * correctly assigned.
     */
    @Test
    fun `Given all order properties, When creating an order, Then it should have all properties correctly assigned`() {
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

    /**
     * Given an order without price specified, When creating the order, Then it should use the
     * default price of 0.
     */
    @Test
    fun `Given an order without price specified, When creating the order, Then it should use the default price of 0`() {
        // Arrange & Act
        val order = Order(id = "order-456", name = "Salad", temp = "cold", freshness = 600)

        // Assert
        assertEquals(0, order.price)
    }

    /**
     * Given an order object, When serializing to JSON, Then it should contain all order properties
     * in JSON format.
     */
    @Test
    fun `Given an order object, When serializing to JSON, Then it should contain all order properties in JSON format`() {
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

    /**
     * Given a valid JSON string, When deserializing to an order, Then it should create an order
     * with correct properties.
     */
    @Test
    fun `Given a valid JSON string, When deserializing to an order, Then it should create an order with correct properties`() {
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

    /**
     * Given JSON without price field, When deserializing to an order, Then it should use the
     * default price value.
     */
    @Test
    fun `Given JSON without price field, When deserializing to an order, Then it should use the default price value`() {
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

    /**
     * Given cold temperature specification, When creating an order, Then it should have temperature
     * set to cold.
     */
    @Test
    fun `Given cold temperature specification, When creating an order, Then it should have temperature set to cold`() {
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

    /**
     * Given frozen temperature specification, When creating an order, Then it should have
     * temperature set to frozen.
     */
    @Test
    fun `Given frozen temperature specification, When creating an order, Then it should have temperature set to frozen`() {
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
