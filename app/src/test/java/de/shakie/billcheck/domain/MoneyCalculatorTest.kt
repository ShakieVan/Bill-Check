package de.shakie.billcheck.domain

import de.shakie.billcheck.data.ReceiptEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyCalculatorTest {
    @Test
    fun `two-decimal receipt converts exactly and overview rounds upward`() {
        val receipt = receipt(amountMinor = 31_638, rate = "55.5")

        assertEquals(570, MoneyCalculator.exactHomeMinor(receipt))
        assertEquals(6, MoneyCalculator.roundedUpHomeMajor(570, "EUR"))
    }

    @Test
    fun `conversion accounts for zero and three fraction digits`() {
        assertEquals(500, MoneyCalculator.toHomeMinor(800, "JPY", "EUR", "160"))
        assertEquals(500, MoneyCalculator.toHomeMinor(1_650, "KWD", "EUR", "0.33"))
        assertEquals(500, MoneyCalculator.toHomeMinor(250, "JPY", "KWD", "500"))
    }

    @Test
    fun `tip in third currency uses its own rate snapshot`() {
        assertEquals(
            1_500,
            MoneyCalculator.calculateExactHomeMinor(
                amountMinor = 55_500,
                currencyCode = "EGP",
                exchangeRateSnapshot = "55.5",
                tipMinor = 540,
                tipCurrencyCode = "USD",
                tipExchangeRateSnapshot = "1.08",
                homeCurrencyCode = "EUR",
            ),
        )
    }

    @Test
    fun `same-currency amount ignores rate text`() {
        assertEquals(123, MoneyCalculator.toHomeMinor(123, "eur", "EUR", "not-a-rate"))
    }

    @Test
    fun `invalid foreign rate is rejected instead of silently returning zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            MoneyCalculator.toHomeMinor(100, "USD", "EUR", "0")
        }
    }

    @Test
    fun `trip rounds exact sum instead of summing rounded receipts`() {
        val receipts = listOf(
            receipt(amountMinor = 28_860, rate = "55.5"),
            receipt(amountMinor = 28_860, rate = "55.5"),
        )

        assertEquals(1_040, MoneyCalculator.exactTripHomeMinor(receipts))
        assertEquals(11, MoneyCalculator.roundedUpTripHomeMajor(receipts, "EUR"))
    }

    private fun receipt(amountMinor: Long, rate: String): ReceiptEntity {
        val exact = MoneyCalculator.calculateExactHomeMinor(
            amountMinor = amountMinor,
            currencyCode = "EGP",
            exchangeRateSnapshot = rate,
            tipMinor = 0,
            tipCurrencyCode = "EUR",
            tipExchangeRateSnapshot = "1",
            homeCurrencyCode = "EUR",
        )
        return ReceiptEntity(
            id = "receipt-$amountMinor",
            tripId = "trip",
            occurredAt = 0,
            location = "",
            checkNumber = "",
            amountMinor = amountMinor,
            currencyCode = "EGP",
            exchangeRateSnapshot = rate,
            exactHomeMinor = exact,
            tipMinor = 0,
            tipCurrencyCode = "EUR",
            tipExchangeRateSnapshot = "1",
            imageUri = null,
            reviewState = "CONFIRMED",
            createdAt = 0,
        )
    }
}
