package de.shakie.billcheck.domain

import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.StatementLineEntity
import java.text.Normalizer
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

object ReconciliationStatus {
    const val CORRECT = "CORRECT"
    const val UNCERTAIN = "UNCERTAIN"
    const val AMOUNT_MISMATCH = "AMOUNT_MISMATCH"
    const val CURRENCY_MISMATCH = "CURRENCY_MISMATCH"
    const val DATE_MISMATCH = "DATE_MISMATCH"
    const val NOT_FOUND = "NOT_FOUND"
    const val ACCEPTED = "ACCEPTED"
}

data class RankedReceiptCandidate(
    val receipt: ReceiptEntity,
    val score: Int,
)

object ReconciliationMatcher {
    private const val CHECK_NUMBER_WEIGHT = 40
    private const val AMOUNT_WEIGHT = 30
    private const val DATE_WEIGHT = 15
    private const val LOCATION_WEIGHT = 15
    private const val AUTOMATIC_MATCH_THRESHOLD = 75
    private const val AUTOMATIC_MATCH_MARGIN = 10

    fun normalizeCheckNumber(value: String): String {
        val compact = value.uppercase(Locale.ROOT).filter(Char::isLetterOrDigit)
        val withoutKnownLabel = when {
            compact.startsWith("CHECK") -> compact.removePrefix("CHECK")
            compact.startsWith("CHK") -> compact.removePrefix("CHK")
            else -> compact
        }
        return if (withoutKnownLabel.all(Char::isDigit)) {
            withoutKnownLabel.trimStart('0').ifBlank { "0" }
        } else {
            withoutKnownLabel
        }
    }

    fun rank(
        line: StatementLineEntity,
        receipts: List<ReceiptEntity>,
    ): List<RankedReceiptCandidate> = receipts
        .map { receipt -> RankedReceiptCandidate(receipt, score(line, receipt)) }
        .sortedWith(
            compareByDescending<RankedReceiptCandidate> { it.score }
                .thenBy { absoluteDifference(it.receipt.foreignAmountMinor, line.amountMinor) }
                .thenByDescending { it.receipt.occurredAt },
        )

    fun selectAutomaticMatch(
        line: StatementLineEntity,
        ranked: List<RankedReceiptCandidate>,
    ): ReceiptEntity? {
        val best = ranked.firstOrNull() ?: return null
        if (!isStrongAutomaticMatch(line, best.receipt)) return null
        val runnerUp = ranked.getOrNull(1)
        if (runnerUp != null && best.score - runnerUp.score < AUTOMATIC_MATCH_MARGIN) return null
        return best.receipt
    }

    fun suggestedStatus(line: StatementLineEntity, receipt: ReceiptEntity): String {
        val checkMatches = hasSameCheckNumber(line, receipt)
        val amountMatches = line.amountMinor == receipt.foreignAmountMinor
        val currencyMatches = line.currencyCode.equals(receipt.foreignCurrencyCode, ignoreCase = true)
        val dateMismatch = dateDistanceDays(line.occurredOn, receipt.occurredAt)?.let { it > 2 } == true
        val numericSuffixLength = numericSuffixLength(line.checkNumber, receipt.checkNumber)
        return when {
            checkMatches && amountMatches && currencyMatches && dateMismatch -> ReconciliationStatus.DATE_MISMATCH
            numericSuffixLength != null && numericSuffixLength <= 2 &&
                isStrongAutomaticMatch(line, receipt) -> ReconciliationStatus.UNCERTAIN
            isStrongAutomaticMatch(line, receipt) -> ReconciliationStatus.CORRECT
            checkMatches && !currencyMatches -> ReconciliationStatus.CURRENCY_MISMATCH
            checkMatches -> ReconciliationStatus.AMOUNT_MISMATCH
            amountMatches && currencyMatches -> ReconciliationStatus.UNCERTAIN
            else -> ReconciliationStatus.NOT_FOUND
        }
    }

    fun isStrongAutomaticMatch(line: StatementLineEntity, receipt: ReceiptEntity): Boolean {
        val amountMatches = line.amountMinor == receipt.foreignAmountMinor &&
            line.currencyCode.equals(receipt.foreignCurrencyCode, ignoreCase = true)
        if (!amountMatches) return false
        if (hasSameCheckNumber(line, receipt)) return true

        val checkScore = checkNumberScore(line.checkNumber, receipt.checkNumber)
        val dateScore = dateScore(line.occurredOn, receipt.occurredAt)
        val locationScore = locationScore(line.description, receipt.location)
        val numericSuffixLength = numericSuffixLength(line.checkNumber, receipt.checkNumber)
        if (numericSuffixLength != null && numericSuffixLength <= 3) {
            val requiredLocationScore = if (numericSuffixLength == 3) 8 else 12
            return dateScore == DATE_WEIGHT &&
                locationScore >= requiredLocationScore &&
                checkScore + AMOUNT_WEIGHT + dateScore + locationScore >= AUTOMATIC_MATCH_THRESHOLD
        }
        return checkScore >= 30 &&
            (dateScore > 0 || locationScore >= 8) &&
            checkScore + AMOUNT_WEIGHT + dateScore + locationScore >= AUTOMATIC_MATCH_THRESHOLD
    }

    private fun score(line: StatementLineEntity, receipt: ReceiptEntity): Int {
        return checkNumberScore(line.checkNumber, receipt.checkNumber) +
            amountScore(line, receipt) +
            dateScore(line.occurredOn, receipt.occurredAt) +
            locationScore(line.description, receipt.location)
    }

    private fun hasSameCheckNumber(line: StatementLineEntity, receipt: ReceiptEntity): Boolean {
        val lineCheck = normalizeCheckNumber(line.checkNumber)
        val receiptCheck = normalizeCheckNumber(receipt.checkNumber)
        return lineCheck.isNotBlank() && lineCheck == receiptCheck
    }

    private fun checkNumberScore(first: String, second: String): Int {
        val normalizedFirst = normalizeCheckNumber(first)
        val normalizedSecond = normalizeCheckNumber(second)
        if (normalizedFirst.isBlank() || normalizedSecond.isBlank()) return 0
        if (normalizedFirst == normalizedSecond) return CHECK_NUMBER_WEIGHT
        if (normalizedFirst.any(Char::isLetter) || normalizedSecond.any(Char::isLetter)) {
            return editSimilarityScore(normalizedFirst, normalizedSecond, CHECK_NUMBER_WEIGHT)
                .coerceAtMost(24)
        }

        when (numericSuffixLength(first, second)) {
            in 4..Int.MAX_VALUE -> return 36
            3 -> return 30
            2 -> return 24
            1 -> return 18
        }

        return editSimilarityScore(normalizedFirst, normalizedSecond, CHECK_NUMBER_WEIGHT)
            .coerceAtMost(32)
    }

    private fun numericSuffixLength(first: String, second: String): Int? {
        val normalizedFirst = normalizeCheckNumber(first)
        val normalizedSecond = normalizeCheckNumber(second)
        if (!normalizedFirst.all(Char::isDigit) || !normalizedSecond.all(Char::isDigit)) return null
        if (normalizedFirst == normalizedSecond) return null
        if (!normalizedFirst.endsWith(normalizedSecond) && !normalizedSecond.endsWith(normalizedFirst)) return null
        return minOf(normalizedFirst.length, normalizedSecond.length)
    }

    private fun amountScore(line: StatementLineEntity, receipt: ReceiptEntity): Int {
        if (!line.currencyCode.equals(receipt.foreignCurrencyCode, ignoreCase = true)) return 0
        val difference = absoluteDifference(line.amountMinor, receipt.foreignAmountMinor)
        if (difference.signum() == 0) return AMOUNT_WEIGHT
        val largerAmount = BigInteger.valueOf(maxOf(line.amountMinor, receipt.foreignAmountMinor))
        return when {
            largerAmount.signum() > 0 && difference <= largerAmount.divide(BigInteger.valueOf(100)) -> 20
            largerAmount.signum() > 0 && difference <= largerAmount.divide(BigInteger.valueOf(20)) -> 10
            else -> 0
        }
    }

    private fun dateScore(first: Long?, second: Long): Int {
        if (first == null) return 0
        return when (dateDistanceDays(first, second)) {
            0L -> DATE_WEIGHT
            1L -> 10
            2L -> 5
            else -> 0
        }
    }

    internal fun dateDistanceDays(first: Long?, second: Long): Long? {
        if (first == null) return null
        val zone = ZoneId.systemDefault()
        val firstDate = Instant.ofEpochMilli(first).atZone(zone).toLocalDate()
        val secondDate = Instant.ofEpochMilli(second).atZone(zone).toLocalDate()
        return kotlin.math.abs(ChronoUnit.DAYS.between(firstDate, secondDate))
    }

    private fun absoluteDifference(first: Long, second: Long): BigInteger =
        BigInteger.valueOf(first).subtract(BigInteger.valueOf(second)).abs()

    private fun locationScore(first: String, second: String): Int {
        val normalizedFirst = normalizeText(first)
        val normalizedSecond = normalizeText(second)
        if (normalizedFirst.isBlank() || normalizedSecond.isBlank()) return 0
        if (normalizedFirst == normalizedSecond) return LOCATION_WEIGHT

        val compactScore = editSimilarityScore(
            normalizedFirst.replace(" ", ""),
            normalizedSecond.replace(" ", ""),
            LOCATION_WEIGHT,
        )
        val firstWords = normalizedFirst.split(' ').filter { it.length >= 3 }.toSet()
        val secondWords = normalizedSecond.split(' ').filter { it.length >= 3 }.toSet()
        val tokenScore = if (firstWords.isEmpty() || secondWords.isEmpty()) {
            0
        } else {
            2 * firstWords.intersect(secondWords).size * LOCATION_WEIGHT /
                (firstWords.size + secondWords.size)
        }
        return maxOf(compactScore, tokenScore)
    }

    private fun normalizeText(value: String): String = Normalizer.normalize(
        value.lowercase(Locale.ROOT),
        Normalizer.Form.NFD,
    )
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
        .trim()
        .replace(Regex(" +"), " ")

    private fun editSimilarityScore(first: String, second: String, weight: Int): Int {
        val maximumLength = maxOf(first.length, second.length)
        if (maximumLength == 0) return weight
        return (maximumLength - levenshteinDistance(first, second)) * weight / maximumLength
    }

    private fun levenshteinDistance(first: String, second: String): Int {
        if (first == second) return 0
        if (first.isEmpty()) return second.length
        if (second.isEmpty()) return first.length
        var previous = IntArray(second.length + 1) { it }
        var current = IntArray(second.length + 1)
        first.forEachIndexed { firstIndex, firstChar ->
            current[0] = firstIndex + 1
            second.forEachIndexed { secondIndex, secondChar ->
                current[secondIndex + 1] = minOf(
                    current[secondIndex] + 1,
                    previous[secondIndex + 1] + 1,
                    previous[secondIndex] + if (firstChar == secondChar) 0 else 1,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[second.length]
    }
}
