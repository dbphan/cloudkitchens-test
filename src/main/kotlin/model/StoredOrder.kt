package com.css.challenge.model

import com.css.challenge.client.Order
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Represents an order that has been placed in storage.
 *
 * Wraps the original Order with additional runtime information needed for managing freshness and
 * storage location.
 *
 * @property order The original order details
 * @property placedAt The instant when the order was placed in storage
 * @property currentLocation The current storage location (heater, cooler, or shelf)
 */
data class StoredOrder(
        val order: Order,
        val placedAt: Instant = Clock.System.now(),
        var currentLocation: String,
) {
    /**
     * Calculates the current freshness value of the order.
     *
     * Freshness decreases over time. Orders stored at their ideal temperature degrade at normal
     * rate, while orders at non-ideal temperatures degrade twice as fast.
     *
     * @param currentTime The time to calculate freshness at (defaults to now)
     * @return Current freshness value (0.0 to 1.0, where 1.0 is perfectly fresh)
     */
    fun calculateFreshness(currentTime: Instant = Clock.System.now()): Double {
        val elapsedSeconds = (currentTime - placedAt).inWholeSeconds.toDouble()

        // Determine decay rate based on storage temperature match
        val decayRate = if (isAtIdealTemperature()) 1.0 else 2.0

        // Calculate effective age with decay rate applied
        val effectiveAge = elapsedSeconds * decayRate

        // Calculate remaining freshness (1.0 = fresh, 0.0 = expired)
        val freshness = 1.0 - (effectiveAge / order.freshness)

        return freshness.coerceAtLeast(0.0)
    }

    /**
     * Checks if the order is still fresh.
     *
     * @param currentTime The time to check freshness at (defaults to now)
     * @return true if freshness is above 0, false if expired
     */
    fun isFresh(currentTime: Instant = Clock.System.now()): Boolean {
        return calculateFreshness(currentTime) > 0.0
    }

    /**
     * Checks if the order is stored at its ideal temperature.
     *
     * @return true if current location matches ideal temperature
     */
    fun isAtIdealTemperature(): Boolean {
        return when (order.temp) {
            "hot" -> currentLocation == "heater"
            "cold" -> currentLocation == "cooler"
            "room" -> currentLocation == "shelf"
            else -> false
        }
    }

    /**
     * Calculates time until the order expires.
     *
     * @param currentTime The time to calculate from (defaults to now)
     * @return Duration until expiration, or zero if already expired
     */
    fun timeUntilExpiration(currentTime: Instant = Clock.System.now()): kotlin.time.Duration {
        val decayRate = if (isAtIdealTemperature()) 1.0 else 2.0
        val remainingSeconds =
                (order.freshness - (currentTime - placedAt).inWholeSeconds * decayRate)
        return if (remainingSeconds > 0) remainingSeconds.toInt().seconds else 0.seconds
    }
}
