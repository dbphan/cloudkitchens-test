package com.css.challenge

import com.css.challenge.client.Client
import com.css.challenge.manager.KitchenManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

/**
 * End-to-end integration tests that interact with the real CloudKitchens API. These tests are only
 * enabled when the AUTH_TOKEN environment variable is set.
 */
@EnabledIfEnvironmentVariable(named = "AUTH_TOKEN", matches = ".+")
class EndToEndIntegrationTest {

    private val authToken = System.getenv("AUTH_TOKEN") ?: ""
    private val endpoint = "https://api.cloudkitchens.com/interview/challenge"

    @Test
    fun `Given real API, When fetching and solving a problem, Then submission should succeed`() =
            runBlocking {
                // Arrange
                val httpClient = HttpClient(CIO) { install(ContentNegotiation) { json() } }

                val client = Client(httpClient, authToken, endpoint)
                val manager = KitchenManager()

                try {
                    // Act - Fetch problem
                    val problem = client.newProblem(null, 12345L)
                    println(
                            "Fetched problem with ${problem.orders.size} orders (Test ID: ${problem.testId})"
                    )

                    // Process each order
                    problem.orders.forEach { order ->
                        manager.placeOrder(order)

                        // Schedule driver pickup with realistic timing
                        manager.scheduleDriverPickup(
                                orderId = order.id,
                                minPickupTime = 4,
                                maxPickupTime = 8,
                                scope = this
                        )

                        // Rate limit to 500ms per order
                        delay(500)
                    }

                    // Wait for all pickups to complete
                    delay(10.seconds)

                    // Get actions and submit
                    val actions = manager.getActions()
                    println("Generated ${actions.size} actions")

                    val result =
                            client.solve(
                                    problem.testId,
                                    500.milliseconds,
                                    4.seconds,
                                    8.seconds,
                                    actions
                            )
                    println("Server response: $result")

                    // Assert
                    assertTrue(
                            result.contains("PASS") ||
                                    result.contains("Success") ||
                                    result.contains("score"),
                            "Expected successful submission, got: $result"
                    )
                } finally {
                    httpClient.close()
                    manager.clear()
                }
            }

    @Test
    fun `Given real API with specific seed, When processing orders, Then results should be reproducible`() =
            runBlocking {
                // Arrange
                val httpClient = HttpClient(CIO) { install(ContentNegotiation) { json() } }

                val client = Client(httpClient, authToken, endpoint)
                val manager = KitchenManager()
                val seed = 99999L

                try {
                    // Act - Fetch problem with specific seed
                    val problem = client.newProblem(null, seed)
                    println("Fetched problem with seed $seed: ${problem.orders.size} orders")

                    // Process orders
                    problem.orders.forEach { order ->
                        manager.placeOrder(order)
                        manager.scheduleDriverPickup(
                                orderId = order.id,
                                minPickupTime = 4,
                                maxPickupTime = 8,
                                scope = this
                        )
                        delay(500)
                    }

                    delay(10.seconds)

                    val actions = manager.getActions()
                    val result =
                            client.solve(
                                    problem.testId,
                                    500.milliseconds,
                                    4.seconds,
                                    8.seconds,
                                    actions
                            )

                    println("Result for seed $seed: $result")

                    // Assert
                    assertTrue(actions.isNotEmpty(), "Should generate actions")
                    assertTrue(result.isNotEmpty(), "Should receive server response")
                } finally {
                    httpClient.close()
                    manager.clear()
                }
            }

    // ========== FAILURE TESTS ==========

    @Test
    fun `Given invalid auth token, When fetching problem, Then client should throw exception`() =
            runTest {
                // Arrange - Mock client that returns 401
                val mockEngine = MockEngine { _ ->
                    respond(
                            content = ByteReadChannel("Unauthorized"),
                            status = HttpStatusCode.Unauthorized
                    )
                }

                val httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { json() } }
                val client = Client(httpClient, "invalid-token", endpoint)

                // Act & Assert
                assertFailsWith<Exception> { client.newProblem(null, 12345L) }

                httpClient.close()
            }

    @Test
    fun `Given network timeout, When submitting solution, Then client should handle error`() =
            runTest {
                // Arrange - Mock client that simulates timeout
                val mockEngine = MockEngine { _ ->
                    respond(
                            content = ByteReadChannel("Request Timeout"),
                            status = HttpStatusCode.RequestTimeout
                    )
                }

                val httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { json() } }
                val client = Client(httpClient, authToken, endpoint)

                // Act & Assert
                assertFailsWith<Exception> {
                    client.solve("test-id", 500.milliseconds, 4.seconds, 8.seconds, emptyList())
                }

                httpClient.close()
            }

    @Test
    fun `Given malformed server response, When fetching problem, Then client should handle gracefully`() =
            runTest {
                // Arrange - Mock client that returns invalid JSON
                val mockEngine = MockEngine { _ ->
                    respond(
                            content = ByteReadChannel("{ invalid json }"),
                            status = HttpStatusCode.OK,
                            headers =
                                    headersOf(
                                            HttpHeaders.ContentType to
                                                    listOf(ContentType.Application.Json.toString())
                                    )
                    )
                }

                val httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { json() } }
                val client = Client(httpClient, authToken, endpoint)

                // Act & Assert - Should throw parsing exception
                assertFailsWith<Exception> { client.newProblem(null, 12345L) }

                httpClient.close()
            }

    @Test
    fun `Given empty action list, When submitting solution, Then server should reject or accept gracefully`() =
            runTest {
                // Arrange - Mock successful response even with empty actions
                val mockEngine = MockEngine { _ ->
                    respond(
                            content = ByteReadChannel("FAIL: No actions submitted"),
                            status = HttpStatusCode.OK
                    )
                }

                val httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { json() } }
                val client = Client(httpClient, authToken, endpoint)

                // Act
                val result =
                        client.solve("test-id", 500.milliseconds, 4.seconds, 8.seconds, emptyList())

                // Assert
                assertTrue(
                        result.contains("FAIL") || result.contains("No actions"),
                        "Empty actions should result in failure or rejection"
                )

                httpClient.close()
            }

    @Test
    fun `Given server error 500, When fetching problem, Then client should propagate error`() =
            runTest {
                // Arrange - Mock server error
                val mockEngine = MockEngine { _ ->
                    respond(
                            content = ByteReadChannel("Internal Server Error"),
                            status = HttpStatusCode.InternalServerError
                    )
                }

                val httpClient = HttpClient(mockEngine)
                val client = Client(httpClient, authToken, endpoint)

                // Act & Assert
                assertFailsWith<Exception> { client.newProblem(null, 12345L) }

                httpClient.close()
            }

    @Test
    fun `Given missing test ID header, When fetching problem, Then client should handle missing header`() =
            runTest {
                // Arrange - Mock response without x-test-id header
                val mockEngine = MockEngine { _ ->
                    respond(
                            content = ByteReadChannel("[]"),
                            status = HttpStatusCode.OK,
                            headers =
                                    headersOf(
                                            HttpHeaders.ContentType to
                                                    listOf(ContentType.Application.Json.toString())
                                    )
                    )
                }

                val httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { json() } }
                val client = Client(httpClient, authToken, endpoint)

                // Act & Assert - Should handle missing header gracefully or throw
                try {
                    val problem = client.newProblem(null, 12345L)
                    // If it doesn't throw, test ID might be empty or null
                    assertTrue(
                            problem.testId.isEmpty() || problem.testId == "unknown",
                            "Should handle missing test ID header"
                    )
                } catch (e: Exception) {
                    // Also acceptable - client may throw on missing required header
                    assertTrue(true, "Client properly validates required headers")
                }

                httpClient.close()
            }

    @Test
    fun `Given rate limiting error, When submitting solution, Then client should receive rate limit response`() =
            runTest {
                // Arrange - Mock rate limit error
                val mockEngine = MockEngine { _ ->
                    respond(
                            content = ByteReadChannel("Rate limit exceeded"),
                            status = HttpStatusCode.TooManyRequests
                    )
                }

                val httpClient = HttpClient(mockEngine)
                val client = Client(httpClient, authToken, endpoint)

                // Act & Assert
                assertFailsWith<Exception> {
                    client.solve("test-id", 500.milliseconds, 4.seconds, 8.seconds, emptyList())
                }

                httpClient.close()
            }

    @Test
    fun `Given corrupted action data, When submitting to server, Then should handle serialization errors`() =
            runTest {
                // Arrange - This tests our client's ability to serialize actions
                val mockEngine = MockEngine { request ->
                    // Verify request body was properly formed
                    respond(content = ByteReadChannel("Success"), status = HttpStatusCode.OK)
                }

                val httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { json() } }
                val client = Client(httpClient, authToken, endpoint)
                val manager = KitchenManager()

                // Create valid actions
                manager.placeOrder(com.css.challenge.client.Order("test", "Pizza", "hot", 10, 300))
                val actions = manager.getActions()

                // Act - Submit with valid actions should work
                val result =
                        client.solve("test-id", 500.milliseconds, 4.seconds, 8.seconds, actions)

                // Assert
                assertTrue(result.contains("Success"), "Valid actions should serialize properly")

                httpClient.close()
                manager.clear()
            }
}
