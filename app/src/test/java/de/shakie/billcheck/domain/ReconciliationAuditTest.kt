package de.shakie.billcheck.domain

import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.ReceiptMatchEntity
import de.shakie.billcheck.data.ReconciliationEntity
import de.shakie.billcheck.data.ReconciliationWithLines
import de.shakie.billcheck.data.StatementLineEntity
import de.shakie.billcheck.data.StatementLineWithMatches
import java.math.BigInteger
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconciliationAuditTest {
    @Test
    fun `one of eleven utopia lines cannot masquerade as complete`() {
        val reconciliation = reconciliation(
            declaredTotal = 740_420,
            lines = listOf(line("0015512", 31_332, date("2024-12-26"), matchedReceiptId = "r5512")),
        )
        val audit = ReconciliationAuditor.audit(
            reconciliation,
            listOf(receipt("r5512", "5512", 31_332, date("2024-12-26"))),
        )

        assertEquals(1, audit.recognizedLineCount)
        assertEquals(StatementTotalCheck.MISMATCH, audit.totalCheck)
        assertEquals(BigInteger.valueOf(709_088), audit.declaredTotalDifferenceMinor)
        assertEquals(BigInteger.valueOf(31_332), audit.matchedReceiptTotals["EGP"])
        assertEquals(0, audit.recognizedLinesWithoutReceiptCount)
    }

    @Test
    fun `full utopia invoice reports ten recognized lines without receipt`() {
        val amounts = listOf(31_332L, 93_996, 104_440, 37_000, 93_996, 94_914, 31_638, 73_822, 116_006, 31_638, 31_638)
        val lines = amounts.mapIndexed { index, amount ->
            line("check-$index", amount, date("2025-01-01"), if (index == 0) "r0" else null)
        }
        val audit = ReconciliationAuditor.audit(
            reconciliation(declaredTotal = 740_420, lines = lines),
            listOf(receipt("r0", "check-0", 31_332, date("2025-01-01"))),
        )

        assertEquals(11, audit.recognizedLineCount)
        assertEquals(10, audit.recognizedLinesWithoutReceiptCount)
        assertEquals(StatementTotalCheck.MATCH, audit.totalCheck)
    }

    @Test
    fun `all receipts including outside range are counted and arithmetic cannot overflow`() {
        val inside = receipt("inside", "1", Long.MAX_VALUE, date("2025-01-02"))
        val outside = receipt("outside", "2", 1, date("2024-01-01"))
        val audit = ReconciliationAuditor.audit(
            reconciliation(
                declaredTotal = null,
                lines = listOf(line("1", Long.MAX_VALUE, date("2025-01-02"))),
            ),
            listOf(inside, outside),
        )

        assertEquals(2, audit.receiptsWithoutRecognizedLineCount)
        assertEquals(1, audit.receiptsOutsideRecognizedDateRangeCount)
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), audit.receiptTotals["EGP"])
        assertEquals(StatementTotalCheck.UNAVAILABLE, audit.totalCheck)
    }

    @Test
    fun `duplicate statement and receipt entries are explicit warnings`() {
        val duplicateLine = line("5512", 31_332, date("2024-12-26"))
        val duplicateReceipt = receipt("r1", "5512", 31_332, date("2024-12-26"))
        val audit = ReconciliationAuditor.audit(
            reconciliation(null, listOf(duplicateLine, duplicateLine.copy(line = duplicateLine.line.copy(id = "l2")))),
            listOf(duplicateReceipt, duplicateReceipt.copy(id = "r2")),
        )

        assertEquals(1, audit.duplicateStatementLineCount)
        assertEquals(1, audit.duplicateReceiptCount)
    }

    @Test
    fun `manually linked unrelated receipt remains an explicit discrepancy`() {
        val audit = ReconciliationAuditor.audit(
            reconciliation(
                null,
                listOf(line("5512", 31_332, date("2024-12-26"), matchedReceiptId = "wrong")),
            ),
            listOf(receipt("wrong", "9999", 99_999, date("2024-12-26"))),
        )

        assertEquals(1, audit.invalidMatchCount)
        assertEquals(0, audit.recognizedLinesWithoutReceiptCount)
        assertEquals(0, audit.receiptsWithoutRecognizedLineCount)
    }

    private fun reconciliation(
        declaredTotal: Long?,
        lines: List<StatementLineWithMatches>,
    ) = ReconciliationWithLines(
        reconciliation = ReconciliationEntity(
            id = "reconciliation",
            tripId = "trip",
            title = "Final statement",
            statementImageUri = null,
            createdAt = 0,
            declaredTotalMinor = declaredTotal,
            declaredTotalCurrencyCode = declaredTotal?.let { "EGP" },
        ),
        lines = lines,
    )

    private fun line(
        check: String,
        amount: Long,
        occurredOn: Long,
        matchedReceiptId: String? = null,
    ): StatementLineWithMatches {
        val entity = StatementLineEntity(
            id = "line-$check",
            reconciliationId = "reconciliation",
            occurredOn = occurredOn,
            description = "Sultana",
            checkNumber = check,
            amountMinor = amount,
            currencyCode = "EGP",
            status = ReconciliationStatus.NOT_FOUND,
            acceptedWithoutReceipt = false,
        )
        return StatementLineWithMatches(
            entity,
            matchedReceiptId?.let { listOf(ReceiptMatchEntity(entity.id, it, false)) }.orEmpty(),
        )
    }

    private fun receipt(id: String, check: String, amount: Long, occurredAt: Long) = ReceiptEntity(
        id = id,
        tripId = "trip",
        occurredAt = occurredAt,
        location = "Sultana",
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

    private fun date(value: String): Long = LocalDate.parse(value)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
