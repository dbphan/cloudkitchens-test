package com.css.challenge.client

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

/**
 * Test suite for the Action data class.
 *
 * Tests action creation, serialization, and constant values.
 */
class ActionTest {

    /** Tests creating an action with long timestamp. */
    @Test
    fun `should create action with long timestamp`() {
        // Arrange
        val timestamp = 1609459200000000L // 2021-01-01 00:00:00 UTC in microseconds
        val orderId = "order-123"
        val actionType = PLACE
        val target = COOLER

        // Act
        val action = Action(timestamp, orderId, actionType, target)

        // Assert
        assertEquals(timestamp, action.timestamp)
        assertEquals(orderId, action.id)
        assertEquals(actionType, action.action)
        assertEquals(target, action.target)
    }

    /** Tests creating an action with Instant timestamp. */
    @Test
    fun `should create action with Instant timestamp`() {
        // Arrange
        val instant = Clock.System.now()
        val orderId = "order-456"
        val actionType = PICKUP
        val target = HEATER

        // Act
        val action = Action(instant, orderId, actionType, target)

        // Assert
        assertEquals(instant.toEpochMilliseconds() * 1000L, action.timestamp)
        assertEquals(orderId, action.id)
        assertEquals(actionType, action.action)
        assertEquals(target, action.target)
    }

    /** Tests all action type constants. */
    @Test
    fun `should have all action type constants defined`() {
        // Assert
        assertEquals("place", PLACE)
        assertEquals("pickup", PICKUP)
        assertEquals("move", MOVE)
        assertEquals("discard", DISCARD)
    }

    /** Tests all target location constants. */
    @Test
    fun `should have all target constants defined`() {
        // Assert
        assertEquals("heater", HEATER)
        assertEquals("cooler", COOLER)
        assertEquals("shelf", SHELF)
    }

    /** Tests action with move operation. */
    @Test
    fun `should create move action`() {
        // Arrange
        val timestamp = Instant.parse("2024-01-01T12:00:00Z")
        val orderId = "order-789"

        // Act
        val action = Action(timestamp, orderId, MOVE, SHELF)

        // Assert
        assertEquals(MOVE, action.action)
        assertEquals(SHELF, action.target)
    }

    /** Tests action with discard operation. */
    @Test
    fun `should create discard action`() {
        // Arrange
        val timestamp = Instant.parse("2024-01-01T12:00:00Z")
        val orderId = "order-999"

        // Act
        val action = Action(timestamp, orderId, DISCARD, HEATER)

        // Assert
        assertEquals(DISCARD, action.action)
        assertNotNull(action.timestamp)
    }

    /** Tests timestamp conversion from milliseconds to microseconds. */
    @Test
    fun `should convert timestamp to microseconds correctly`() {
        // Arrange
        val instant = Instant.fromEpochMilliseconds(1000L) // 1 second

        // Act
        val action = Action(instant, "test-order", PLACE, COOLER)

        // Assert
        assertEquals(1000000L, action.timestamp) // 1000ms * 1000 = 1,000,000 microseconds
    }
}
