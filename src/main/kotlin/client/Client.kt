package com.css.challenge.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Client for fetching and solving Cloud Kitchens challenge test problems.
 *
 * This client handles all communication with the Cloud Kitchens API, including fetching new test
 * problems and submitting solutions for evaluation.
 *
 * @property client The HTTP client used for making requests
 * @property auth Authentication token for API access
 * @property endpoint Base URL of the Cloud Kitchens API
 */
class Client(
        private val client: HttpClient,
        private val auth: String,
        private val endpoint: String
) {
    /**
     * Fetches a new test problem from the server.
     *
     * The generated URL also works in a browser for convenience. The server returns a test ID in
     * the response headers and a list of orders in the body.
     *
     * @param name Optional problem name for identification
     * @param seed Optional seed for deterministic problem generation; uses random seed if null
     * @return Problem object containing test ID and list of orders
     * @throws IOException if the HTTP request fails or returns a non-OK status
     */
    suspend fun newProblem(name: String?, seed: Long?): Problem {
        val url =
                "${endpoint}/interview/challenge/new?auth=${auth}&name=${name.orEmpty()}&seed=${seed ?: Random.nextLong()}"
        val resp = client.get(url)
        if (resp.status != HttpStatusCode.OK) {
            throw IOException("$url: ${resp.status}")
        }
        val id = resp.headers["x-test-id"].orEmpty()

        println("Fetched new test problem, id=$id: $url")
        return Problem(id, resp.body<String>().let { Json.decodeFromString<List<Order>>(it) })
    }

    /**
     * Configuration options for the solution submission.
     *
     * @property rate Order processing rate in microseconds
     * @property min Minimum pickup time in microseconds
     * @property max Maximum pickup time in microseconds
     */
    @Serializable
    private data class Options(
            val rate: Long,
            val min: Long,
            val max: Long,
    )

    /**
     * Complete solution submission containing options and actions.
     *
     * @property options Configuration parameters used for the solution
     * @property actions Sequence of actions performed to solve the problem
     */
    @Serializable
    private data class Solution(
            val options: Options,
            val actions: List<Action>,
    )

    /**
     * Submits a sequence of actions and parameters as a solution to a test problem.
     *
     * Converts duration parameters to microseconds and sends them along with the action sequence to
     * the server for evaluation.
     *
     * @param testId Unique identifier of the test problem
     * @param rate Order processing rate
     * @param min Minimum pickup time
     * @param max Maximum pickup time
     * @param actions Sequence of actions that form the solution
     * @return Test result message from the server
     * @throws IOException if the HTTP request fails or returns a non-OK status
     */
    suspend fun solve(
            testId: String,
            rate: Duration,
            min: Duration,
            max: Duration,
            actions: List<Action>
    ): String {
        val options =
                Options(
                        rate.toLong(DurationUnit.MICROSECONDS),
                        min.toLong(DurationUnit.MICROSECONDS),
                        max.toLong(DurationUnit.MICROSECONDS)
                )
        val solution = Solution(options, actions)

        val url = "${endpoint}/interview/challenge/solve?auth=${auth}"
        val resp =
                client.post(url) {
                    contentType(ContentType.Application.Json)
                    headers { append("x-test-id", testId) }
                    setBody(Json.encodeToString(solution))
                }
        if (resp.status != HttpStatusCode.OK) {
            throw IOException("$url: ${resp.status}")
        }

        return resp.bodyAsText()
    }
}
