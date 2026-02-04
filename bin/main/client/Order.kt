package com.css.challenge.client

import kotlinx.serialization.Serializable

/**
 * Represents a food order in the kitchen management system.
 *
 * Each order contains information about the food item, its temperature requirements, pricing, and
 * freshness constraints.
 *
 * @property id Unique identifier for the order
 * @property name Name of the food item
 * @property temp Ideal temperature category for the food (hot, cold, or frozen)
 * @property price Price of the order in dollars
 * @property freshness Time in seconds before the order becomes stale
 */
@Serializable
data class Order(
        val id: String,
        val name: String,
        val temp: String,
        val price: Int = 0,
        val freshness: Int
)
