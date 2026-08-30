package de.shakie.billcheck.domain

import de.shakie.billcheck.data.ReceiptEntity
import java.math.BigDecimal
import java.math.RoundingMode

object MoneyCalculator {
    /** Converts a minor-unit amount using `1 home = x source`. */
    fun toHomeMinor(
        sourceMinor: Long,
        sourceCurrencyCode: String,
        homeCurrencyCode: String,
        sourceUnitsPerHome: String,
    ): Long {
        val source = CurrencyAmount.normalizeCode(sourceCurrencyCode)
        val home = CurrencyAmount.normalizeCode(homeCurrencyCode)
        if (source == home) return sourceMinor
        val rate = sourceUnitsPerHome.toBigDecimalOrNull()
            ?.takeIf { it.signum() == 1 }
            ?: throw IllegalArgumentException("Exchange rate must be positive")

        // sourceMinor * 10^homeDigits / (rate * 10^sourceDigits), HALF_UP
        return BigDecimal.valueOf(sourceMinor)
            .multiply(CurrencyAmount.minorFactor(home))
            .divide(
                rate.multiply(CurrencyAmount.minorFactor(source)),
                0,
                RoundingMode.HALF_UP,
            )
            .longValueExact()
    }

    fun calculateExactHomeMinor(
        amountMinor: Long,
        currencyCode: String,
        exchangeRateSnapshot: String,
        tipMinor: Long,
        tipCurrencyCode: String,
        tipExchangeRateSnapshot: String,
        homeCurrencyCode: String,
    ): Long = Math.addExact(
        toHomeMinor(amountMinor, currencyCode, homeCurrencyCode, exchangeRateSnapshot),
        toHomeMinor(tipMinor, tipCurrencyCode, homeCurrencyCode, tipExchangeRateSnapshot),
    )

    fun exactHomeMinor(receipt: ReceiptEntity): Long = receipt.exactHomeMinor

    fun exactTripHomeMinor(receipts: List<ReceiptEntity>): Long =
        receipts.fold(0L) { total, receipt -> Math.addExact(total, receipt.exactHomeMinor) }

    fun roundedUpHomeMajor(exactHomeMinor: Long, homeCurrencyCode: String): Long {
        if (exactHomeMinor <= 0) return 0
        val factor = CurrencyAmount.minorFactor(homeCurrencyCode).longValueExact()
        return Math.addExact(exactHomeMinor, factor - 1) / factor
    }

    fun roundedUpTripHomeMajor(
        receipts: List<ReceiptEntity>,
        homeCurrencyCode: String,
    ): Long = roundedUpHomeMajor(exactTripHomeMinor(receipts), homeCurrencyCode)
}
