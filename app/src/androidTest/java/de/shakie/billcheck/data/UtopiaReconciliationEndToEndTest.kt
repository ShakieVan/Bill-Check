package de.shakie.billcheck.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.shakie.billcheck.domain.ExtractedStatement
import de.shakie.billcheck.domain.ExtractedStatementLine
import de.shakie.billcheck.domain.ReconciliationStatus
import de.shakie.billcheck.domain.StatementTotalCheck
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UtopiaReconciliationEndToEndTest {
    @Test
    fun realUtopiaValuesMatch5512AndExposeTenMissingLinesAndOutsideReceipt() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, BillCheckDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = BillCheckRepository(database)
            val trip = repository.createTrip("Utopia", "EGP", "55.5", "FIXED")
            repository.addReceipt(
                trip = trip,
                location = "Sultana Restaurant",
                checkNumber = "5512",
                foreignAmountMinor = 31_332,
                addDefaultTip = false,
                occurredAt = date("2024-12-26"),
            )
            repository.addReceipt(
                trip = trip,
                location = "Wrong date trap",
                checkNumber = "9999",
                foreignAmountMinor = 1,
                addDefaultTip = false,
                occurredAt = date("2020-01-01"),
            )
            val reconciliation = repository.createReconciliation(trip, "Endrechnung")
            repository.applyExtractedStatement(reconciliation, utopiaStatement(), "EGP")

            val imported = repository.reconciliations(trip.id).first().single()
            val report = repository.runAutomaticReconciliation(trip.id, imported)
            val refreshed = repository.reconciliations(trip.id).first().single()
            val matched = refreshed.lines.single { it.line.checkNumber == "0015512" }

            assertEquals(11, refreshed.lines.size)
            assertEquals(740_420L, refreshed.reconciliation.declaredTotalMinor)
            assertEquals(ReconciliationStatus.CORRECT, matched.line.status)
            assertEquals(1, matched.matches.size)
            assertNotNull(refreshed.reconciliation.analysisUpdatedAt)
            assertEquals(10, report.statementOnlyCount)
            assertEquals(1, report.receiptOnlyCount)
            assertEquals(StatementTotalCheck.MATCH.name, report.totalCheck)
            assertTrue(report.entries.any { it.description == "Wrong date trap" })
        } finally {
            database.close()
        }
    }

    @Test
    fun fuzzyEarlierLineCannotStealReceiptFromExactCheckNumber() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, BillCheckDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val repository = BillCheckRepository(database)
            val trip = repository.createTrip("Trap", "EGP", "55.5", "FIXED")
            repository.addReceipt(
                trip, "Beach Bar", "5512", 10_000, false,
                occurredAt = date("2025-01-02"),
            )
            val reconciliation = repository.createReconciliation(trip, "Greedy trap")
            repository.applyExtractedStatement(
                reconciliation,
                ExtractedStatement(
                    title = "Greedy trap",
                    declaredTotalAmountText = "200.00",
                    declaredTotalCurrencyCode = "EGP",
                    lines = listOf(
                        extractedLine("2025-01-01", "5513", "100.00"),
                        extractedLine("2025-01-02", "5512", "100.00"),
                    ),
                ),
                "EGP",
            )

            repository.runAutomaticReconciliation(
                trip.id,
                repository.reconciliations(trip.id).first().single(),
            )
            val lines = repository.reconciliations(trip.id).first().single().lines

            assertTrue(lines.single { it.line.checkNumber == "5512" }.matches.isNotEmpty())
            assertTrue(lines.single { it.line.checkNumber == "5513" }.matches.isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun threeDigitReceipt783MatchesUtopiaStatement0050783WithSupportingFacts() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, BillCheckDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val repository = BillCheckRepository(database)
            val trip = repository.createTrip("Utopia", "EGP", "55.5", "FIXED")
            repository.addReceipt(
                trip = trip,
                location = "Sunset Lobby",
                checkNumber = "783",
                foreignAmountMinor = 116_006,
                addDefaultTip = false,
                occurredAt = date("2025-01-02"),
            )
            val captured = repository.receipts(trip.id).first().single()
            val reconciliation = repository.createReconciliation(trip, "Endrechnung")
            repository.applyExtractedStatement(
                reconciliation,
                ExtractedStatement(
                    title = "INFORMATION INVOICE",
                    declaredTotalAmountText = "1160.06",
                    declaredTotalCurrencyCode = "EGP",
                    lines = listOf(
                        ExtractedStatementLine(
                            description = "Sunset Lobby Beverage",
                            checkNumber = "0050783",
                            amountText = "1160.06",
                            currencyCode = "EGP",
                            occurredOn = "2025-01-02",
                            sourceDateText = "02.01.25",
                        ),
                    ),
                ),
                "EGP",
            )

            repository.runAutomaticReconciliation(
                trip.id,
                repository.reconciliations(trip.id).first().single(),
            )
            val result = repository.reconciliations(trip.id).first().single().lines.single()

            assertEquals("0050783", result.line.checkNumber)
            assertEquals(ReconciliationStatus.CORRECT, result.line.status)
            assertEquals(captured.receipt.id, result.matches.single().receiptId)
        } finally {
            database.close()
        }
    }

    private fun utopiaStatement(): ExtractedStatement {
        val rows = listOf(
            Row("2024-12-26", "Sultana Restaurant Food", "0015512", "313.32"),
            Row("2024-12-28", "Sunset Lobby Beverage", "0050602", "939.96"),
            Row("2024-12-28", "Sunset Lobby Beverage", "0050604", "1044.40"),
            Row("2024-12-28", "Guest Laundry", "0111876", "370.00"),
            Row("2024-12-30", "Sunset Lobby Beverage", "0050635", "939.96"),
            Row("2025-01-01", "Sunset Lobby Beverage", "0050768", "949.14"),
            Row("2025-01-02", "Sultana Restaurant Food", "0015568", "316.38"),
            Row("2025-01-02", "Sunset Lobby Beverage", "0050778", "738.22"),
            Row("2025-01-02", "Sunset Lobby Beverage", "0050783", "1160.06"),
            Row("2025-01-04", "Sultana Restaurant Food", "0015587", "316.38"),
            Row("2025-01-05", "Sultana Restaurant Food", "0015595", "316.38"),
        )
        return ExtractedStatement(
            title = "INFORMATION INVOICE",
            declaredTotalAmountText = "7404.20",
            declaredTotalCurrencyCode = "EGP",
            lines = rows.map { row ->
                ExtractedStatementLine(
                    description = row.description,
                    checkNumber = row.check,
                    amountText = row.amount,
                    currencyCode = "EGP",
                    occurredOn = row.date,
                    sourceDateText = row.date,
                )
            },
        )
    }

    private fun date(value: String): Long = LocalDate.parse(value)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private data class Row(val date: String, val description: String, val check: String, val amount: String)

    private fun extractedLine(date: String, check: String, amount: String) = ExtractedStatementLine(
        description = "Beach Bar",
        checkNumber = check,
        amountText = amount,
        currencyCode = "EGP",
        occurredOn = date,
        sourceDateText = date,
    )
}
