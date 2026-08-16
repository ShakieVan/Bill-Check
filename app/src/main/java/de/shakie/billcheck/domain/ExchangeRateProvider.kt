package de.shakie.billcheck.domain

data class ExchangeRateQuote(
    val targetCurrencyCode: String,
    val foreignPerEuro: String,
    val updatedAt: Long,
    val nextUpdateAt: Long,
    val fromCache: Boolean,
)

interface ExchangeRateProvider {
    suspend fun latestForeignPerEuro(targetCurrencyCode: String): ExchangeRateQuote
}
