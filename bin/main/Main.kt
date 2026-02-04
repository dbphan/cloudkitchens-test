package com.css.challenge

import com.css.challenge.client.Action
import com.css.challenge.client.COOLER
import com.css.challenge.client.Client
import com.css.challenge.client.PLACE
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.long
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock.System.now
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
     * Fetches a new problem from the API, processes orders sequentially, and submits the solution
     * for evaluation.
     *
     * @throws IOException if communication with the API fails
     */
    override fun run() = runBlocking {
        try {
            val client = Client(HttpClient(CIO), auth, endpoint)
            val problem = client.newProblem(name, seed)

            // ------ Execution harness logic goes here using rate, min and max ----

            val actions = mutableListOf<Action>()
            for (order in problem.orders) {
                println("Received: $order")

                actions.add(Action(now(), order.id, PLACE, COOLER))
                delay(rate)
            }

            // ----------------------------------------------------------------------

            val result = client.solve(problem.testId, rate, min, max, actions)
            println("Result: $result")
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
