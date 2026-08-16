package de.shakie.billcheck.domain

import de.shakie.billcheck.data.ReceiptEntity
import java.math.BigDecimal
import java.math.RoundingMode

object MoneyCalculator {
    fun foreignMinorToEuroCents(foreignMinor: Long, foreignPerEuro: String): Long {
        val rate = foreignPerEuro.toBigDecimalOrNull()
            ?.takeIf { it > BigDecimal.ZERO }
            ?: return 0
        return BigDecimal.valueOf(foreignMinor)
            .divide(rate, 0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun calculateExactEuroCents(
        foreignAmountMinor: Long,
        exchangeRate: String,
        tipMinor: Long,
        tipCurrencyCode: String,
    ): Long {
        val receiptCents = foreignMinorToEuroCents(foreignAmountMinor, exchangeRate)
        val tipCents = if (tipCurrencyCode == "EUR") {
            tipMinor
        } else {
            foreignMinorToEuroCents(tipMinor, exchangeRate)
        }
        return receiptCents + tipCents
    }

    fun exactEuroCents(receipt: ReceiptEntity): Long = receipt.exactEuroCents

    fun roundedUpEuro(exactEuroCents: Long): Long = when {
        exactEuroCents <= 0 -> 0
        else -> (exactEuroCents + 99) / 100
    }

    fun exactTripEuroCents(receipts: List<ReceiptEntity>): Long =
        receipts.sumOf(::exactEuroCents)

    fun roundedUpTripEuro(receipts: List<ReceiptEntity>): Long =
        roundedUpEuro(exactTripEuroCents(receipts))
}
