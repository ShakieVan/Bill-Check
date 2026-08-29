package de.shakie.billcheck.data

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Base64
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class LocalAiConnectionResult(
    val elapsedMilliseconds: Long,
    val availableModels: List<String>,
    val configuredModelAvailable: Boolean,
)

class LocalAiConnectionTester(
    private val openConnection: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    },
) {
    suspend fun test(settings: LocalAiSettings, credential: String): LocalAiConnectionResult =
        withContext(Dispatchers.IO) {
            val baseUrl = normalizeLocalAiBaseUrl(settings.baseUrl)
            require(settings.model.isNotBlank()) { "Model name is missing" }
            require(credential.isNotBlank()) { "Access credential is missing" }
            if (settings.authType == LocalAiAuthType.BASIC) {
                require(settings.username.isNotBlank()) { "Username is missing" }
            }

            val connection = openConnection(URL("$baseUrl/models"))
            val started = TimeSource.Monotonic.markNow()
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Authorization", authorizationHeader(settings, credential))
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                check(code in 200..299) { "HTTP $code" }
                val data = JSONObject(body).optJSONArray("data")
                    ?: error("Server response contains no model list")
                val models = (0 until data.length()).mapNotNull { index ->
                    data.optJSONObject(index)?.optString("id")?.takeIf(String::isNotBlank)
                }
                LocalAiConnectionResult(
                    elapsedMilliseconds = started.elapsedNow().inWholeMilliseconds,
                    availableModels = models,
                    configuredModelAvailable = settings.model in models,
                )
            } finally {
                connection.disconnect()
            }
        }

    internal fun authorizationHeader(settings: LocalAiSettings, credential: String): String =
        localAiAuthorizationHeader(settings, credential)
}

internal fun localAiAuthorizationHeader(settings: LocalAiSettings, credential: String): String =
    when (settings.authType) {
        LocalAiAuthType.BASIC -> {
            val value = "${settings.username}:$credential".toByteArray(Charsets.UTF_8)
            "Basic ${Base64.getEncoder().encodeToString(value)}"
        }
        LocalAiAuthType.BEARER -> "Bearer $credential"
    }

internal fun normalizeLocalAiBaseUrl(value: String): String {
    val normalized = value.trim().trimEnd('/')
    val uri = runCatching { URI(normalized) }.getOrNull()
        ?: throw IllegalArgumentException("Invalid server URL")
    require(uri.scheme.equals("https", ignoreCase = true)) { "Server URL must use HTTPS" }
    require(!uri.host.isNullOrBlank()) { "Server URL has no host" }
    require(uri.rawQuery == null && uri.rawFragment == null) { "Server URL must not contain query or fragment" }
    return normalized
}
