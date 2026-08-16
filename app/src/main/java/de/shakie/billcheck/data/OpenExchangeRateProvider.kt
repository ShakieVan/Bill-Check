package de.shakie.billcheck.data

import android.content.Context
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

    override suspend fun latestForeignPerEuro(targetCurrencyCode: String): ExchangeRateQuote {
        val target = targetCurrencyCode.trim().uppercase(Locale.ROOT)
        require(target.length == 3) { "Invalid target currency" }
        if (target == "EUR") {
            return ExchangeRateQuote(
                targetCurrencyCode = target,
                foreignPerEuro = "1",
                updatedAt = System.currentTimeMillis(),
                nextUpdateAt = Long.MAX_VALUE,
                fromCache = true,
            )
        }

        val cached = cachedQuote(target)
        if (cached != null && cached.nextUpdateAt > System.currentTimeMillis()) return cached
        return runCatching { fetch(target).also(::cache) }
            .getOrElse { cached ?: throw it }
    }

    private fun cachedQuote(target: String): ExchangeRateQuote? {
        val rate = preferences.getString("rate_$target", null) ?: return null
        val updatedAt = preferences.getLong("updated_$target", 0)
        val nextUpdateAt = preferences.getLong("next_$target", 0)
        return ExchangeRateQuote(target, rate, updatedAt, nextUpdateAt, fromCache = true)
    }

    private suspend fun fetch(target: String): ExchangeRateQuote = withContext(Dispatchers.IO) {
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Bill-Check-Android")
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "Exchange-rate service returned HTTP ${connection.responseCode}"
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            check(json.getString("result") == "success") { "Exchange-rate response failed" }
            check(json.getString("base_code") == "EUR") { "Unexpected base currency" }
            val rate = json.getJSONObject("rates").get(target).toString()
            check(rate.toBigDecimalOrNull()?.signum() == 1) { "Invalid exchange rate" }
            ExchangeRateQuote(
                targetCurrencyCode = target,
                foreignPerEuro = rate,
                updatedAt = json.getLong("time_last_update_unix") * 1_000,
                nextUpdateAt = json.getLong("time_next_update_unix") * 1_000,
                fromCache = false,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun cache(quote: ExchangeRateQuote) {
        preferences.edit()
            .putString("rate_${quote.targetCurrencyCode}", quote.foreignPerEuro)
            .putLong("updated_${quote.targetCurrencyCode}", quote.updatedAt)
            .putLong("next_${quote.targetCurrencyCode}", quote.nextUpdateAt)
            .apply()
    }

    private companion object {
        const val PREFERENCES = "exchange_rate_cache"
        const val ENDPOINT = "https://open.er-api.com/v6/latest/EUR"
    }
}
