package de.shakie.billcheck.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun hasGeminiApiKey(): Boolean = hasCredential(GEMINI_CIPHERTEXT, GEMINI_IV)

    fun getGeminiApiKey(): String? = getCredential(GEMINI_CIPHERTEXT, GEMINI_IV, GEMINI_ALIAS)

    fun setGeminiApiKey(value: String) = setCredential(
        value = value,
        ciphertextKey = GEMINI_CIPHERTEXT,
        ivKey = GEMINI_IV,
        alias = GEMINI_ALIAS,
    )

    fun clearGeminiApiKey() = clearCredential(GEMINI_CIPHERTEXT, GEMINI_IV)

    fun hasLocalAiCredential(): Boolean = hasCredential(LOCAL_AI_CIPHERTEXT, LOCAL_AI_IV)

    fun getLocalAiCredential(): String? =
        getCredential(LOCAL_AI_CIPHERTEXT, LOCAL_AI_IV, LOCAL_AI_ALIAS)

    fun setLocalAiCredential(value: String) = setCredential(
        value = value,
        ciphertextKey = LOCAL_AI_CIPHERTEXT,
        ivKey = LOCAL_AI_IV,
        alias = LOCAL_AI_ALIAS,
    )

    fun clearLocalAiCredential() = clearCredential(LOCAL_AI_CIPHERTEXT, LOCAL_AI_IV)

    private fun hasCredential(ciphertextKey: String, ivKey: String): Boolean =
        preferences.contains(ciphertextKey) && preferences.contains(ivKey)

    private fun getCredential(ciphertextKey: String, ivKey: String, alias: String): String? {
        val ciphertext = preferences.getString(ciphertextKey, null) ?: return null
        val iv = preferences.getString(ivKey, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(alias),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
            )
            String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun setCredential(value: String, ciphertextKey: String, ivKey: String, alias: String) {
        val normalized = value.trim()
        if (normalized.isEmpty()) {
            clearCredential(ciphertextKey, ivKey)
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(alias))
        val ciphertext = cipher.doFinal(normalized.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(ciphertextKey, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(ivKey, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    private fun clearCredential(ciphertextKey: String, ivKey: String) =
        preferences.edit().remove(ciphertextKey).remove(ivKey).apply()

    private fun getOrCreateKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES = "secure_api_credentials"
        const val GEMINI_CIPHERTEXT = "gemini_ciphertext"
        const val GEMINI_IV = "gemini_iv"
        const val LOCAL_AI_CIPHERTEXT = "local_ai_ciphertext"
        const val LOCAL_AI_IV = "local_ai_iv"
        const val KEYSTORE = "AndroidKeyStore"
        const val GEMINI_ALIAS = "bill_check_gemini_api_key"
        const val LOCAL_AI_ALIAS = "bill_check_local_ai_credential"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
