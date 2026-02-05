package com.css.challenge.model

import com.css.challenge.client.Order
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** Tracks an order's freshness and storage location. */
data class StoredOrder(
        val order: Order,
        val placedAt: Instant = Clock.System.now(),
        var currentLocation: String,
) {
    /** Calculates freshness (0.0 to 1.0). Wrong temperature = 2x decay rate. */
    fun calculateFreshness(currentTime: Instant = Clock.System.now()): Double {
        val elapsedSeconds = (currentTime - placedAt).inWholeSeconds.toDouble()
        val decayRate = if (isAtIdealTemperature()) 1.0 else 2.0
        val effectiveAge = elapsedSeconds * decayRate
        val freshness = 1.0 - (effectiveAge / order.freshness)

        return freshness.coerceAtLeast(0.0)
    }

    fun isFresh(currentTime: Instant = Clock.System.now()): Boolean {
        return calculateFreshness(currentTime) > 0.0
    }

    fun isAtIdealTemperature(): Boolean {
        return when (order.temp) {
            "hot" -> currentLocation == "heater"
            "cold" -> currentLocation == "cooler"
            "room" -> currentLocation == "shelf"
            else -> false
        }
    }

    fun timeUntilExpiration(currentTime: Instant = Clock.System.now()): kotlin.time.Duration {
        val decayRate = if (isAtIdealTemperature()) 1.0 else 2.0
        val remainingSeconds =
                (order.freshness - (currentTime - placedAt).inWholeSeconds * decayRate)
        return if (remainingSeconds > 0) remainingSeconds.toInt().seconds else 0.seconds
    }
}
