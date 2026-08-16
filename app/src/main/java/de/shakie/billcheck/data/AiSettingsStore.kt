package de.shakie.billcheck.data

import android.content.Context

data class AiSettings(
    val providerId: String = "gemini",
    val model: String = "gemini-3.6-flash",
    val hasApiKey: Boolean = false,
)

class AiSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val apiKeyStore = ApiKeyStore(context)

    fun read(): AiSettings = AiSettings(
        providerId = preferences.getString(PROVIDER, "gemini") ?: "gemini",
        model = preferences.getString(MODEL, "gemini-3.6-flash") ?: "gemini-3.6-flash",
        hasApiKey = apiKeyStore.hasGeminiApiKey(),
    )

    fun saveGemini(apiKey: String?, model: String) {
        apiKey?.let(apiKeyStore::setGeminiApiKey)
        preferences.edit()
            .putString(PROVIDER, "gemini")
            .putString(MODEL, model.trim().ifBlank { "gemini-3.6-flash" })
            .apply()
    }

    fun apiKey(): String? = apiKeyStore.getGeminiApiKey()

    fun clearApiKey() = apiKeyStore.clearGeminiApiKey()

    private companion object {
        const val PREFERENCES = "ai_settings"
        const val PROVIDER = "provider"
        const val MODEL = "model"
    }
}
