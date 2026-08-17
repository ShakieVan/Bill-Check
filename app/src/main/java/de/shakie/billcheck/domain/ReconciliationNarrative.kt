package de.shakie.billcheck.domain

import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.ReconciliationWithLines
import java.time.Instant
import java.time.ZoneId

enum class ReconciliationCoverage {
    EMPTY,
    NONE,
    FEW,
    SOME,
    MOST,
    ALL,
}

enum class ReconciliationNarrativeIssueKind {
    STATEMENT_WITHOUT_RECEIPT,
    RECEIPT_WITHOUT_STATEMENT,
    RECEIPT_OUTSIDE_DATE_RANGE,
    QUESTIONABLE_MATCH,
}

data class ReconciliationNarrativeIssue(
    val kind: ReconciliationNarrativeIssueKind,
    val description: String,
    val checkNumber: String,
    val occurredAt: Long?,
)

data class ReconciliationNarrativeFacts(
    val coverage: ReconciliationCoverage,
    val matchedLineCount: Int,
    val recognizedLineCount: Int,
    val openStatementCount: Int,
    val unmatchedReceiptCount: Int,
    val questionableMatchCount: Int,
    val issues: List<ReconciliationNarrativeIssue>,
    val dataWarningCount: Int,
    val totalMismatch: Boolean,
)

object ReconciliationNarrator {
    fun facts(
        reconciliation: ReconciliationWithLines,
        receipts: List<ReceiptEntity>,
    ): ReconciliationNarrativeFacts {
        val receiptById = receipts.associateBy { it.id }
        val matchedReceiptIds = reconciliation.lines.flatMap { it.matches }
            .mapTo(mutableSetOf()) { it.receiptId }
        val matchedLineCount = reconciliation.lines.count { it.matches.isNotEmpty() }
        val statementDates = reconciliation.lines.mapNotNull { it.line.occurredOn }
        val zone = ZoneId.systemDefault()
        val minDate = statementDates.minOrNull()?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        val maxDate = statementDates.maxOrNull()?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

        val statementIssues = reconciliation.lines
            .filter { it.matches.isEmpty() && !it.line.acceptedWithoutReceipt }
            .map { related ->
                ReconciliationNarrativeIssue(
                    kind = ReconciliationNarrativeIssueKind.STATEMENT_WITHOUT_RECEIPT,
                    description = related.line.description,
                    checkNumber = related.line.checkNumber,
                    occurredAt = related.line.occurredOn,
                )
            }
        val receiptIssues = receipts.filter { it.id !in matchedReceiptIds }.map { receipt ->
            val receiptDate = Instant.ofEpochMilli(receipt.occurredAt).atZone(zone).toLocalDate()
            val outsideRange = minDate != null && maxDate != null &&
                (receiptDate < minDate || receiptDate > maxDate)
            ReconciliationNarrativeIssue(
                kind = if (outsideRange) {
                    ReconciliationNarrativeIssueKind.RECEIPT_OUTSIDE_DATE_RANGE
                } else {
                    ReconciliationNarrativeIssueKind.RECEIPT_WITHOUT_STATEMENT
                },
                description = receipt.location,
                checkNumber = receipt.checkNumber,
                occurredAt = receipt.occurredAt,
            )
        }
        val questionableIssues = reconciliation.lines.mapNotNull { related ->
            val receipt = related.matches.singleOrNull()?.receiptId?.let(receiptById::get) ?: return@mapNotNull null
            val status = ReconciliationMatcher.suggestedStatus(related.line, receipt)
            if (status == ReconciliationStatus.CORRECT) return@mapNotNull null
            ReconciliationNarrativeIssue(
                kind = ReconciliationNarrativeIssueKind.QUESTIONABLE_MATCH,
                description = related.line.description,
                checkNumber = related.line.checkNumber,
                occurredAt = related.line.occurredOn,
            )
        }
        val issues = (statementIssues + receiptIssues + questionableIssues)
            .sortedWith(compareBy<ReconciliationNarrativeIssue> { it.occurredAt ?: Long.MAX_VALUE }.thenBy { it.description })
        val audit = ReconciliationAuditor.audit(reconciliation, receipts)
        val lineCount = reconciliation.lines.size
        val coverage = when {
            lineCount == 0 -> ReconciliationCoverage.EMPTY
            matchedLineCount == 0 -> ReconciliationCoverage.NONE
            matchedLineCount == lineCount -> ReconciliationCoverage.ALL
            matchedLineCount * 4 >= lineCount * 3 -> ReconciliationCoverage.MOST
            matchedLineCount * 4 >= lineCount -> ReconciliationCoverage.SOME
            else -> ReconciliationCoverage.FEW
        }
        return ReconciliationNarrativeFacts(
            coverage = coverage,
            matchedLineCount = matchedLineCount,
            recognizedLineCount = lineCount,
            openStatementCount = statementIssues.size,
            unmatchedReceiptCount = receiptIssues.size,
            questionableMatchCount = questionableIssues.size,
            issues = issues,
            dataWarningCount = audit.ambiguousDateCount + audit.duplicateStatementLineCount +
                audit.duplicateReceiptCount + audit.nonPositiveAmountCount,
            totalMismatch = audit.totalCheck == StatementTotalCheck.MISMATCH ||
                audit.totalCheck == StatementTotalCheck.CURRENCY_MISMATCH,
        )
    }
}
