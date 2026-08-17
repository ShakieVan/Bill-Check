package de.shakie.billcheck.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReconciliationReportTest {
    @Test
    fun `verified report counts every discrepancy category without AI interpretation`() {
        val report = VerifiedReconciliationReport(
            reconciliationId = "reconciliation",
            title = "Final statement",
            languageCode = "de",
            entries = listOf(
                entry(VerifiedReconciliationReport.KIND_MATCHED, ReconciliationStatus.CORRECT),
                entry(VerifiedReconciliationReport.KIND_ACCEPTED, ReconciliationStatus.ACCEPTED),
                entry(VerifiedReconciliationReport.KIND_STATEMENT_ONLY, ReconciliationStatus.NOT_FOUND),
                entry(VerifiedReconciliationReport.KIND_STATEMENT_ONLY, ReconciliationStatus.AMOUNT_MISMATCH),
                entry(VerifiedReconciliationReport.KIND_RECEIPT_ONLY, ReconciliationStatus.NOT_FOUND),
            ),
        )

        assertEquals(1, report.correctCount)
        assertEquals(1, report.acceptedCount)
        assertEquals(1, report.amountMismatchCount)
        assertEquals(2, report.statementOnlyCount)
        assertEquals(1, report.receiptOnlyCount)
    }

    private fun entry(kind: String, status: String) = VerifiedReconciliationEntry(
        kind = kind,
        occurredAt = 0,
        description = "Entry",
        statementCheckNumber = null,
        receiptCheckNumber = null,
        statementAmountMinor = null,
        receiptAmountMinor = null,
        currencyCode = "EGP",
        status = status,
    )
}
