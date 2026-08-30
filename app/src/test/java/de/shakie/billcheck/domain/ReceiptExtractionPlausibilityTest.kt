package de.shakie.billcheck.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptExtractionPlausibilityTest {
    @Test
    fun `total copied from one of multiple item rows needs review`() {
        val receipt = receipt("56.78", "12.34", "56.78")

        assertTrue(ReceiptExtractionPlausibility.markUnsafeTotal(receipt).totalAmountNeedsReview)
    }

    @Test
    fun `exact item sum is safe`() {
        val receipt = receipt("69.12", "12.34", "56.78")

        assertFalse(ReceiptExtractionPlausibility.markUnsafeTotal(receipt).totalAmountNeedsReview)
    }

    @Test
    fun `tax service and discount differences are not guessed`() {
        assertFalse(
            ReceiptExtractionPlausibility.markUnsafeTotal(
                receipt("75.00", "20.00", "50.00"),
            ).totalAmountNeedsReview,
        )
        assertFalse(
            ReceiptExtractionPlausibility.markUnsafeTotal(
                receipt("65.00", "20.00", "50.00"),
            ).totalAmountNeedsReview,
        )
    }

    @Test
    fun `single item receipt may equal total`() {
        assertFalse(
            ReceiptExtractionPlausibility.markUnsafeTotal(
                receipt("20.00", "20.00"),
            ).totalAmountNeedsReview,
        )
    }

    @Test
    fun `currency fraction digits are respected`() {
        assertTrue(
            ReceiptExtractionPlausibility.markUnsafeTotal(
                receipt("200", "100", "200", currency = "JPY"),
            ).totalAmountNeedsReview,
        )
        assertTrue(
            ReceiptExtractionPlausibility.markUnsafeTotal(
                receipt("2.345", "1.111", "2.345", currency = "KWD"),
            ).totalAmountNeedsReview,
        )
    }

    @Test
    fun `invalid proposed total needs review but blank total does not`() {
        assertTrue(
            ReceiptExtractionPlausibility.markUnsafeTotal(
                receipt("20.-", "10.00", "10.00"),
            ).totalAmountNeedsReview,
        )
        assertFalse(
            ReceiptExtractionPlausibility.markUnsafeTotal(
                receipt("", "10.00", "10.00"),
            ).totalAmountNeedsReview,
        )
    }

    @Test
    fun `overflowing item sum is never considered safe`() {
        val receipt = receipt("92233720368547758.07", "92233720368547758.07", "0.01")

        assertTrue(ReceiptExtractionPlausibility.markUnsafeTotal(receipt).totalAmountNeedsReview)
    }

    private fun receipt(
        total: String,
        vararg itemAmounts: String,
        currency: String = "EUR",
    ) = ExtractedReceipt(
        location = "Test venue",
        checkNumber = "123",
        totalAmountText = total,
        currencyCode = currency,
        occurredOn = "2026-08-30",
        items = itemAmounts.mapIndexed { index, amount ->
            ExtractedItem(name = "Item ${index + 1}", amountText = amount)
        },
    )
}
