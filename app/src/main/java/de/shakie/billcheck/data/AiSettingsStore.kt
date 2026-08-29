package de.shakie.billcheck.data

import android.content.Context

data class AiSettings(
    val providerId: String = AI_PROVIDER_GEMINI,
    val model: String = "gemini-3.6-flash",
    val hasApiKey: Boolean = false,
)

class AiSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val apiKeyStore = ApiKeyStore(context)

    fun read(): AiSettings = AiSettings(
        providerId = preferences.getString(PROVIDER, AI_PROVIDER_GEMINI)
            ?.takeIf { it == AI_PROVIDER_GEMINI || it == AI_PROVIDER_LOCAL }
            ?: AI_PROVIDER_GEMINI,
        model = preferences.getString(MODEL, "gemini-3.6-flash") ?: "gemini-3.6-flash",
        hasApiKey = apiKeyStore.hasGeminiApiKey(),
    )

    fun save(providerId: String, apiKey: String?, model: String) {
        require(providerId == AI_PROVIDER_GEMINI || providerId == AI_PROVIDER_LOCAL) {
            "Unknown AI provider"
        }
        apiKey?.let(apiKeyStore::setGeminiApiKey)
        preferences.edit()
            .putString(PROVIDER, providerId)
            .putString(MODEL, model.trim().ifBlank { "gemini-3.6-flash" })
            .apply()
    }

    fun saveGemini(apiKey: String?, model: String) = save(AI_PROVIDER_GEMINI, apiKey, model)

    fun apiKey(): String? = apiKeyStore.getGeminiApiKey()

    fun clearApiKey() = apiKeyStore.clearGeminiApiKey()

    private companion object {
        const val PREFERENCES = "ai_settings"
        const val PROVIDER = "provider"
        const val MODEL = "model"
    }
}

const val AI_PROVIDER_GEMINI = "gemini"
const val AI_PROVIDER_LOCAL = "local_openai"
