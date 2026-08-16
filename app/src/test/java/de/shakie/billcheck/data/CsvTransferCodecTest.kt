package de.shakie.billcheck.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTransferCodecTest {
    @Test
    fun roundTripPreservesOverviewAndAssignments() {
        val source = TransferPackage(
            exportedAt = 1234,
            trips = listOf(
                TransferTrip(
                    id = "trip-1",
                    name = "Utopia; Test",
                    foreignCurrencyCode = "EGP",
                    defaultExchangeRate = "55.5",
                    exchangeRateMode = "DAILY",
                    defaultTipMinor = 100,
                    defaultTipCurrencyCode = "EUR",
                    defaultTipSelected = false,
                    imageStorageMode = "ORIGINAL",
                    createdAt = 100,
                    receipts = listOf(
                        TransferReceipt(
                            id = "receipt-1",
                            occurredAt = 200,
                            location = "Beach \"West\"; Bar",
                            checkNumber = "0602",
                            foreignAmountMinor = 93996,
                            foreignCurrencyCode = "EGP",
                            exchangeRate = "55.6",
                            exactEuroCents = 1691,
                            tipMinor = 100,
                            tipCurrencyCode = "EUR",
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
                            lines = listOf(
                                TransferStatementLine(
                                    id = "line-1",
                                    occurredOn = 200,
                                    description = "Sunset Lobby",
                                    checkNumber = "602",
                                    amountMinor = 93996,
                                    currencyCode = "EGP",
                                    status = "CORRECT",
                                    acceptedWithoutReceipt = false,
                                    matchedReceiptId = "receipt-1",
                                    matchedManually = true,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val encoded = CsvTransferCodec.encode(source)
        val decoded = CsvTransferCodec.decode(encoded)

        assertTrue(encoded.startsWith("\uFEFF\"Bill Check\""))
        assertEquals("Utopia; Test", decoded.trips.single().name)
        assertEquals("Beach \"West\"; Bar", decoded.trips.single().receipts.single().location)
        assertEquals("55.6", decoded.trips.single().receipts.single().exchangeRate)
        assertEquals("receipt-1", decoded.trips.single().reconciliations.single().lines.single().matchedReceiptId)
        assertTrue(decoded.trips.single().reconciliations.single().lines.single().matchedManually)
    }
}
