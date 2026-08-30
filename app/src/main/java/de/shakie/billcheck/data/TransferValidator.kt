package de.shakie.billcheck.data

import de.shakie.billcheck.domain.CurrencyCatalog
import de.shakie.billcheck.domain.MoneyCalculator
import java.util.Locale

internal object TransferValidator {
    private val supportedCurrencies by lazy {
        CurrencyCatalog.entries(Locale.ROOT).mapTo(hashSetOf()) { it.code }
    }

    fun validate(source: TransferTrip) {
        require(source.name.length <= 500) { "Trip name is too long" }
        require(validCurrency(source.homeCurrencyCode)) { "Invalid home currency" }
        require(source.currencies.isNotEmpty()) { "Trip has no currencies" }
        require(source.currencies.map { it.currencyCode }.distinct().size == source.currencies.size) {
            "Duplicate trip currencies"
        }
        require(source.currencies.count { it.isDefault } == 1) { "Trip needs one default currency" }
        require(source.currencies.any {
            it.currencyCode == source.homeCurrencyCode &&
                it.homeToCurrencyRate.toBigDecimalOrNull()?.compareTo(java.math.BigDecimal.ONE) == 0 &&
                it.exchangeRateMode == "FIXED"
        }) { "Home currency with rate 1 is missing" }
        source.currencies.forEach { currency ->
            require(validCurrency(currency.currencyCode)) { "Invalid trip currency" }
            require(currency.homeToCurrencyRate.toBigDecimalOrNull()?.signum() == 1) { "Invalid exchange rate" }
            require(currency.exchangeRateMode == "FIXED" || currency.exchangeRateMode == "DAILY") {
                "Invalid exchange-rate mode"
            }
        }
        val configuredCodes = source.currencies.mapTo(hashSetOf()) { it.currencyCode }
        require(source.defaultTipMinor >= 0) { "Default tip must not be negative" }
        require(validCurrency(source.defaultTipCurrencyCode)) { "Invalid tip currency" }
        require(source.defaultTipCurrencyCode in configuredCodes) { "Tip currency is not configured" }
        require(source.receipts.map { it.id }.distinct().size == source.receipts.size) { "Duplicate receipt IDs" }
        source.receipts.forEach { receipt ->
            require(receipt.amountMinor > 0) { "Receipt amount must be positive" }
            require(receipt.currencyCode in configuredCodes) { "Receipt currency is not configured" }
            require(receipt.tipCurrencyCode in configuredCodes) { "Receipt tip currency is not configured" }
            require(receipt.exchangeRateSnapshot.toBigDecimalOrNull()?.signum() == 1) { "Invalid receipt exchange rate" }
            require(receipt.tipExchangeRateSnapshot.toBigDecimalOrNull()?.signum() == 1) { "Invalid tip exchange rate" }
            if (receipt.currencyCode == source.homeCurrencyCode) {
                require(receipt.exchangeRateSnapshot.toBigDecimalOrNull()?.compareTo(java.math.BigDecimal.ONE) == 0) {
                    "Home-currency receipt rate must be 1"
                }
            }
            if (receipt.tipCurrencyCode == source.homeCurrencyCode) {
                require(receipt.tipExchangeRateSnapshot.toBigDecimalOrNull()?.compareTo(java.math.BigDecimal.ONE) == 0) {
                    "Home-currency tip rate must be 1"
                }
            }
            require(receipt.tipMinor >= 0 && receipt.exactHomeMinor >= 0) { "Invalid receipt calculated amount" }
            if (receipt.tipMinor == 0L) {
                require(
                    receipt.tipCurrencyCode == source.homeCurrencyCode &&
                        receipt.tipExchangeRateSnapshot.toBigDecimalOrNull()
                            ?.compareTo(java.math.BigDecimal.ONE) == 0,
                ) { "A zero tip must use home currency and rate 1" }
            }
            require(
                receipt.exactHomeMinor == MoneyCalculator.calculateExactHomeMinor(
                    amountMinor = receipt.amountMinor,
                    currencyCode = receipt.currencyCode,
                    exchangeRateSnapshot = receipt.exchangeRateSnapshot,
                    tipMinor = receipt.tipMinor,
                    tipCurrencyCode = receipt.tipCurrencyCode,
                    tipExchangeRateSnapshot = receipt.tipExchangeRateSnapshot,
                    homeCurrencyCode = source.homeCurrencyCode,
                ),
            ) { "Receipt home amount does not match its snapshots" }
            require(receipt.location.length <= 2_000 && receipt.checkNumber.length <= 500) {
                "Receipt text is too long"
            }
            receipt.items.forEach { item ->
                require(item.amountMinor >= 0 && item.currencyCode == receipt.currencyCode) {
                    "Invalid receipt item"
                }
            }
        }
        require(source.reconciliations.map { it.id }.distinct().size == source.reconciliations.size) {
            "Duplicate reconciliation IDs"
        }
        val allStatementLineIds = source.reconciliations.flatMap { reconciliation ->
            reconciliation.lines.map { it.id }
        }
        require(allStatementLineIds.distinct().size == allStatementLineIds.size) {
            "Duplicate statement line IDs"
        }
        source.reconciliations.forEach { reconciliation ->
            require(reconciliation.title.length <= 500) { "Statement title is too long" }
            require(reconciliation.declaredTotalMinor == null || reconciliation.declaredTotalMinor > 0) {
                "Declared statement total must be positive"
            }
            require(
                reconciliation.declaredTotalCurrencyCode == null ||
                    validCurrency(reconciliation.declaredTotalCurrencyCode),
            ) { "Invalid declared statement currency" }
            require(
                (reconciliation.declaredTotalMinor == null) ==
                    (reconciliation.declaredTotalCurrencyCode == null),
            ) {
                "Declared statement total and currency must be provided together"
            }
            val matchedIds = reconciliation.lines.mapNotNull { it.matchedReceiptId }
            require(matchedIds.distinct().size == matchedIds.size) {
                "A receipt is assigned more than once in the same reconciliation"
            }
            reconciliation.lines.forEach { line ->
                require(line.amountMinor > 0) { "Statement line amount must be positive" }
                require(validCurrency(line.currencyCode)) { "Invalid statement line currency" }
                require(line.description.length <= 2_000 && line.checkNumber.length <= 500) {
                    "Statement line text is too long"
                }
                require(line.matchedReceiptId == null || source.receipts.any { it.id == line.matchedReceiptId }) {
                    "Statement match references an unknown receipt"
                }
            }
        }
    }

    private fun validCurrency(value: String): Boolean = value in supportedCurrencies
}
