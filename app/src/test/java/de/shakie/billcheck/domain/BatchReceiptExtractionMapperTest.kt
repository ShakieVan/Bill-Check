package de.shakie.billcheck.domain

import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.TripCurrencyEntity
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchReceiptExtractionMapperTest {
    private val currencies = listOf(
        TripCurrencyEntity("trip", "EGP", "55.5", "FIXED", true),
        TripCurrencyEntity("trip", "EUR", "1", "FIXED", false),
    )

    @Test
    fun `clean extraction becomes complete receipt draft`() {
        val draft = BatchReceiptExtractionMapper.map(
            extracted = extracted(),
            currencies = currencies,
            fallbackOccurredAt = 0,
            existingReceipts = emptyList(),
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals("Sultana", draft.location)
        assertEquals(15_595, draft.amountMinor)
        assertEquals("EGP", draft.currency.currencyCode)
        assertEquals("2024-12-28T19:42:00Z", Instant.ofEpochMilli(draft.occurredAt).toString())
        assertEquals("5 × Cola", draft.items.single().name)
        assertTrue(draft.reviewReasons.isEmpty())
    }

    @Test
    fun `unavailable currency and missing date use safe fallbacks and require review`() {
        val draft = BatchReceiptExtractionMapper.map(
            extracted = extracted().copy(currencyCode = "USD", occurredOn = "", occurredTime = ""),
            currencies = currencies,
            fallbackOccurredAt = 1234,
            existingReceipts = emptyList(),
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals("EGP", draft.currency.currencyCode)
        assertEquals(1234, draft.occurredAt)
        assertTrue(draft.reviewReasons.contains("CURRENCY_NOT_AVAILABLE=USD"))
        assertTrue(draft.reviewReasons.contains(BatchReceiptReviewReason.DATE_MISSING))
    }

    @Test
    fun `unsafe ambiguous possible duplicate remains automatic but is marked`() {
        val extracted = extracted().copy(
            totalAmountNeedsReview = true,
            locationSuggestions = ExtractedFieldSuggestions(
                preferred = "Sultana",
                ambiguous = true,
                candidates = listOf(
                    ExtractedFieldCandidate("Sultana", "Sultana", AiSuggestionCertainty.MEDIUM),
                    ExtractedFieldCandidate("Beach", "Beach", AiSuggestionCertainty.MEDIUM),
                ),
            ),
        )
        val existing = receipt(
            occurredAt = Instant.parse("2024-12-28T10:00:00Z").toEpochMilli(),
        )

        val draft = BatchReceiptExtractionMapper.map(
            extracted = extracted,
            currencies = currencies,
            fallbackOccurredAt = 0,
            existingReceipts = listOf(existing),
            zoneId = ZoneId.of("UTC"),
        )

        assertTrue(draft.reviewReasons.contains(BatchReceiptReviewReason.TOTAL_PLAUSIBILITY))
        assertTrue(draft.reviewReasons.contains(BatchReceiptReviewReason.AMBIGUOUS))
        assertTrue(draft.reviewReasons.contains(BatchReceiptReviewReason.POSSIBLE_DUPLICATE))
    }

    @Test(expected = IllegalStateException::class)
    fun `receipt without total or valid items is not fabricated`() {
        BatchReceiptExtractionMapper.map(
            extracted = extracted().copy(totalAmountText = "", items = emptyList()),
            currencies = currencies,
            fallbackOccurredAt = 0,
            existingReceipts = emptyList(),
            zoneId = ZoneId.of("UTC"),
        )
    }

    private fun extracted() = ExtractedReceipt(
        location = "Sultana",
        checkNumber = "5595",
        totalAmountText = "155.95",
        currencyCode = "EGP",
        occurredOn = "2024-12-28",
        occurredTime = "19:42",
        items = listOf(ExtractedItem("Cola", "125.00", quantityText = "5")),
    )

    private fun receipt(occurredAt: Long) = ReceiptEntity(
        id = "existing",
        tripId = "trip",
        occurredAt = occurredAt,
        location = "Sultana",
        checkNumber = "5595",
        amountMinor = 15_595,
        currencyCode = "EGP",
        exchangeRateSnapshot = "55.5",
        exactHomeMinor = 281,
        tipMinor = 0,
        tipCurrencyCode = "EUR",
        tipExchangeRateSnapshot = "1",
        imageUri = null,
        reviewState = "CONFIRMED",
        createdAt = 0,
    )
}
