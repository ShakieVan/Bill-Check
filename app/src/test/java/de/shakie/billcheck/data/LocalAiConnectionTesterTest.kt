package de.shakie.billcheck.data

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAiConnectionTesterTest {
    @Test
    fun basicAuthenticationLoadsConfiguredModel() = runTest {
        val connection = FakeHttpConnection(
            URL("https://ai.replinator.de/v1/models"),
            200,
            """{"data":[{"id":"qwen3.8-27b-q8"}]}""",
        )
        val tester = LocalAiConnectionTester { connection }

        val result = tester.test(
            LocalAiSettings(
                baseUrl = "https://ai.replinator.de/v1/",
                model = "qwen3.8-27b-q8",
                authType = LocalAiAuthType.BASIC,
                username = "lmstudio",
            ),
            "secret",
        )

        assertTrue(result.configuredModelAvailable)
        assertEquals(listOf("qwen3.8-27b-q8"), result.availableModels)
        assertEquals("Basic bG1zdHVkaW86c2VjcmV0", connection.requestHeaders["Authorization"])
        assertTrue(connection.disconnected)
    }

    @Test
    fun bearerAuthenticationReportsMissingConfiguredModel() = runTest {
        val connection = FakeHttpConnection(
            URL("https://ai.replinator.de/v1/models"),
            200,
            """{"data":[{"id":"another-model"}]}""",
        )
        val tester = LocalAiConnectionTester { connection }

        val result = tester.test(
            LocalAiSettings(authType = LocalAiAuthType.BEARER),
            "token-value",
        )

        assertFalse(result.configuredModelAvailable)
        assertEquals("Bearer token-value", connection.requestHeaders["Authorization"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnencryptedRemoteServerUrl() {
        normalizeLocalAiBaseUrl("http://ai.replinator.de/v1")
    }
}

private class FakeHttpConnection(
    url: URL,
    private val status: Int,
    private val responseBody: String,
) : HttpURLConnection(url) {
    val requestHeaders = mutableMapOf<String, String>()
    var disconnected = false

    override fun setRequestProperty(key: String, value: String) {
        requestHeaders[key] = value
    }

    override fun getResponseCode(): Int = status

    override fun getInputStream(): InputStream =
        ByteArrayInputStream(responseBody.toByteArray(Charsets.UTF_8))

    override fun getErrorStream(): InputStream? = null

    override fun disconnect() {
        disconnected = true
    }

    override fun usingProxy(): Boolean = false

    override fun connect() = Unit
}
