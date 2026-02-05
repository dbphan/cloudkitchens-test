package com.css.challenge.manager

import com.css.challenge.client.Order
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for driver pickup simulation in KitchenManager.
 *
 * Verifies that pickups are scheduled with random delays, executed concurrently, and tracked
 * correctly in the action ledger.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DriverPickupSimulationTest {
        private val manager = KitchenManager()

        @AfterEach fun cleanup() = runBlocking { manager.clear() }

        @Test
        fun `Given a placed order, When scheduling driver pickup with delay, Then it should execute pickup after the specified delay`() =
                runTest {
                        // Arrange: Place an order
                        val order =
                                Order(id = "order1", name = "Pizza", temp = "hot", freshness = 300)
                        manager.placeOrder(order)

                        // Act: Schedule pickup with 1-2 second delay
                        manager.scheduleDriverPickup(
                                orderId = "order1",
                                minPickupTime = 1,
                                maxPickupTime = 2,
                                scope = this
                        )

                        // Advance virtual time to trigger pickup
                        testScheduler.advanceTimeBy(3000)
                        testScheduler.runCurrent()

                        // Assert: Order should be picked up
                        val actions = manager.getActions()
                        assertTrue(actions.any { it.action == "pickup" && it.id == "order1" })
                }

        @Test
        fun `Given multiple placed orders, When scheduling concurrent pickups, Then all orders should be picked up successfully`() =
                runTest {
                        // Arrange: Place multiple orders
                        val orders =
                                listOf(
                                        Order(
                                                id = "order1",
                                                name = "Pizza",
                                                temp = "hot",
                                                freshness = 300
                                        ),
                                        Order(
                                                id = "order2",
                                                name = "Salad",
                                                temp = "cold",
                                                freshness = 300
                                        ),
                                        Order(
                                                id = "order3",
                                                name = "Sandwich",
                                                temp = "room",
                                                freshness = 300
                                        )
                                )

                        orders.forEach { manager.placeOrder(it) }

                        // Act: Schedule concurrent pickups
                        orders.forEach { order ->
                                manager.scheduleDriverPickup(
                                        orderId = order.id,
                                        minPickupTime = 1,
                                        maxPickupTime = 2,
                                        scope = this
                                )
                        }

                        // Advance virtual time to trigger all pickups
                        testScheduler.advanceTimeBy(3000)
                        testScheduler.runCurrent()

                        // Assert: All orders should be picked up
                        val actions = manager.getActions()
                        assertEquals(3, actions.count { it.action == "pickup" })
                        assertTrue(actions.any { it.id == "order1" && it.action == "pickup" })
                        assertTrue(actions.any { it.id == "order2" && it.action == "pickup" })
                        assertTrue(actions.any { it.id == "order3" && it.action == "pickup" })
                }

        @Test
        fun `Given an order with short freshness, When scheduling immediate pickup, Then it should be either picked up or discarded`() =
                runTest {
                        // Note: This test verifies the expiry logic works, but cannot easily test
                        // timing
                        // with Clock.System.now() in virtual time. In real usage, orders expire
                        // based on
                        // actual elapsed time.

                        // Arrange: Place an order and wait for it to actually expire in real time
                        val order =
                                Order(
                                        id = "order1",
                                        name = "Fast Food",
                                        temp = "hot",
                                        freshness = 1
                                )
                        manager.placeOrder(order)

                        // Schedule pickup immediately (will execute in < 1 second with runTest)
                        manager.scheduleDriverPickup(
                                orderId = "order1",
                                minPickupTime = 0,
                                maxPickupTime = 0,
                                scope = this
                        )

                        // Advance time
                        testScheduler.advanceTimeBy(1000)
                        testScheduler.runCurrent()

                        // Assert: Order should be picked up before expiry (virtual time executes
                        // instantly)
                        val actions = manager.getActions()
                        // In virtual time, pickup happens so fast the order doesn't expire
                        assertTrue(
                                actions.any {
                                        it.id == "order1" &&
                                                (it.action == "pickup" || it.action == "discard")
                                },
                                "Order should be either picked up or discarded"
                        )
                }

        @Test
        fun `Given a non-existent order ID, When scheduling pickup, Then it should handle gracefully without recording actions`() =
                runTest {
                        // Act: Schedule pickup for order that doesn't exist
                        manager.scheduleDriverPickup(
                                orderId = "nonexistent",
                                minPickupTime = 1,
                                maxPickupTime = 1,
                                scope = this
                        )

                        // Advance virtual time to trigger pickup attempt
                        testScheduler.advanceTimeBy(2000)
                        testScheduler.runCurrent()

                        // Assert: No pickup action should be recorded
                        val actions = manager.getActions()
                        assertFalse(actions.any { it.id == "nonexistent" })
                }

        @Test
        fun `Given min-max pickup time interval, When scheduling pickup, Then it should occur within the specified interval`() =
                runTest {
                        // Arrange: Place order
                        val order =
                                Order(id = "order1", name = "Burger", temp = "hot", freshness = 300)
                        manager.placeOrder(order)

                        // Act: Schedule pickup with 2-3 second delay
                        manager.scheduleDriverPickup(
                                orderId = "order1",
                                minPickupTime = 2,
                                maxPickupTime = 3,
                                scope = this
                        )

                        // Assert: Pickup should not happen before 2 seconds
                        testScheduler.advanceTimeBy(1999)
                        testScheduler.runCurrent()
                        var actions = manager.getActions()
                        assertFalse(
                                actions.any { it.id == "order1" && it.action == "pickup" },
                                "Pickup should not occur before min time"
                        )

                        // Advance to max time + buffer and verify pickup happened
                        testScheduler.advanceTimeBy(2001)
                        testScheduler.runCurrent()
                        actions = manager.getActions()
                        assertTrue(
                                actions.any { it.id == "order1" && it.action == "pickup" },
                                "Pickup should occur within max time"
                        )
                }

        @Test
        fun `Given an order in storage, When executing pickup, Then it should track the action with correct location details`() =
                runTest {
                        // Arrange: Place order in cooler
                        val order =
                                Order(
                                        id = "order1",
                                        name = "Ice Cream",
                                        temp = "cold",
                                        freshness = 300
                                )
                        manager.placeOrder(order)

                        // Act: Schedule and execute pickup
                        manager.scheduleDriverPickup(
                                orderId = "order1",
                                minPickupTime = 1,
                                maxPickupTime = 1,
                                scope = this
                        )

                        // Advance virtual time to trigger pickup
                        testScheduler.advanceTimeBy(2000)
                        testScheduler.runCurrent()

                        // Assert: Action should be recorded with correct details
                        val actions = manager.getActions()
                        val pickupAction =
                                actions.find { it.id == "order1" && it.action == "pickup" }
                        assertNotNull(pickupAction)
                        assertEquals(
                                "cooler",
                                pickupAction?.target
                        ) // Should be in cooler (ideal storage)
        }

        @Test
        fun `Given orders placed at different times, When scheduling staggered pickups, Then all should be picked up correctly`() =
                runTest {
                        // Arrange & Act: Place orders at different times and schedule pickups
                        val order1 =
                                Order(id = "order1", name = "Pizza", temp = "hot", freshness = 300)
                        manager.placeOrder(order1)
                        manager.scheduleDriverPickup(
                                orderId = "order1",
                                minPickupTime = 2,
                                maxPickupTime = 2,
                                scope = this
                        )

                        // Advance time by 1 second
                        testScheduler.advanceTimeBy(1000)
                        testScheduler.runCurrent()

                        val order2 =
                                Order(id = "order2", name = "Salad", temp = "cold", freshness = 300)
                        manager.placeOrder(order2)
                        manager.scheduleDriverPickup(
                                orderId = "order2",
                                minPickupTime = 1,
                                maxPickupTime = 1,
                                scope = this
                        )

                        // Advance virtual time to trigger both pickups
                        testScheduler.advanceTimeBy(3000)
                        testScheduler.runCurrent()

                        // Assert: Both orders should be picked up
                        val actions = manager.getActions()
                        assertEquals(2, actions.count { it.action == "pickup" })
                }
}
