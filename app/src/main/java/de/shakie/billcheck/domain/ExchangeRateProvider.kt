package de.shakie.billcheck.domain

data class ExchangeRateQuote(
    val baseCurrencyCode: String,
    val targetCurrencyCode: String,
    /** Number of target-currency major units for one base-currency major unit. */
    val targetUnitsPerBase: String,
    val updatedAt: Long,
    val nextUpdateAt: Long,
    val fromCache: Boolean,
)

interface ExchangeRateProvider {
    suspend fun latestRate(
        baseCurrencyCode: String,
        targetCurrencyCode: String,
    ): ExchangeRateQuote
}
