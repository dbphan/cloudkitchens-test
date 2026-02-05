package com.css.challenge.model

import com.css.challenge.client.Order
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

/** Tests for order freshness tracking. */
class StoredOrderTest {

    @Test
    fun `creating stored order captures current timestamp`() {
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
    fun `freshness decays at 1x rate when at ideal temperature`() {
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
    fun `wrong temperature causes 2x decay rate`() {
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
    fun `expired orders return zero freshness`() {
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
    fun `isFresh correctly identifies fresh and expired states`() {
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
    fun `hot orders match heater as ideal temperature`() {
        // Arrange
        val order = Order("test-1", "Pizza", "hot", 15, 100)

        // Act & Assert
        assertTrue(StoredOrder(order, currentLocation = "heater").isAtIdealTemperature())
        assertFalse(StoredOrder(order, currentLocation = "cooler").isAtIdealTemperature())
        assertFalse(StoredOrder(order, currentLocation = "shelf").isAtIdealTemperature())
    }

    @Test
    fun `cold orders match cooler as ideal`() {
        // Arrange
        val order = Order("test-1", "Ice Cream", "cold", 8, 60)

        // Act & Assert
        assertTrue(StoredOrder(order, currentLocation = "cooler").isAtIdealTemperature())
        assertFalse(StoredOrder(order, currentLocation = "heater").isAtIdealTemperature())
        assertFalse(StoredOrder(order, currentLocation = "shelf").isAtIdealTemperature())
    }

    @Test
    fun `room temperature orders belong on shelf`() {
        // Arrange
        val order = Order("test-1", "Bread", "room", 5, 120)

        // Act & Assert
        assertTrue(StoredOrder(order, currentLocation = "shelf").isAtIdealTemperature())
        assertFalse(StoredOrder(order, currentLocation = "heater").isAtIdealTemperature())
        assertFalse(StoredOrder(order, currentLocation = "cooler").isAtIdealTemperature())
    }

    @Test
    fun `calculates remaining time until expiration correctly`() {
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
    fun `time until expiration returns zero when already expired`() {
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
