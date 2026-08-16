package de.shakie.billcheck.domain

import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.StatementLineEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconciliationMatcherTest {
    @Test
    fun `check numbers tolerate formatting and leading zeroes`() {
        assertEquals("12345", ReconciliationMatcher.normalizeCheckNumber("CHK-0012345"))
        assertEquals("12345", ReconciliationMatcher.normalizeCheckNumber("00012345"))
    }

    @Test
    fun `exact check and amount is a strong correct match`() {
        val line = line(check = "000-4711", amount = 12_345)
        val receipt = receipt(check = "4711", amount = 12_345)

        assertTrue(ReconciliationMatcher.isStrongAutomaticMatch(line, receipt))
        assertEquals(ReconciliationStatus.CORRECT, ReconciliationMatcher.suggestedStatus(line, receipt))
    }

    @Test
    fun `same check with different amount is orange mismatch`() {
        val status = ReconciliationMatcher.suggestedStatus(
            line(check = "0815", amount = 10_000),
            receipt(check = "0000815", amount = 9_500),
        )

        assertEquals(ReconciliationStatus.AMOUNT_MISMATCH, status)
    }

    @Test
    fun `ranking prioritizes normalized check before amount-only candidates`() {
        val line = line(check = "42", amount = 5_000)
        val amountOnly = receipt(id = "amount", check = "7", amount = 5_000)
        val checkCandidate = receipt(id = "check", check = "00042", amount = 5_100)

        val ranked = ReconciliationMatcher.rank(line, listOf(amountOnly, checkCandidate))

        assertEquals("check", ranked.first().receipt.id)
    }

    private fun line(check: String, amount: Long) = StatementLineEntity(
        id = "line",
        reconciliationId = "reconciliation",
        occurredOn = null,
        description = "Beach Restaurant",
        checkNumber = check,
        amountMinor = amount,
        currencyCode = "EGP",
        status = ReconciliationStatus.NOT_FOUND,
        acceptedWithoutReceipt = false,
    )

    private fun receipt(
        id: String = "receipt",
        check: String,
        amount: Long,
    ) = ReceiptEntity(
        id = id,
        tripId = "trip",
        occurredAt = 0,
        location = "Beach Restaurant",
        checkNumber = check,
        foreignAmountMinor = amount,
        foreignCurrencyCode = "EGP",
        exchangeRate = "55.5",
        exactEuroCents = 0,
        tipMinor = 0,
        tipCurrencyCode = "EUR",
        imageUri = null,
        reviewState = "CONFIRMED",
        createdAt = 0,
    )
}
