package de.shakie.billcheck.domain

import de.shakie.billcheck.data.NewReceiptItem
import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.TripCurrencyEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class BatchReceiptDraft(
    val location: String,
    val checkNumber: String,
    val amountMinor: Long,
    val currency: TripCurrencyEntity,
    val occurredAt: Long,
    val items: List<NewReceiptItem>,
    val reviewReasons: List<String>,
)

object BatchReceiptReviewReason {
    const val CURRENCY_MISSING = "CURRENCY_MISSING"
    const val CURRENCY_NOT_AVAILABLE = "CURRENCY_NOT_AVAILABLE"
    const val DATE_MISSING = "DATE_MISSING"
    const val DATE_INVALID = "DATE_INVALID"
    const val TIME_MISSING = "TIME_MISSING"
    const val TIME_INVALID = "TIME_INVALID"
    const val LOCATION_MISSING = "LOCATION_MISSING"
    const val CHECK_NUMBER_MISSING = "CHECK_NUMBER_MISSING"
    const val AMBIGUOUS = "AMBIGUOUS"
    const val TOTAL_PLAUSIBILITY = "TOTAL_PLAUSIBILITY"
    const val ITEMS_INCOMPLETE = "ITEMS_INCOMPLETE"
    const val POSSIBLE_DUPLICATE = "POSSIBLE_DUPLICATE"
}

object BatchReceiptExtractionMapper {
    fun map(
        extracted: ExtractedReceipt,
        currencies: List<TripCurrencyEntity>,
        fallbackOccurredAt: Long,
        existingReceipts: List<ReceiptEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): BatchReceiptDraft {
        require(currencies.isNotEmpty()) { "Trip has no currencies" }
        val defaultCurrency = currencies.firstOrNull { it.isDefault } ?: currencies.first()
        val detectedCurrencyCode = extracted.currencyCode.trim().uppercase(Locale.ROOT)
        val detectedCurrency = currencies.firstOrNull { it.currencyCode == detectedCurrencyCode }
        val reasons = mutableListOf<String>()
        val currency = when {
            detectedCurrencyCode.isBlank() -> defaultCurrency.also {
                reasons += BatchReceiptReviewReason.CURRENCY_MISSING
            }
            detectedCurrency == null -> defaultCurrency.also {
                reasons += "${BatchReceiptReviewReason.CURRENCY_NOT_AVAILABLE}=$detectedCurrencyCode"
            }
            else -> detectedCurrency
        }

        val items = extracted.items.mapNotNull { item ->
            val amount = CurrencyAmount.parseMajorToMinor(item.amountText, currency.currencyCode)
            if (amount == null || amount < 0) {
                if (item.name.isNotBlank() || item.amountText.isNotBlank()) {
                    reasons += BatchReceiptReviewReason.ITEMS_INCOMPLETE
                }
                null
            } else {
                NewReceiptItem(
                    name = formatBatchItemName(item.quantityText, item.name),
                    amountMinor = amount,
                )
            }
        }
        val amountMinor = CurrencyAmount.parseMajorToMinor(
            extracted.totalAmountText,
            currency.currencyCode,
        )?.takeIf { it > 0 } ?: items.takeIf(List<NewReceiptItem>::isNotEmpty)?.fold(0L) {
            total, item -> Math.addExact(total, item.amountMinor)
        }?.takeIf { it > 0 } ?: error("Kein gültiger Gesamtbetrag erkannt")

        val occurredAt = parseOccurredAt(extracted, fallbackOccurredAt, zoneId, reasons)
        if (extracted.location.isBlank()) reasons += BatchReceiptReviewReason.LOCATION_MISSING
        if (extracted.checkNumber.isBlank()) reasons += BatchReceiptReviewReason.CHECK_NUMBER_MISSING
        if (extracted.totalAmountNeedsReview) reasons += BatchReceiptReviewReason.TOTAL_PLAUSIBILITY
        if (hasAmbiguousCandidate(extracted)) reasons += BatchReceiptReviewReason.AMBIGUOUS

        val occurredDate = Instant.ofEpochMilli(occurredAt).atZone(zoneId).toLocalDate()
        if (existingReceipts.any { existing ->
                existing.currencyCode == currency.currencyCode &&
                    existing.amountMinor == amountMinor &&
                    Instant.ofEpochMilli(existing.occurredAt).atZone(zoneId).toLocalDate() == occurredDate &&
                    extracted.checkNumber.isNotBlank() &&
                    normalizedCheck(existing.checkNumber) == normalizedCheck(extracted.checkNumber)
            }
        ) {
            reasons += BatchReceiptReviewReason.POSSIBLE_DUPLICATE
        }

        return BatchReceiptDraft(
            location = extracted.location.trim(),
            checkNumber = extracted.checkNumber.trim(),
            amountMinor = amountMinor,
            currency = currency,
            occurredAt = occurredAt,
            items = items,
            reviewReasons = reasons.distinct(),
        )
    }

    private fun parseOccurredAt(
        extracted: ExtractedReceipt,
        fallback: Long,
        zoneId: ZoneId,
        reasons: MutableList<String>,
    ): Long {
        val date = when {
            extracted.occurredOn.isBlank() -> null.also {
                reasons += BatchReceiptReviewReason.DATE_MISSING
            }
            else -> runCatching {
                LocalDate.parse(extracted.occurredOn.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            }.getOrNull().also {
                if (it == null) reasons += BatchReceiptReviewReason.DATE_INVALID
            }
        } ?: return fallback
        val time = when {
            extracted.occurredTime.isBlank() -> LocalTime.MIDNIGHT.also {
                reasons += BatchReceiptReviewReason.TIME_MISSING
            }
            else -> runCatching {
                LocalTime.parse(extracted.occurredTime.trim(), DateTimeFormatter.ofPattern("HH:mm"))
            }.getOrNull() ?: LocalTime.MIDNIGHT.also {
                reasons += BatchReceiptReviewReason.TIME_INVALID
            }
        }
        return date.atTime(time).atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun hasAmbiguousCandidate(extracted: ExtractedReceipt): Boolean =
        listOf(
            extracted.locationSuggestions,
            extracted.checkNumberSuggestions,
            extracted.totalAmountSuggestions,
            extracted.occurredOnSuggestions,
            extracted.occurredTimeSuggestions,
        ).any { it.ambiguous } || extracted.items.any { item ->
            item.quantitySuggestions.ambiguous ||
                item.nameSuggestions.ambiguous ||
                item.amountSuggestions.ambiguous
        }

    private fun formatBatchItemName(quantity: String, name: String): String {
        val cleanQuantity = quantity.trim()
        val cleanName = name.trim()
        return when {
            cleanQuantity.isBlank() -> cleanName.ifBlank { "Posten" }
            cleanName.isBlank() -> cleanQuantity
            else -> "$cleanQuantity × $cleanName"
        }
    }

    private fun normalizedCheck(value: String): String = value.trim()
        .uppercase(Locale.ROOT)
        .filter(Char::isLetterOrDigit)
        .trimStart('0')
}
