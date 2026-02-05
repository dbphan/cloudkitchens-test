package com.css.challenge.model

import com.css.challenge.client.Order
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

/** Test suite for StoredOrder freshness tracking and calculations. */
class StoredOrderTest {

    @Test
    fun `Given an order and location, When creating a stored order, Then it should have current timestamp`() {
        // Arrange
        val order = Order("test-1", "Pizza", "hot", 15, 300)

        // Act
        val storedOrder = StoredOrder(order, currentLocation = "heater")

        // Assert
        assertEquals("test-1", storedOrder.order.id)
        assertEquals("heater", storedOrder.currentLocation)
        assertTrue(storedOrder.placedAt <= Clock.System.now())
    }

    @Test
    fun `Given an order at ideal temperature, When calculating freshness after elapsed time, Then it should decay at 1x rate`() {
        // Arrange
        val order = Order("test-1", "Pizza", "hot", 15, 100) // 100 seconds freshness
        val placedAt = Instant.parse("2024-01-01T12:00:00Z")
        val storedOrder = StoredOrder(order, placedAt, "heater") // hot order in heater = ideal

        // Act - check after 50 seconds (half the freshness time)
        val checkTime = Instant.parse("2024-01-01T12:00:50Z")
        val freshness = storedOrder.calculateFreshness(checkTime)

        // Assert - should be 50% fresh (1.0 - 50/100 = 0.5)
        assertEquals(0.5, freshness, 0.01)
    }

    @Test
    fun `Given an order at non-ideal temperature, When calculating freshness, Then it should decay at 2x rate`() {
        // Arrange
        val order = Order("test-1", "Pizza", "hot", 15, 100) // 100 seconds freshness
        val placedAt = Instant.parse("2024-01-01T12:00:00Z")
        val storedOrder = StoredOrder(order, placedAt, "shelf") // hot order on shelf = 2x decay

        // Act - check after 25 seconds
        val checkTime = Instant.parse("2024-01-01T12:00:25Z")
        val freshness = storedOrder.calculateFreshness(checkTime)

        // Assert - 25 seconds * 2x decay = 50 effective seconds
        // 1.0 - 50/100 = 0.5
        assertEquals(0.5, freshness, 0.01)
    }

    @Test
    fun `Given an expired order, When calculating freshness, Then it should return zero`() {
        // Arrange
        val order = Order("test-1", "Pizza", "hot", 15, 100)
        val placedAt = Instant.parse("2024-01-01T12:00:00Z")
        val storedOrder = StoredOrder(order, placedAt, "heater")

        // Act - check after 120 seconds (past freshness time)
        val checkTime = Instant.parse("2024-01-01T12:02:00Z")
        val freshness = storedOrder.calculateFreshness(checkTime)

        // Assert
        assertEquals(0.0, freshness, 0.01)
    }

    @Test
    fun `Given different check times, When verifying freshness, Then it should correctly identify fresh and expired states`() {
        // Arrange
        val order = Order("test-1", "Pizza", "hot", 15, 100)
        val placedAt = Instant.parse("2024-01-01T12:00:00Z")
        val storedOrder = StoredOrder(order, placedAt, "heater")

        // Act & Assert - fresh after 50 seconds
        val checkTime1 = Instant.parse("2024-01-01T12:00:50Z")
        assertTrue(storedOrder.isFresh(checkTime1))

        // Act & Assert - not fresh after 120 seconds
        val checkTime2 = Instant.parse("2024-01-01T12:02:00Z")
        assertFalse(storedOrder.isFresh(checkTime2))
    }

    @Test
    fun `Given a hot order, When checking ideal temperature, Then heater should be ideal and others non-ideal`() {
        // Arrange
        val order = Order("test-1", "Pizza", "hot", 15, 100)

        // Act & Assert
        assertTrue(StoredOrder(order, currentLocation = "heater").isAtIdealTemperature())
        assertFalse(StoredOrder(order, currentLocation = "cooler").isAtIdealTemperature())
        assertFalse(StoredOrder(order, currentLocation = "shelf").isAtIdealTemperature())
    }

    @Test
    fun `Given a cold order, When checking ideal temperature, Then cooler should be ideal and others non-ideal`() {
        // Arrange
        val order = Order("test-1", "Ice Cream", "cold", 8, 60)

        // Act & Assert
        assertTrue(StoredOrder(order, currentLocation = "cooler").isAtIdealTemperature())
        assertFalse(StoredOrder(order, currentLocation = "heater").isAtIdealTemperature())
        assertFalse(StoredOrder(order, currentLocation = "shelf").isAtIdealTemperature())
    }

    @Test
    fun `Given a room temperature order, When checking ideal temperature, Then shelf should be ideal and others non-ideal`() {
        // Arrange
        val order = Order("test-1", "Bread", "room", 5, 120)

        // Act & Assert
        assertTrue(StoredOrder(order, currentLocation = "shelf").isAtIdealTemperature())
        assertFalse(StoredOrder(order, currentLocation = "heater").isAtIdealTemperature())
        assertFalse(StoredOrder(order, currentLocation = "cooler").isAtIdealTemperature())
    }

    @Test
    fun `Given a placed order, When calculating time until expiration, Then it should return remaining seconds correctly`() {
        // Arrange
        val order = Order("test-1", "Pizza", "hot", 15, 100)
        val placedAt = Instant.parse("2024-01-01T12:00:00Z")
        val storedOrder = StoredOrder(order, placedAt, "heater")

        // Act - after 30 seconds, should have 70 seconds remaining
        val checkTime = Instant.parse("2024-01-01T12:00:30Z")
        val timeRemaining = storedOrder.timeUntilExpiration(checkTime)

        // Assert
        assertEquals(70.seconds, timeRemaining)
    }

    @Test
    fun `Given an already expired order, When calculating time until expiration, Then it should return zero`() {
        // Arrange
        val order = Order("test-1", "Pizza", "hot", 15, 100)
        val placedAt = Instant.parse("2024-01-01T12:00:00Z")
        val storedOrder = StoredOrder(order, placedAt, "heater")

        // Act - after 120 seconds (already expired)
        val checkTime = Instant.parse("2024-01-01T12:02:00Z")
        val timeRemaining = storedOrder.timeUntilExpiration(checkTime)

        // Assert
        assertEquals(0.seconds, timeRemaining)
    }
}
