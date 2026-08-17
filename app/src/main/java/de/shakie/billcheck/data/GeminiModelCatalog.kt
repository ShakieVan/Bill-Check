package de.shakie.billcheck.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class GeminiModelInfo(
    val id: String,
    val displayName: String,
    val inputTokenLimit: Int,
    val outputTokenLimit: Int,
)

class GeminiModelCatalog {
    suspend fun list(apiKey: String): List<GeminiModelInfo> = withContext(Dispatchers.IO) {
        val connection = URL("$ENDPOINT?pageSize=1000").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)
            val code = connection.responseCode
            val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(code in 200..299) { "Gemini HTTP $code" }
            val models = JSONObject(response).optJSONArray("models") ?: return@withContext emptyList()
            (0 until models.length()).mapNotNull { index ->
                val model = models.getJSONObject(index)
                val methods = model.optJSONArray("supportedGenerationMethods")
                val supportsGenerate = methods != null && (0 until methods.length()).any {
                    methods.optString(it) == "generateContent"
                }
                val id = model.optString("name").removePrefix("models/")
                if (!supportsGenerate || !isSuitableVisionModel(id)) return@mapNotNull null
                GeminiModelInfo(
                    id = id,
                    displayName = model.optString("displayName", id),
                    inputTokenLimit = model.optInt("inputTokenLimit"),
                    outputTokenLimit = model.optInt("outputTokenLimit"),
                )
            }.distinctBy { it.id }.sortedWith(
                compareByDescending<GeminiModelInfo> { stablePreference(it.id) }
                    .thenBy { it.displayName },
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun isSuitableVisionModel(id: String): Boolean {
        val value = id.lowercase()
        return value.startsWith("gemini-") &&
            ("flash" in value || "pro" in value) &&
            listOf("embedding", "image", "live", "tts", "audio").none(value::contains)
    }

    private fun stablePreference(id: String): Int = when {
        "preview" in id || "exp" in id || "latest" in id -> 0
        else -> 1
    }

    private companion object {
        const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
    }
}
