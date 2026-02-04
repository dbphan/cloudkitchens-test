package com.css.challenge.client

/**
 * Represents a test problem from the Cloud Kitchens challenge.
 *
 * A problem consists of a unique test identifier and a sequence of orders that need to be
 * efficiently managed through the kitchen system.
 *
 * @property testId Unique identifier for this test problem instance
 * @property orders List of orders to be processed as part of this problem
 */
data class Problem(val testId: String, val orders: List<Order>)
