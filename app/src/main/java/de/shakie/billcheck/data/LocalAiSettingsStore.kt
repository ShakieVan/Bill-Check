package de.shakie.billcheck.data

import android.content.Context

enum class LocalAiAuthType {
    BASIC,
    BEARER,
}

data class LocalAiSettings(
    val baseUrl: String = DEFAULT_LOCAL_AI_BASE_URL,
    val model: String = DEFAULT_LOCAL_AI_MODEL,
    val authType: LocalAiAuthType = LocalAiAuthType.BASIC,
    val username: String = DEFAULT_LOCAL_AI_USERNAME,
    val hasCredential: Boolean = false,
)

class LocalAiSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val credentialStore = ApiKeyStore(context)

    fun read(): LocalAiSettings = LocalAiSettings(
        baseUrl = preferences.getString(BASE_URL, DEFAULT_LOCAL_AI_BASE_URL)
            ?: DEFAULT_LOCAL_AI_BASE_URL,
        model = preferences.getString(MODEL, DEFAULT_LOCAL_AI_MODEL) ?: DEFAULT_LOCAL_AI_MODEL,
        authType = runCatching {
            LocalAiAuthType.valueOf(
                preferences.getString(AUTH_TYPE, LocalAiAuthType.BASIC.name)
                    ?: LocalAiAuthType.BASIC.name,
            )
        }.getOrDefault(LocalAiAuthType.BASIC),
        username = preferences.getString(USERNAME, DEFAULT_LOCAL_AI_USERNAME)
            ?: DEFAULT_LOCAL_AI_USERNAME,
        hasCredential = credentialStore.hasLocalAiCredential(),
    )

    fun save(settings: LocalAiSettings, newCredential: String?) {
        newCredential?.takeIf(String::isNotBlank)?.let(credentialStore::setLocalAiCredential)
        preferences.edit()
            .putString(BASE_URL, settings.baseUrl.trim().trimEnd('/'))
            .putString(MODEL, settings.model.trim())
            .putString(AUTH_TYPE, settings.authType.name)
            .putString(USERNAME, settings.username.trim())
            .apply()
    }

    fun credential(): String? = credentialStore.getLocalAiCredential()

    fun clearCredential() = credentialStore.clearLocalAiCredential()

    private companion object {
        const val PREFERENCES = "local_ai_settings"
        const val BASE_URL = "base_url"
        const val MODEL = "model"
        const val AUTH_TYPE = "auth_type"
        const val USERNAME = "username"
    }
}

const val DEFAULT_LOCAL_AI_BASE_URL = "https://ai.replinator.de/v1"
const val DEFAULT_LOCAL_AI_MODEL = "qwen3.8-27b-q8"
const val DEFAULT_LOCAL_AI_USERNAME = "lmstudio"
