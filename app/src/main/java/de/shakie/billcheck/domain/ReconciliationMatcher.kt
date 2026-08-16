package de.shakie.billcheck.domain

import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.StatementLineEntity
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

object ReconciliationStatus {
    const val CORRECT = "CORRECT"
    const val UNCERTAIN = "UNCERTAIN"
    const val AMOUNT_MISMATCH = "AMOUNT_MISMATCH"
    const val NOT_FOUND = "NOT_FOUND"
    const val ACCEPTED = "ACCEPTED"
}

data class RankedReceiptCandidate(
    val receipt: ReceiptEntity,
    val score: Int,
)

object ReconciliationMatcher {
    fun normalizeCheckNumber(value: String): String {
        val compact = value.uppercase().filter(Char::isLetterOrDigit)
        val numeric = compact.filter(Char::isDigit).trimStart('0')
        return numeric.ifBlank { compact.trimStart('0') }
    }

    fun rank(
        line: StatementLineEntity,
        receipts: List<ReceiptEntity>,
    ): List<RankedReceiptCandidate> = receipts
        .map { receipt -> RankedReceiptCandidate(receipt, score(line, receipt)) }
        .sortedWith(
            compareByDescending<RankedReceiptCandidate> { it.score }
                .thenBy { abs(it.receipt.foreignAmountMinor - line.amountMinor) }
                .thenByDescending { it.receipt.occurredAt },
        )

    fun suggestedStatus(line: StatementLineEntity, receipt: ReceiptEntity): String {
        val checkMatches = hasSameCheckNumber(line, receipt)
        val amountMatches = line.amountMinor == receipt.foreignAmountMinor &&
            line.currencyCode.equals(receipt.foreignCurrencyCode, ignoreCase = true)
        return when {
            checkMatches && amountMatches -> ReconciliationStatus.CORRECT
            checkMatches -> ReconciliationStatus.AMOUNT_MISMATCH
            amountMatches -> ReconciliationStatus.UNCERTAIN
            else -> ReconciliationStatus.NOT_FOUND
        }
    }

    fun isStrongAutomaticMatch(line: StatementLineEntity, receipt: ReceiptEntity): Boolean =
        hasSameCheckNumber(line, receipt) &&
            line.amountMinor == receipt.foreignAmountMinor &&
            line.currencyCode.equals(receipt.foreignCurrencyCode, ignoreCase = true)

    private fun score(line: StatementLineEntity, receipt: ReceiptEntity): Int {
        var result = 0
        if (hasSameCheckNumber(line, receipt)) result += 100
        if (line.amountMinor == receipt.foreignAmountMinor) result += 60
        if (line.currencyCode.equals(receipt.foreignCurrencyCode, ignoreCase = true)) result += 10
        if (line.occurredOn != null && sameLocalDay(line.occurredOn, receipt.occurredAt)) result += 25
        val descriptionWords = line.description.lowercase().split(Regex("\\W+"))
            .filter { it.length >= 3 }
        val location = receipt.location.lowercase()
        result += descriptionWords.count(location::contains).coerceAtMost(3) * 5
        return result
    }

    private fun hasSameCheckNumber(line: StatementLineEntity, receipt: ReceiptEntity): Boolean {
        val lineCheck = normalizeCheckNumber(line.checkNumber)
        val receiptCheck = normalizeCheckNumber(receipt.checkNumber)
        return lineCheck.isNotBlank() && lineCheck == receiptCheck
    }

    private fun sameLocalDay(first: Long, second: Long): Boolean {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(first).atZone(zone).toLocalDate() ==
            Instant.ofEpochMilli(second).atZone(zone).toLocalDate()
    }
}
