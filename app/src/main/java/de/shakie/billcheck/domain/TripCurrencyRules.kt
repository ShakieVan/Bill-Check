package de.shakie.billcheck.domain

import de.shakie.billcheck.data.TripCurrencyEntity
import java.math.BigDecimal
import java.util.Locale

object TripCurrencyRules {
    fun requireValid(homeCurrencyCode: String, currencies: List<TripCurrencyEntity>) {
        val home = CurrencyAmount.normalizeCode(homeCurrencyCode)
        require(homeCurrencyCode == home) { "Home currency code must be normalized" }
        require(home in supportedCurrencyCodes) { "Unsupported home currency" }
        require(currencies.isNotEmpty()) { "A trip needs at least its home currency" }
        require(currencies.map { CurrencyAmount.normalizeCode(it.currencyCode) }.distinct().size == currencies.size) {
            "Trip currencies must be unique"
        }
        require(currencies.count(TripCurrencyEntity::isDefault) == 1) {
            "A trip needs exactly one default currency"
        }
        val homeEntry = currencies.singleOrNull {
            CurrencyAmount.normalizeCode(it.currencyCode) == home
        } ?: throw IllegalArgumentException("The home currency must be part of the trip")
        require(homeEntry.homeToCurrencyRate.toBigDecimalOrNull()?.compareTo(BigDecimal.ONE) == 0) {
            "The home currency rate must be 1"
        }
        require(homeEntry.exchangeRateMode == "FIXED") {
            "The home currency exchange-rate mode must be FIXED"
        }
        currencies.forEach {
            val normalized = CurrencyAmount.normalizeCode(it.currencyCode)
            require(it.currencyCode == normalized) { "Trip currency code must be normalized" }
            require(normalized in supportedCurrencyCodes) {
                "Unsupported trip currency"
            }
            require(it.homeToCurrencyRate.toBigDecimalOrNull()?.signum() == 1) {
                "Exchange rates must be positive"
            }
            require(it.exchangeRateMode == "FIXED" || it.exchangeRateMode == "DAILY") {
                "Unsupported exchange-rate mode"
            }
        }
    }

    private val supportedCurrencyCodes: Set<String> by lazy {
        CurrencyCatalog.entries(Locale.ROOT).mapTo(hashSetOf()) { it.code }
    }
}
