package com.css.challenge

import com.css.challenge.client.Client
import com.css.challenge.manager.KitchenManager
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.long
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import kotlinx.io.IOException

/**
 * Main entry point for the Cloud Kitchens challenge application.
 *
 * This command-line application fetches test problems from the Cloud Kitchens API, processes
 * orders, and submits solutions for evaluation.
 */
class Main : CliktCommand() {
    /** The Cloud Kitchens API endpoint URL */
    private val endpoint by
            option().default(System.getenv("ENDPOINT") ?: "https://api.cloudkitchens.com")
                    .help("Problem server endpoint")

    /** Authentication token required for API access */
    private val auth by
            option().default(System.getenv("AUTH_TOKEN") ?: "")
                    .help("Authentication token (required)")

    /** Optional problem name identifier */
    private val name by option().help("Problem name. Leave blank (optional)")

    /** Optional seed for deterministic problem generation */
    private val seed by option().long().help("Problem seed (random if unset)")

    /** Rate at which orders are processed (inverse order rate) */
    private val rate by
            option()
                    .convert { Duration.parse(it) }
                    .default(500.milliseconds)
                    .help("Inverse order rate")

    /** Minimum time for order pickup */
    private val min by
            option().convert { Duration.parse(it) }.default(4.seconds).help("Minimum pickup time")

    /** Maximum time for order pickup */
    private val max by
            option().convert { Duration.parse(it) }.default(8.seconds).help("Maximum pickup time")

    /**
     * Executes the main application logic.
     *
     * Fetches a new problem from the API, processes orders sequentially at the configured rate,
     * schedules driver pickups with random delays, waits for all pickups to complete, and submits
     * the action ledger for evaluation.
     *
     * @throws IOException if communication with the API fails
     */
    override fun run() = runBlocking {
        try {
            val client = Client(HttpClient(CIO), auth, endpoint)
            val problem = client.newProblem(name, seed)

            println("========================================")
            println("Cloud Kitchens Order Fulfillment System")
            println("========================================")
            println("Problem ID: ${problem.testId}")
            println("Total Orders: ${problem.orders.size}")
            println("Order Rate: $rate")
            println("Pickup Time: $min - $max")
            println("========================================\n")

            // Initialize kitchen manager
            val kitchen = KitchenManager()

            // Process orders at configured rate
            for ((index, order) in problem.orders.withIndex()) {
                println(
                        "[${index + 1}/${problem.orders.size}] Processing order: ${order.id} (${order.name})"
                )

                // Place order in kitchen
                val placed = kitchen.placeOrder(order)

                if (placed) {
                    // Schedule driver pickup with random delay (runs in background)
                    kitchen.scheduleDriverPickup(
                            orderId = order.id,
                            minPickupTime = min.inWholeSeconds.toInt(),
                            maxPickupTime = max.inWholeSeconds.toInt(),
                            scope = this
                    )
                } else {
                    println("[WARNING] Failed to place order ${order.id}")
                }

                // Wait before processing next order (except for last order)
                if (index < problem.orders.size - 1) {
                    delay(rate)
                }
            }

            println("\n========================================")
            println("All orders placed. Waiting for pickups to complete...")
            println("========================================\n")

            // Wait for all active child coroutines (pickups) to complete
            coroutineContext[Job]?.children?.forEach { it.join() }
            println("All pickups complete!")
            println("========================================\n")

            // Get action ledger from kitchen manager
            val actions = kitchen.getActions()

            println("Submitting ${actions.size} actions to server...")

            // Submit solution to server
            val result = client.solve(problem.testId, rate, min, max, actions)

            println("\n========================================")
            println("Server Validation Result")
            println("========================================")
            println(result)
            println("========================================")
        } catch (e: IOException) {
            println("Simulation failed: ${e.message}")
        }
    }
}

/**
 * Application entry point.
 *
 * Initializes and runs the Main command with the provided command-line arguments.
 *
 * @param args Command-line arguments passed to the application
 */
fun main(args: Array<String>) = Main().main(args)
