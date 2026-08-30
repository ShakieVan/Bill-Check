package de.shakie.billcheck.domain

import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.ReceiptMatchEntity
import de.shakie.billcheck.data.ReconciliationEntity
import de.shakie.billcheck.data.ReconciliationWithLines
import de.shakie.billcheck.data.StatementLineEntity
import de.shakie.billcheck.data.StatementLineWithMatches
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconciliationNarrativeTest {
    @Test
    fun `twenty open lines become an extreme overview instead of twenty details`() {
        val lines = (1..20).map { index -> line("$index", "Posten $index", date("2025-01-02")) }

        val facts = ReconciliationNarrator.facts(reconciliation(lines), emptyList())

        assertEquals(ReconciliationCoverage.NONE, facts.coverage)
        assertEquals(20, facts.openStatementCount)
        assertEquals(20, facts.issues.size)
    }

    @Test
    fun `small discrepancy set retains concrete statement and outside receipt facts`() {
        val matched = line("5512", "Sultana Restaurant", date("2025-01-02"), "matched")
        val open = line("778", "Wäscherei", date("2025-01-03"))
        val receipts = listOf(
            receipt("matched", "5512", "Sultana Restaurant", date("2025-01-02")),
            receipt("outside", "99", "Sunset Lobby", date("2024-01-01")),
        )

        val facts = ReconciliationNarrator.facts(reconciliation(listOf(matched, open)), receipts)

        assertEquals(2, facts.issues.size)
        assertTrue(facts.issues.any { it.kind == ReconciliationNarrativeIssueKind.STATEMENT_WITHOUT_RECEIPT })
        assertTrue(facts.issues.any { it.kind == ReconciliationNarrativeIssueKind.RECEIPT_OUTSIDE_DATE_RANGE })
    }

    @Test
    fun `fully matched statement produces all coverage and no issues`() {
        val line = line("5512", "Sultana Restaurant", date("2025-01-02"), "matched")
        val facts = ReconciliationNarrator.facts(
            reconciliation(listOf(line)),
            listOf(receipt("matched", "5512", "Sultana Restaurant", date("2025-01-02"))),
        )

        assertEquals(ReconciliationCoverage.ALL, facts.coverage)
        assertTrue(facts.issues.isEmpty())
    }

    private fun reconciliation(lines: List<StatementLineWithMatches>) = ReconciliationWithLines(
        reconciliation = ReconciliationEntity(
            id = "reconciliation",
            tripId = "trip",
            title = "Endrechnung",
            statementImageUri = null,
            createdAt = 0,
        ),
        lines = lines,
    )

    private fun line(
        check: String,
        description: String,
        occurredOn: Long,
        receiptId: String? = null,
    ): StatementLineWithMatches {
        val entity = StatementLineEntity(
            id = "line-$check",
            reconciliationId = "reconciliation",
            occurredOn = occurredOn,
            description = description,
            checkNumber = check,
            amountMinor = 10_000,
            currencyCode = "EGP",
            status = ReconciliationStatus.NOT_FOUND,
            acceptedWithoutReceipt = false,
        )
        return StatementLineWithMatches(
            line = entity,
            matches = receiptId?.let { listOf(ReceiptMatchEntity(entity.id, it, false)) }.orEmpty(),
        )
    }

    private fun receipt(
        id: String,
        check: String,
        location: String,
        occurredAt: Long,
    ) = ReceiptEntity(
        id = id,
        tripId = "trip",
        occurredAt = occurredAt,
        location = location,
        checkNumber = check,
        amountMinor = 10_000,
        currencyCode = "EGP",
        exchangeRateSnapshot = "55.5",
        exactHomeMinor = 0,
        tipMinor = 0,
        tipCurrencyCode = "EUR",
        tipExchangeRateSnapshot = "1",
        imageUri = null,
        reviewState = "CONFIRMED",
        createdAt = 0,
    )

    private fun date(value: String): Long = LocalDate.parse(value)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
