package com.css.challenge.client

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Action type: Place an order on a shelf */
const val PLACE = "place"

/** Action type: Pick up an order from a shelf */
const val PICKUP = "pickup"

/** Action type: Move an order between shelves */
const val MOVE = "move"

/** Action type: Discard an order */
const val DISCARD = "discard"

/** Target location: Heater shelf for hot items */
const val HEATER = "heater"

/** Target location: Cooler shelf for cold items */
const val COOLER = "cooler"

/** Target location: General shelf for any temperature items */
const val SHELF = "shelf"

/**
 * Represents an action performed on an order in the kitchen management system.
 *
 * Actions track what happens to orders over time, including placement on shelves, movement between
 * locations, pickup by drivers, and disposal.
 *
 * @property timestamp Unix timestamp in microseconds when the action occurred
 * @property id Unique identifier of the order this action applies to
 * @property action Type of action performed (place, pickup, move, or discard)
 * @property target Target location for the action (heater, cooler, or shelf)
 */
@Serializable
data class Action(
        val timestamp: Long,
        val id: String,
        val action: String,
        val target: String,
) {
    /**
     * Convenience constructor that accepts an Instant timestamp.
     *
     * Converts the Instant to Unix timestamp in microseconds for serialization.
     *
     * @param timestamp The instant when the action occurred
     * @param id Unique identifier of the order
     * @param action Type of action performed
     * @param target Target location for the action
     */
    constructor(
            timestamp: Instant,
            id: String,
            action: String,
            target: String
    ) : this(timestamp.toEpochMilliseconds() * 1000L, id, action, target)
}
