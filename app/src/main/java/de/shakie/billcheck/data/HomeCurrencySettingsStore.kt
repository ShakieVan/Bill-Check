package de.shakie.billcheck.data

import android.content.Context
import de.shakie.billcheck.domain.CurrencyCatalog
import java.util.Locale

/**
 * Stores the default reporting currency for newly created trips.
 *
 * Trips keep their own snapshot, so changing this setting never changes historic totals.
 */
class HomeCurrencySettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun read(): String = preferences.getString(HOME_CURRENCY, DEFAULT_HOME_CURRENCY)
        ?.normalizeCurrencyCode()
        ?: DEFAULT_HOME_CURRENCY

    fun save(currencyCode: String) {
        val normalized = requireNotNull(currencyCode.normalizeCurrencyCode()) {
            "Unknown currency code"
        }
        preferences.edit()
            .putString(HOME_CURRENCY, normalized)
            .putString(RECENT_CURRENCIES, updatedRecentCurrencies(normalized).joinToString(","))
            .apply()
    }

    fun recentCurrencyCodes(): List<String> = preferences
        .getString(RECENT_CURRENCIES, "")
        .orEmpty()
        .split(',')
        .mapNotNull { it.normalizeCurrencyCode() }
        .distinct()
        .take(MAX_RECENT_CURRENCIES)

    fun recordUsed(currencyCode: String) {
        val normalized = requireNotNull(currencyCode.normalizeCurrencyCode()) {
            "Unknown currency code"
        }
        preferences.edit()
            .putString(RECENT_CURRENCIES, updatedRecentCurrencies(normalized).joinToString(","))
            .apply()
    }

    private fun updatedRecentCurrencies(code: String): List<String> =
        (listOf(code) + recentCurrencyCodes()).distinct().take(MAX_RECENT_CURRENCIES)

    private fun String.normalizeCurrencyCode(): String? {
        val normalized = trim().uppercase(Locale.ROOT)
        return normalized.takeIf { it in supportedCurrencyCodes }
    }

    private companion object {
        const val PREFERENCES = "currency_settings"
        const val HOME_CURRENCY = "home_currency"
        const val RECENT_CURRENCIES = "recent_currencies"
        const val DEFAULT_HOME_CURRENCY = "EUR"
        const val MAX_RECENT_CURRENCIES = 8
        val supportedCurrencyCodes by lazy {
            CurrencyCatalog.entries(Locale.ROOT).mapTo(hashSetOf()) { it.code }
        }
    }
}
