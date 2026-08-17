package de.shakie.billcheck.data

import org.junit.Assert.assertThrows
import org.junit.Test

class TransferValidatorTest {
    @Test
    fun `import rejects zero negative and extreme negative receipt amounts`() {
        listOf(0L, -1L, Long.MIN_VALUE).forEach { amount ->
            assertThrows(IllegalArgumentException::class.java) {
                TransferValidator.validate(trip(receiptAmount = amount))
            }
        }
    }

    @Test
    fun `import rejects nonpositive statement lines and declared totals`() {
        assertThrows(IllegalArgumentException::class.java) {
            TransferValidator.validate(trip(lineAmount = -1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            TransferValidator.validate(trip(declaredTotal = 0))
        }
    }

    @Test
    fun `same receipt cannot be assigned twice inside one reconciliation`() {
        val base = trip()
        val original = base.reconciliations.single().lines.single()
        val duplicate = original.copy(id = "line-2")

        assertThrows(IllegalArgumentException::class.java) {
            TransferValidator.validate(
                base.copy(
                    reconciliations = listOf(
                        base.reconciliations.single().copy(lines = listOf(original, duplicate)),
                    ),
                ),
            )
        }
    }

    @Test
    fun `same receipt may be audited in interim and final reconciliation`() {
        val base = trip()
        val second = base.reconciliations.single().copy(
            id = "final",
            lines = listOf(base.reconciliations.single().lines.single().copy(id = "final-line")),
        )

        TransferValidator.validate(base.copy(reconciliations = base.reconciliations + second))
    }

    private fun trip(
        receiptAmount: Long = 31_332,
        lineAmount: Long = 31_332,
        declaredTotal: Long? = 31_332,
    ): TransferTrip {
        val receipt = TransferReceipt(
            id = "receipt",
            occurredAt = 0,
            location = "Sultana",
            checkNumber = "5512",
            foreignAmountMinor = receiptAmount,
            foreignCurrencyCode = "EGP",
            exchangeRate = "55.5",
            exactEuroCents = 0,
            tipMinor = 0,
            tipCurrencyCode = "EUR",
            imageEntry = null,
            imageMimeType = null,
            reviewState = "CONFIRMED",
            createdAt = 0,
            items = emptyList(),
        )
        val line = TransferStatementLine(
            id = "line",
            occurredOn = 0,
            description = "Sultana",
            checkNumber = "0015512",
            amountMinor = lineAmount,
            currencyCode = "EGP",
            status = "CORRECT",
            acceptedWithoutReceipt = false,
            matchedReceiptId = "receipt",
            matchedManually = false,
        )
        return TransferTrip(
            id = "trip",
            name = "Trip",
            foreignCurrencyCode = "EGP",
            defaultExchangeRate = "55.5",
            exchangeRateMode = "FIXED",
            defaultTipMinor = 0,
            defaultTipCurrencyCode = "EUR",
            defaultTipSelected = false,
            imageStorageMode = "ORIGINAL",
            createdAt = 0,
            receipts = listOf(receipt),
            reconciliations = listOf(
                TransferReconciliation(
                    id = "reconciliation",
                    title = "Final",
                    statementImageEntry = null,
                    statementImageMimeType = null,
                    createdAt = 0,
                    lines = listOf(line),
                    declaredTotalMinor = declaredTotal,
                    declaredTotalCurrencyCode = declaredTotal?.let { "EGP" },
                ),
            ),
        )
    }
}
