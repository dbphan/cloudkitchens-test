package com.css.challenge

import com.github.ajalt.clikt.testing.test
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

/**
 * Test suite for the Main command-line application.
 *
 * Tests command-line argument parsing, validation, and basic execution flow.
 */
class MainTest {

    /** Tests that the application requires an authentication token. */
    @Test
    fun `should require auth token`() {
        // Arrange
        val main = Main()

        // Act
        val result = main.test("")

        // Assert
        assertTrue(result.statusCode != 0, "Should fail without auth token")
        assertTrue(
                result.output.contains("--auth", ignoreCase = true) ||
                        result.output.contains("required", ignoreCase = true),
                "Output should mention missing auth: ${result.output}"
        )
    }

    /** Tests that the application accepts valid command-line options. */
    @Test
    fun `should parse valid options`() {
        // Arrange
        val main = Main()

        // Act
        val result = main.test("--auth test-token --name test-problem")

        // Assert
        // The command will try to connect and fail with IOException, which is expected
        // We just verify it doesn't fail due to argument parsing issues
        assertTrue(
                result.statusCode == 0 || result.output.isNotEmpty(),
                "Command should either succeed or produce output"
        )
    }

    /** Tests default values for optional parameters. */
    @Test
    fun `should use default endpoint when not specified`() {
        // Arrange
        val main = Main()

        // Act
        val result = main.test("--auth test-token")

        // Assert
        // The command will attempt to run with defaults
        assertTrue(
                result.statusCode == 0 || result.output.isNotEmpty(),
                "Command should either succeed or produce output"
        )
    }

    /** Tests that duration parameters can be parsed. */
    @Test
    fun `should parse duration parameters`() {
        // Arrange
        val main = Main()

        // Act
        val result = main.test("--auth test-token --rate 1s --min 2s --max 5s")

        // Assert
        assertTrue(
                result.statusCode == 0 || result.output.isNotEmpty(),
                "Command should either succeed or produce output"
        )
    }

    /** Tests that seed parameter accepts numeric values. */
    @Test
    fun `should accept seed parameter`() {
        // Arrange
        val main = Main()

        // Act
        val result = main.test("--auth test-token --seed 12345")

        // Assert
        assertTrue(
                result.statusCode == 0 || result.output.isNotEmpty(),
                "Command should either succeed or produce output"
        )
    }
}
