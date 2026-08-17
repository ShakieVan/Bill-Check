package de.shakie.billcheck.domain

import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.StatementLineEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `cash register prefix before check number still produces automatic match`() {
        val line = line(
            check = "0015512",
            amount = 31_332,
            description = "Sultana Restaurant Food",
        )
        val receipt = receipt(
            check = "5512",
            amount = 31_332,
            location = "Sultana Restaurant",
        )

        val ranked = ReconciliationMatcher.rank(line, listOf(receipt))

        assertEquals(78, ranked.single().score)
        assertTrue(ReconciliationMatcher.isStrongAutomaticMatch(line, receipt))
        assertEquals(receipt, ReconciliationMatcher.selectAutomaticMatch(line, ranked))
        assertEquals(ReconciliationStatus.CORRECT, ReconciliationMatcher.suggestedStatus(line, receipt))
    }

    @Test
    fun `three digit receipt id matches register-prefixed statement id with exact supporting facts`() {
        val line = line(
            check = "0050783",
            amount = 116_006,
            occurredOn = 0,
            description = "Sunset Lobby Beverage",
        )
        val receipt = receipt(
            check = "783",
            amount = 116_006,
            location = "Sunset Lobby",
        )

        val ranked = ReconciliationMatcher.rank(line, listOf(receipt))

        assertTrue(ReconciliationMatcher.isStrongAutomaticMatch(line, receipt))
        assertEquals(receipt, ReconciliationMatcher.selectAutomaticMatch(line, ranked))
        assertEquals(ReconciliationStatus.CORRECT, ReconciliationMatcher.suggestedStatus(line, receipt))
    }

    @Test
    fun `three digit suffix does not match without exact date and similar location`() {
        val line = line(
            check = "0050783",
            amount = 116_006,
            occurredOn = 0,
            description = "Sunset Lobby Beverage",
        )
        val wrongContext = receipt(
            check = "783",
            amount = 116_006,
            location = "Sultana Restaurant",
        ).copy(occurredAt = 24 * 60 * 60 * 1_000L)

        assertFalse(ReconciliationMatcher.isStrongAutomaticMatch(line, wrongContext))
    }

    @Test
    fun `three digit suffix remains unresolved when two receipts are equally plausible`() {
        val line = line(
            check = "0050783",
            amount = 116_006,
            occurredOn = 0,
            description = "Sunset Lobby Beverage",
        )
        val first = receipt(id = "first", check = "783", amount = 116_006, location = "Sunset Lobby")
        val second = receipt(id = "second", check = "783", amount = 116_006, location = "Sunset Lobby")

        assertNull(ReconciliationMatcher.selectAutomaticMatch(line, ReconciliationMatcher.rank(line, listOf(first, second))))
    }

    @Test
    fun `two digit suffix can be linked but remains uncertain`() {
        val line = line(
            check = "0050783",
            amount = 116_006,
            occurredOn = 0,
            description = "Sunset Lobby Beverage",
        )
        val receipt = receipt(check = "83", amount = 116_006, location = "Sunset Lobby")

        assertTrue(ReconciliationMatcher.isStrongAutomaticMatch(line, receipt))
        assertEquals(ReconciliationStatus.UNCERTAIN, ReconciliationMatcher.suggestedStatus(line, receipt))
    }

    @Test
    fun `single digit suffix can be linked only with exact supporting facts and remains uncertain`() {
        val line = line(
            check = "0050001",
            amount = 116_006,
            occurredOn = 0,
            description = "Sunset Lobby Beverage",
        )
        val receipt = receipt(check = "1", amount = 116_006, location = "Sunset Lobby")

        assertTrue(ReconciliationMatcher.isStrongAutomaticMatch(line, receipt))
        assertEquals(
            ReconciliationStatus.UNCERTAIN,
            ReconciliationMatcher.suggestedStatus(line, receipt),
        )
    }

    @Test
    fun `single digit suffix is rejected when restaurant is unrelated`() {
        val line = line(
            check = "0050001",
            amount = 116_006,
            occurredOn = 0,
            description = "Sunset Lobby Beverage",
        )
        val receipt = receipt(check = "1", amount = 116_006, location = "Sultana Restaurant")

        assertFalse(ReconciliationMatcher.isStrongAutomaticMatch(line, receipt))
    }

    @Test
    fun `typo and abbreviation are tolerated when amount and date confirm candidate`() {
        val line = line(
            check = "4712",
            amount = 12_345,
            occurredOn = 0,
            description = "Utopia Beach Club",
        )
        val receipt = receipt(
            check = "4711",
            amount = 12_345,
            location = "Utopia Bch Club",
        )

        assertTrue(ReconciliationMatcher.isStrongAutomaticMatch(line, receipt))
        assertTrue(ReconciliationMatcher.rank(line, listOf(receipt)).single().score >= 80)
    }

    @Test
    fun `amount date and location alone do not override unrelated check number`() {
        val line = line(check = "9999", amount = 12_345, occurredOn = 0)
        val receipt = receipt(check = "4711", amount = 12_345)

        assertFalse(ReconciliationMatcher.isStrongAutomaticMatch(line, receipt))
    }

    @Test
    fun `automatic matching rejects nearly tied candidates`() {
        val line = line(check = "0015512", amount = 31_332, occurredOn = 0)
        val first = receipt(id = "first", check = "5512", amount = 31_332)
        val second = receipt(id = "second", check = "5512", amount = 31_332)
        val ranked = ReconciliationMatcher.rank(line, listOf(first, second))

        assertNull(ReconciliationMatcher.selectAutomaticMatch(line, ranked))
    }

    @Test
    fun `alphanumeric check prefixes remain significant`() {
        assertEquals("A00123", ReconciliationMatcher.normalizeCheckNumber("CHK-A00123"))
        assertEquals("B00123", ReconciliationMatcher.normalizeCheckNumber("CHK-B00123"))
        assertFalse(
            ReconciliationMatcher.isStrongAutomaticMatch(
                line(check = "A123", amount = 12_345, occurredOn = 0),
                receipt(check = "B123", amount = 12_345),
            ),
        )
    }

    @Test
    fun `exact id and amount with wildly wrong date remains linked but flagged`() {
        val thirtyDays = 30L * 24 * 60 * 60 * 1_000
        val statement = line(check = "5512", amount = 31_332, occurredOn = 0)
        val captured = receipt(check = "5512", amount = 31_332).copy(occurredAt = thirtyDays)

        assertTrue(ReconciliationMatcher.isStrongAutomaticMatch(statement, captured))
        assertEquals(
            ReconciliationStatus.DATE_MISMATCH,
            ReconciliationMatcher.suggestedStatus(statement, captured),
        )
    }

    @Test
    fun `long extremes never overflow amount ranking`() {
        val ranked = ReconciliationMatcher.rank(
            line(check = "1", amount = Long.MAX_VALUE),
            listOf(receipt(check = "2", amount = Long.MIN_VALUE)),
        )

        assertEquals(1, ranked.size)
        assertTrue(ranked.single().score >= 0)
    }

    private fun line(
        check: String,
        amount: Long,
        occurredOn: Long? = null,
        description: String = "Beach Restaurant",
    ) = StatementLineEntity(
        id = "line",
        reconciliationId = "reconciliation",
        occurredOn = occurredOn,
        description = description,
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
        location: String = "Beach Restaurant",
    ) = ReceiptEntity(
        id = id,
        tripId = "trip",
        occurredAt = 0,
        location = location,
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
