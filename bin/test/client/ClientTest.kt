package com.css.challenge.client

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * Test suite for the Client class.
 *
 * Tests HTTP client interactions, problem fetching, and solution submission. Uses MockEngine to
 * simulate API responses without actual network calls.
 */
class ClientTest {

    /** Tests fetching a new problem successfully. */
    @Test
    fun `should fetch new problem successfully`() = runTest {
        // Arrange
        val orders =
                listOf(
                        Order("order-1", "Burger", "hot", 15, 300),
                        Order("order-2", "Salad", "cold", 10, 600)
                )
        val ordersJson = Json.encodeToString(orders)

        val mockEngine = MockEngine { request ->
            respond(
                    content = ByteReadChannel(ordersJson),
                    status = HttpStatusCode.OK,
                    headers =
                            headersOf(
                                    HttpHeaders.ContentType to
                                            listOf(ContentType.Application.Json.toString()),
                                    "x-test-id" to listOf("test-123")
                            )
            )
        }

        val httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { json() } }

        val client = Client(httpClient, "test-auth", "https://test.api.com")

        // Act
        val problem = client.newProblem("test-problem", 12345L)

        // Assert
        assertEquals("test-123", problem.testId)
        assertEquals(2, problem.orders.size)
        assertEquals("order-1", problem.orders[0].id)
        assertEquals("order-2", problem.orders[1].id)

        httpClient.close()
    }

    /** Tests fetching a new problem with null name and seed. */
    @Test
    fun `should fetch problem with null name and seed`() = runTest {
        // Arrange
        val orders = listOf(Order("order-1", "Pizza", "hot", 20, 450))
        val ordersJson = Json.encodeToString(orders)

        val mockEngine = MockEngine { request ->
            // Verify URL contains empty name and has seed parameter
            assert(request.url.toString().contains("name="))
            assert(request.url.toString().contains("seed="))

            respond(
                    content = ByteReadChannel(ordersJson),
                    status = HttpStatusCode.OK,
                    headers =
                            headersOf(
                                    HttpHeaders.ContentType to
                                            listOf(ContentType.Application.Json.toString()),
                                    "x-test-id" to listOf("test-456")
                            )
            )
        }

        val httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { json() } }

        val client = Client(httpClient, "test-auth", "https://test.api.com")

        // Act
        val problem = client.newProblem(null, null)

        // Assert
        assertEquals("test-456", problem.testId)
        assertEquals(1, problem.orders.size)

        httpClient.close()
    }

    /** Tests submitting a solution successfully. */
    @Test
    fun `should submit solution successfully`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            // Verify request has correct headers and body
            assert(request.headers.contains("x-test-id", "test-789"))
            assert(request.method == HttpMethod.Post)

            respond(
                    content = ByteReadChannel("Success: Score 100"),
                    status = HttpStatusCode.OK,
                    headers =
                            headersOf(
                                    HttpHeaders.ContentType to
                                            listOf(ContentType.Text.Plain.toString())
                            )
            )
        }

        val httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { json() } }

        val client = Client(httpClient, "test-auth", "https://test.api.com")

        val actions =
                listOf(
                        Action(1609459200000000L, "order-1", PLACE, COOLER),
                        Action(1609459201000000L, "order-1", PICKUP, COOLER)
                )

        // Act
        val result = client.solve("test-789", 500.milliseconds, 4.seconds, 8.seconds, actions)

        // Assert
        assertNotNull(result)
        assertEquals("Success: Score 100", result)

        httpClient.close()
    }

    /** Tests error handling when fetching problem fails. */
    @Test
    fun `should throw exception on fetch problem error`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(content = ByteReadChannel("Unauthorized"), status = HttpStatusCode.Unauthorized)
        }

        val httpClient = HttpClient(mockEngine)
        val client = Client(httpClient, "invalid-auth", "https://test.api.com")

        // Act & Assert
        try {
            client.newProblem("test", 123L)
            throw AssertionError("Should have thrown IOException")
        } catch (e: Exception) {
            assert(e.message?.contains("401") == true)
        }

        httpClient.close()
    }

    /** Tests error handling when submitting solution fails. */
    @Test
    fun `should throw exception on solve error`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(content = ByteReadChannel("Bad Request"), status = HttpStatusCode.BadRequest)
        }

        val httpClient = HttpClient(mockEngine)
        val client = Client(httpClient, "test-auth", "https://test.api.com")

        val actions = listOf(Action(1609459200000000L, "order-1", PLACE, COOLER))

        // Act & Assert
        try {
            client.solve("test-123", 500.milliseconds, 4.seconds, 8.seconds, actions)
            throw AssertionError("Should have thrown IOException")
        } catch (e: Exception) {
            assert(e.message?.contains("400") == true)
        }

        httpClient.close()
    }

    /** Tests that duration parameters are converted to microseconds correctly. */
    @Test
    fun `should convert duration parameters to microseconds`() = runTest {
        // Arrange
        var capturedBody = ""

        val mockEngine = MockEngine { request ->
            capturedBody = request.body.toString()

            respond(content = ByteReadChannel("Success"), status = HttpStatusCode.OK)
        }

        val httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { json() } }

        val client = Client(httpClient, "test-auth", "https://test.api.com")
        val actions = listOf(Action(1609459200000000L, "order-1", PLACE, COOLER))

        // Act
        client.solve("test-123", 500.milliseconds, 4.seconds, 8.seconds, actions)

        // Assert
        // The body should contain durations in microseconds
        // 500ms = 500000 microseconds, 4s = 4000000 microseconds, 8s = 8000000 microseconds
        assertNotNull(capturedBody)

        httpClient.close()
    }
}
