package de.shakie.billcheck.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTransferCodecTest {
    @Test
    fun roundTripPreservesGenericCurrenciesSnapshotsDefaultsAndAssignments() {
        val source = TransferPackage(
            exportedAt = 1234,
            trips = listOf(
                TransferTrip(
                    id = "trip-1",
                    name = "Utopia; Test",
                    homeCurrencyCode = "KWD",
                    defaultTipMinor = 325,
                    defaultTipCurrencyCode = "USD",
                    defaultTipSelected = true,
                    imageStorageMode = "ORIGINAL",
                    createdAt = 100,
                    currencies = listOf(
                        TransferTripCurrency("KWD", "1", "FIXED", false),
                        TransferTripCurrency("JPY", "500", "DAILY", true),
                        TransferTripCurrency("USD", "3.25", "FIXED", false),
                    ),
                    receipts = listOf(
                        TransferReceipt(
                            id = "receipt-1",
                            occurredAt = 200,
                            location = "=HYPERLINK(\"https://invalid.example\")",
                            checkNumber = "0602",
                            amountMinor = 1_000,
                            currencyCode = "JPY",
                            exchangeRateSnapshot = "500",
                            exactHomeMinor = 3_000,
                            tipMinor = 325,
                            tipCurrencyCode = "USD",
                            tipExchangeRateSnapshot = "3.25",
                            imageEntry = null,
                            imageMimeType = null,
                            reviewState = "CONFIRMED",
                            createdAt = 210,
                            items = emptyList(),
                        ),
                    ),
                    reconciliations = listOf(
                        TransferReconciliation(
                            id = "reconciliation-1",
                            title = "Final bill",
                            statementImageEntry = null,
                            statementImageMimeType = null,
                            createdAt = 300,
                            analysisSummary = "One discrepancy remains.",
                            analysisUpdatedAt = 400,
                            declaredTotalMinor = 1_000,
                            declaredTotalCurrencyCode = "JPY",
                            lines = listOf(
                                TransferStatementLine(
                                    id = "line-1",
                                    occurredOn = 200,
                                    description = "Sunset Lobby",
                                    checkNumber = "602",
                                    amountMinor = 1_000,
                                    currencyCode = "JPY",
                                    status = "CORRECT",
                                    acceptedWithoutReceipt = false,
                                    matchedReceiptId = "receipt-1",
                                    matchedManually = true,
                                    aiSuggestedReceiptId = "receipt-1",
                                    aiConfidence = 96,
                                    aiReason = "Same check and amount",
                                    sourceDateText = "28.12.24",
                                    dateAmbiguous = true,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val encoded = CsvTransferCodec.encode(source)
        val decoded = CsvTransferCodec.decode(encoded)
        val trip = decoded.trips.single()
        val receipt = trip.receipts.single()

        assertTrue(encoded.startsWith("\uFEFF\"Bill Check\""))
        assertEquals("Utopia; Test", trip.name)
        assertEquals("KWD", trip.homeCurrencyCode)
        assertEquals(true, trip.defaultTipSelected)
        assertEquals("USD", trip.defaultTipCurrencyCode)
        assertEquals(listOf("KWD", "JPY", "USD"), trip.currencies.map { it.currencyCode })
        assertEquals(true, trip.currencies.single { it.currencyCode == "JPY" }.isDefault)
        assertTrue(encoded.contains("'=HYPERLINK"))
        assertEquals("=HYPERLINK(\"https://invalid.example\")", receipt.location)
        assertEquals("JPY", receipt.currencyCode)
        assertEquals("500", receipt.exchangeRateSnapshot)
        assertEquals("USD", receipt.tipCurrencyCode)
        assertEquals("3.25", receipt.tipExchangeRateSnapshot)
        assertEquals(3_000L, receipt.exactHomeMinor)
        assertEquals("receipt-1", trip.reconciliations.single().lines.single().matchedReceiptId)
        assertTrue(trip.reconciliations.single().lines.single().matchedManually)
        assertEquals("One discrepancy remains.", trip.reconciliations.single().analysisSummary)
        assertEquals(400L, trip.reconciliations.single().analysisUpdatedAt)
        assertEquals(96, trip.reconciliations.single().lines.single().aiConfidence)
        assertEquals(1_000L, trip.reconciliations.single().declaredTotalMinor)
        assertEquals("JPY", trip.reconciliations.single().declaredTotalCurrencyCode)
    }
}
