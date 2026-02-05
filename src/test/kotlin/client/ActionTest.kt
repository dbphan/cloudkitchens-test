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

    /**
     * Given a long timestamp value, When creating an action, Then it should have all properties
     * correctly assigned.
     */
    @Test
    fun `Given a long timestamp value, When creating an action, Then it should have all properties correctly assigned`() {
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

    /**
     * Given an Instant timestamp, When creating an action, Then it should convert to microseconds
     * correctly.
     */
    @Test
    fun `Given an Instant timestamp, When creating an action, Then it should convert to microseconds correctly`() {
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

    /**
     * Given action type constants, When checking their values, Then they should match expected
     * action types.
     */
    @Test
    fun `Given action type constants, When checking their values, Then they should match expected action types`() {
        // Assert
        assertEquals("place", PLACE)
        assertEquals("pickup", PICKUP)
        assertEquals("move", MOVE)
        assertEquals("discard", DISCARD)
    }

    /**
     * Given target location constants, When checking their values, Then they should match expected
     * storage locations.
     */
    @Test
    fun `Given target location constants, When checking their values, Then they should match expected storage locations`() {
        // Assert
        assertEquals("heater", HEATER)
        assertEquals("cooler", COOLER)
        assertEquals("shelf", SHELF)
    }

    /**
     * Given move operation parameters, When creating an action, Then it should have action type set
     * to move.
     */
    @Test
    fun `Given move operation parameters, When creating an action, Then it should have action type set to move`() {
        // Arrange
        val timestamp = Instant.parse("2024-01-01T12:00:00Z")
        val orderId = "order-789"

        // Act
        val action = Action(timestamp, orderId, MOVE, SHELF)

        // Assert
        assertEquals(MOVE, action.action)
        assertEquals(SHELF, action.target)
    }

    /**
     * Given discard operation parameters, When creating an action, Then it should have action type
     * set to discard.
     */
    @Test
    fun `Given discard operation parameters, When creating an action, Then it should have action type set to discard`() {
        // Arrange
        val timestamp = Instant.parse("2024-01-01T12:00:00Z")
        val orderId = "order-999"

        // Act
        val action = Action(timestamp, orderId, DISCARD, HEATER)

        // Assert
        assertEquals(DISCARD, action.action)
        assertNotNull(action.timestamp)
    }

    /**
     * Given a timestamp in milliseconds, When creating an action with Instant, Then it should
     * convert to microseconds.
     */
    @Test
    fun `Given a timestamp in milliseconds, When creating an action with Instant, Then it should convert to microseconds`() {
        // Arrange
        val instant = Instant.fromEpochMilliseconds(1000L) // 1 second

        // Act
        val action = Action(instant, "test-order", PLACE, COOLER)

        // Assert
        assertEquals(1000000L, action.timestamp) // 1000ms * 1000 = 1,000,000 microseconds
    }
}
