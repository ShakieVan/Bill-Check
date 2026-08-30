package de.shakie.billcheck.data

import android.content.Context
import de.shakie.billcheck.domain.CurrencyAmount
import de.shakie.billcheck.domain.CurrencyCatalog
import de.shakie.billcheck.domain.ExchangeRateProvider
import de.shakie.billcheck.domain.ExchangeRateQuote
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class OpenExchangeRateProvider(context: Context) : ExchangeRateProvider {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override suspend fun latestRate(
        baseCurrencyCode: String,
        targetCurrencyCode: String,
    ): ExchangeRateQuote {
        val base = CurrencyAmount.normalizeCode(baseCurrencyCode).also {
            require(it in SUPPORTED_CURRENCY_CODES) { "Unsupported base currency" }
        }
        val target = CurrencyAmount.normalizeCode(targetCurrencyCode).also {
            require(it in SUPPORTED_CURRENCY_CODES) { "Unsupported target currency" }
        }
        if (base == target) {
            return ExchangeRateQuote(base, target, "1", System.currentTimeMillis(), Long.MAX_VALUE, true)
        }

        val cached = cachedQuote(base, target)
        if (cached != null && cached.nextUpdateAt > System.currentTimeMillis()) return cached
        return runCatching { fetch(base, target).also(::cache) }
            .getOrElse { cached ?: throw it }
    }

    private fun cachedQuote(base: String, target: String): ExchangeRateQuote? {
        val key = cacheKey(base, target)
        val rate = preferences.getString("rate_$key", null) ?: return null
        return ExchangeRateQuote(
            base,
            target,
            rate,
            preferences.getLong("updated_$key", 0),
            preferences.getLong("next_$key", 0),
            fromCache = true,
        )
    }

    private suspend fun fetch(base: String, target: String): ExchangeRateQuote =
        withContext(Dispatchers.IO) {
            val connection = URL("$ENDPOINT_ROOT/$base").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "Bill-Check-Android")
                check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                    "Exchange-rate service returned HTTP ${connection.responseCode}"
                }
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                check(json.getString("result") == "success") { "Exchange-rate response failed" }
                val responseBase = CurrencyAmount.normalizeCode(json.getString("base_code"))
                check(responseBase == base) { "Unexpected base currency: $responseBase" }
                val rate = json.getJSONObject("rates").get(target).toString()
                check(rate.toBigDecimalOrNull()?.signum() == 1) { "Invalid exchange rate" }
                ExchangeRateQuote(
                    base,
                    target,
                    rate,
                    json.getLong("time_last_update_unix") * 1_000,
                    json.getLong("time_next_update_unix") * 1_000,
                    fromCache = false,
                )
            } finally {
                connection.disconnect()
            }
        }

    private fun cache(quote: ExchangeRateQuote) {
        val key = cacheKey(quote.baseCurrencyCode, quote.targetCurrencyCode)
        preferences.edit()
            .putString("rate_$key", quote.targetUnitsPerBase)
            .putLong("updated_$key", quote.updatedAt)
            .putLong("next_$key", quote.nextUpdateAt)
            .apply()
    }

    private fun cacheKey(base: String, target: String) = "${base}_$target"

    private companion object {
        const val PREFERENCES = "exchange_rate_cache_v2"
        const val ENDPOINT_ROOT = "https://open.er-api.com/v6/latest"
        val SUPPORTED_CURRENCY_CODES: Set<String> by lazy {
            CurrencyCatalog.entries(Locale.ROOT).mapTo(hashSetOf()) { it.code }
        }
    }
}
