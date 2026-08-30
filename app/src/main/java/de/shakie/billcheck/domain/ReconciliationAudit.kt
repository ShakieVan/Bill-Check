package de.shakie.billcheck.domain

import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.ReconciliationWithLines
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId

enum class StatementTotalCheck { MATCH, MISMATCH, UNAVAILABLE, CURRENCY_MISMATCH }

data class ReconciliationAudit(
    val recognizedLineCount: Int,
    val matchedCorrectCount: Int,
    val acceptedCount: Int,
    val recognizedLinesWithoutReceiptCount: Int,
    val receiptsWithoutRecognizedLineCount: Int,
    val uncertainMatchCount: Int,
    val amountMismatchCount: Int,
    val currencyMismatchCount: Int,
    val dateMismatchCount: Int,
    val invalidMatchCount: Int,
    val ambiguousDateCount: Int,
    val receiptsOutsideRecognizedDateRangeCount: Int,
    val duplicateStatementLineCount: Int,
    val duplicateReceiptCount: Int,
    val nonPositiveAmountCount: Int,
    val statementTotals: Map<String, BigInteger>,
    val receiptTotals: Map<String, BigInteger>,
    val matchedReceiptTotals: Map<String, BigInteger>,
    val declaredTotalMinor: BigInteger?,
    val declaredTotalCurrencyCode: String?,
    val declaredTotalDifferenceMinor: BigInteger?,
    val totalCheck: StatementTotalCheck,
)

object ReconciliationAuditor {
    fun audit(
        reconciliation: ReconciliationWithLines,
        receipts: List<ReceiptEntity>,
    ): ReconciliationAudit {
        val receiptById = receipts.associateBy { it.id }
        val currentMatchedIds = reconciliation.lines.flatMap { it.matches }
            .mapTo(mutableSetOf()) { it.receiptId }
        val effectiveStatuses = reconciliation.lines.map { related ->
            val receipt = related.matches.singleOrNull()?.receiptId?.let(receiptById::get)
            when {
                related.line.acceptedWithoutReceipt -> ReconciliationStatus.ACCEPTED
                receipt != null -> ReconciliationMatcher.suggestedStatus(related.line, receipt)
                else -> related.line.status
            }
        }
        val statementTotals = totalsByCurrency(
            reconciliation.lines.map { it.line.currencyCode to it.line.amountMinor },
        )
        val receiptTotals = totalsByCurrency(
            receipts.map { it.currencyCode to it.amountMinor },
        )
        val matchedReceiptTotals = totalsByCurrency(
            receipts.filter { it.id in currentMatchedIds }
                .map { it.currencyCode to it.amountMinor },
        )
        val declaredMinor = reconciliation.reconciliation.declaredTotalMinor?.let(BigInteger::valueOf)
        val declaredCurrency = reconciliation.reconciliation.declaredTotalCurrencyCode
        val computedDeclaredCurrency = declaredCurrency?.let(statementTotals::get)
        val totalCheck = when {
            declaredMinor == null || declaredCurrency == null -> StatementTotalCheck.UNAVAILABLE
            reconciliation.lines.any {
                !it.line.currencyCode.equals(declaredCurrency, ignoreCase = true)
            } -> StatementTotalCheck.CURRENCY_MISMATCH
            computedDeclaredCurrency == declaredMinor -> StatementTotalCheck.MATCH
            else -> StatementTotalCheck.MISMATCH
        }
        val zone = ZoneId.systemDefault()
        val statementDates = reconciliation.lines.mapNotNull { related ->
            related.line.occurredOn?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        }
        val minDate = statementDates.minOrNull()
        val maxDate = statementDates.maxOrNull()
        val receiptsOutsideRange = if (minDate == null || maxDate == null) 0 else receipts.count {
            val date = Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate()
            date < minDate || date > maxDate
        }
        val statementFingerprints = reconciliation.lines.map { related ->
            listOf(
                related.line.occurredOn?.toString().orEmpty(),
                ReconciliationMatcher.normalizeCheckNumber(related.line.checkNumber),
                related.line.amountMinor.toString(),
                related.line.currencyCode.uppercase(),
            ).joinToString("|")
        }
        val receiptFingerprints = receipts.map { receipt ->
            listOf(
                receipt.occurredAt.toString(),
                ReconciliationMatcher.normalizeCheckNumber(receipt.checkNumber),
                receipt.amountMinor.toString(),
                receipt.currencyCode.uppercase(),
            ).joinToString("|")
        }
        return ReconciliationAudit(
            recognizedLineCount = reconciliation.lines.size,
            matchedCorrectCount = effectiveStatuses.count { it == ReconciliationStatus.CORRECT },
            acceptedCount = effectiveStatuses.count { it == ReconciliationStatus.ACCEPTED },
            recognizedLinesWithoutReceiptCount = reconciliation.lines.count {
                it.matches.isEmpty() && !it.line.acceptedWithoutReceipt
            },
            receiptsWithoutRecognizedLineCount = receipts.count { it.id !in currentMatchedIds },
            uncertainMatchCount = effectiveStatuses.count { it == ReconciliationStatus.UNCERTAIN },
            amountMismatchCount = effectiveStatuses.count { it == ReconciliationStatus.AMOUNT_MISMATCH },
            currencyMismatchCount = effectiveStatuses.count { it == ReconciliationStatus.CURRENCY_MISMATCH },
            dateMismatchCount = effectiveStatuses.count { it == ReconciliationStatus.DATE_MISMATCH },
            invalidMatchCount = reconciliation.lines.indices.count { index ->
                reconciliation.lines[index].matches.isNotEmpty() &&
                    effectiveStatuses[index] == ReconciliationStatus.NOT_FOUND
            },
            ambiguousDateCount = reconciliation.lines.count { it.line.dateAmbiguous },
            receiptsOutsideRecognizedDateRangeCount = receiptsOutsideRange,
            duplicateStatementLineCount = duplicateExcess(statementFingerprints),
            duplicateReceiptCount = duplicateExcess(receiptFingerprints),
            nonPositiveAmountCount = reconciliation.lines.count { it.line.amountMinor <= 0 } +
                receipts.count { it.amountMinor <= 0 },
            statementTotals = statementTotals,
            receiptTotals = receiptTotals,
            matchedReceiptTotals = matchedReceiptTotals,
            declaredTotalMinor = declaredMinor,
            declaredTotalCurrencyCode = declaredCurrency,
            declaredTotalDifferenceMinor = if (declaredMinor != null && computedDeclaredCurrency != null) {
                declaredMinor.subtract(computedDeclaredCurrency)
            } else null,
            totalCheck = totalCheck,
        )
    }

    private fun totalsByCurrency(values: List<Pair<String, Long>>): Map<String, BigInteger> = values
        .groupBy { it.first.uppercase() }
        .mapValues { (_, entries) ->
            entries.fold(BigInteger.ZERO) { total, (_, value) -> total.add(BigInteger.valueOf(value)) }
        }

    private fun duplicateExcess(values: List<String>): Int = values.groupingBy { it }.eachCount()
        .values.sumOf { count -> (count - 1).coerceAtLeast(0) }
}
