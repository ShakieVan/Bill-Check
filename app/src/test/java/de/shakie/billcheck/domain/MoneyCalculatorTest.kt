package de.shakie.billcheck.domain

import de.shakie.billcheck.data.ReceiptEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyCalculatorTest {
    @Test
    fun `receipt is exact to cents and overview rounds upward`() {
        val receipt = receipt(foreignMinor = 31_638, rate = "55.5")

        assertEquals(570, MoneyCalculator.exactEuroCents(receipt))
        assertEquals(6, MoneyCalculator.roundedUpEuro(570))
    }

    @Test
    fun `euro tip is added before upward overview rounding`() {
        val receipt = receipt(foreignMinor = 31_638, rate = "55.5", tipMinor = 100)

        assertEquals(670, MoneyCalculator.exactEuroCents(receipt))
        assertEquals(7, MoneyCalculator.roundedUpEuro(670))
    }

    @Test
    fun `trip rounds exact sum instead of summing rounded receipts`() {
        val receipts = listOf(
            receipt(foreignMinor = 28_860, rate = "55.5"),
            receipt(foreignMinor = 28_860, rate = "55.5"),
        )

        assertEquals(1_040, MoneyCalculator.exactTripEuroCents(receipts))
        assertEquals(11, MoneyCalculator.roundedUpTripEuro(receipts))
    }

    private fun receipt(
        foreignMinor: Long,
        rate: String,
        tipMinor: Long = 0,
    ) = ReceiptEntity(
        id = "receipt-$foreignMinor-$tipMinor",
        tripId = "trip",
        occurredAt = 0,
        location = "",
        checkNumber = "",
        foreignAmountMinor = foreignMinor,
        foreignCurrencyCode = "EGP",
        exchangeRate = rate,
        exactEuroCents = MoneyCalculator.calculateExactEuroCents(
            foreignAmountMinor = foreignMinor,
            exchangeRate = rate,
            tipMinor = tipMinor,
            tipCurrencyCode = "EUR",
        ),
        tipMinor = tipMinor,
        tipCurrencyCode = "EUR",
        imageUri = null,
        reviewState = "CONFIRMED",
        createdAt = 0,
    )
}
