package de.shakie.billcheck.domain

/**
 * Conservative local checks for extraction mistakes that are unsafe to auto-apply.
 *
 * A receipt total may legitimately differ from the sum of extracted items because of taxes,
 * service, discounts, tips, or incomplete extraction. We therefore never replace it with a
 * calculated value. The narrow mistake this catches is a proposed total that is itself one of at
 * least two positive item-line amounts while the positive item sum is different.
 */
object ReceiptExtractionPlausibility {
    fun markUnsafeTotal(extracted: ExtractedReceipt): ExtractedReceipt {
        val currency = extracted.currencyCode.trim()
        val total = CurrencyAmount.parseMajorToMinor(extracted.totalAmountText, currency)
            ?.takeIf { it > 0 }
            ?: return extracted.copy(totalAmountNeedsReview = extracted.totalAmountText.isNotBlank())
        val itemAmounts = extracted.items.mapNotNull { item ->
            CurrencyAmount.parseMajorToMinor(item.amountText, currency)?.takeIf { it > 0 }
        }
        if (itemAmounts.size < 2) return extracted
        val itemSum = itemAmounts.fold(0L) { sum, amount ->
            runCatching { Math.addExact(sum, amount) }.getOrElse {
                return extracted.copy(totalAmountNeedsReview = true)
            }
        }
        val proposedTotalLooksLikeItem = itemAmounts.any { it == total }
        return extracted.copy(
            totalAmountNeedsReview = proposedTotalLooksLikeItem && itemSum != total,
        )
    }
}
