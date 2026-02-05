package com.css.challenge

import com.css.challenge.client.Client
import com.css.challenge.service.KitchenService
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

class Main : CliktCommand(name = "challenge") {

    override fun help(context: com.github.ajalt.clikt.core.Context): String {
        return """
            Cloud Kitchens Order Fulfillment System
            
            Simulates a kitchen order fulfillment system with intelligent storage management,
            concurrent driver pickups, and value-based discard strategy.
            
            For invalid options, use --help to see all available options.
        """.trimIndent()
    }

    private val endpoint by
            option().default(System.getenv("ENDPOINT") ?: "https://api.cloudkitchens.com")
                    .help("Problem server endpoint")

    private val auth by
            option().default(System.getenv("AUTH_TOKEN") ?: "")
                    .help("Authentication token (required)")

    private val name by option().help("Problem name. Leave blank (optional)")
    private val seed by option().long().help("Problem seed (random if unset)")

    private val rate by
            option()
                    .convert { Duration.parse(it) }
                    .default(500.milliseconds)
                    .help("Inverse order rate")

    private val min by
            option().convert { Duration.parse(it) }.default(4.seconds).help("Minimum pickup time")

    private val max by
            option().convert { Duration.parse(it) }.default(8.seconds).help("Maximum pickup time")

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

            val kitchen = KitchenService()

            // Process orders at configured rate
            for ((index, order) in problem.orders.withIndex()) {
                println(
                        "[${index + 1}/${problem.orders.size}] Processing order: ${order.id} (${order.name})"
                )

                val placed = kitchen.placeOrder(order)

                if (placed) {
                    kitchen.scheduleDriverPickup(
                            orderId = order.id,
                            minPickupTime = min.inWholeSeconds.toInt(),
                            maxPickupTime = max.inWholeSeconds.toInt(),
                            scope = this
                    )
                } else {
                    println("[WARNING] Failed to place order ${order.id}")
                }

                if (index < problem.orders.size - 1) {
                    delay(rate)
                }
            }

            println("\n========================================")
            println("All orders placed. Waiting for pickups to complete...")
            println("========================================\n")

            // Wait for all pickups to complete
            coroutineContext[Job]?.children?.forEach { it.join() }
            println("All pickups complete!")
            println("========================================\n")

            val actions = kitchen.getActions()

            println("Submitting ${actions.size} actions to server...")

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

fun main(args: Array<String>) = Main().main(args)
