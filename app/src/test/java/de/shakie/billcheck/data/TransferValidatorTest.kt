package de.shakie.billcheck.data

import de.shakie.billcheck.domain.MoneyCalculator
import org.junit.Assert.assertThrows
import org.junit.Test

class TransferValidatorTest {
    @Test
    fun `valid non-euro home with JPY receipt and third-currency tip is accepted`() {
        TransferValidator.validate(trip())
    }

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

    @Test
    fun `reconciliation IDs must be unique within a trip`() {
        val base = trip()

        assertThrows(IllegalArgumentException::class.java) {
            TransferValidator.validate(base.copy(reconciliations = base.reconciliations + base.reconciliations.single()))
        }
    }

    @Test
    fun `statement line IDs must be unique across reconciliations`() {
        val base = trip()
        val second = base.reconciliations.single().copy(id = "final")

        assertThrows(IllegalArgumentException::class.java) {
            TransferValidator.validate(base.copy(reconciliations = base.reconciliations + second))
        }
    }

    @Test
    fun `declared statement total and currency must be present together`() {
        val base = trip()
        val reconciliation = base.reconciliations.single()

        assertThrows(IllegalArgumentException::class.java) {
            TransferValidator.validate(
                base.copy(reconciliations = listOf(reconciliation.copy(declaredTotalCurrencyCode = null))),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            TransferValidator.validate(
                base.copy(
                    reconciliations = listOf(
                        reconciliation.copy(declaredTotalMinor = null, declaredTotalCurrencyCode = "JPY"),
                    ),
                ),
            )
        }
    }

    @Test
    fun `home currency must use fixed rate one`() {
        val base = trip()
        val currencies = base.currencies.map {
            if (it.currencyCode == base.homeCurrencyCode) it.copy(exchangeRateMode = "DAILY") else it
        }

        assertThrows(IllegalArgumentException::class.java) {
            TransferValidator.validate(base.copy(currencies = currencies))
        }
    }

    @Test
    fun `tampered home-currency receipt snapshot is rejected`() {
        val base = trip()
        val tampered = base.receipts.single().copy(
            amountMinor = 1_000,
            currencyCode = "KWD",
            exchangeRateSnapshot = "1.01",
            exactHomeMinor = 2_000,
        )

        assertThrows(IllegalArgumentException::class.java) {
            TransferValidator.validate(base.copy(receipts = listOf(tampered)))
        }
    }

    private fun trip(
        receiptAmount: Long = 1_000,
        lineAmount: Long = 1_000,
        declaredTotal: Long? = 1_000,
    ): TransferTrip {
        val exactHomeMinor = runCatching {
            MoneyCalculator.calculateExactHomeMinor(
                amountMinor = receiptAmount,
                currencyCode = "JPY",
                exchangeRateSnapshot = "500",
                tipMinor = 325,
                tipCurrencyCode = "USD",
                tipExchangeRateSnapshot = "3.25",
                homeCurrencyCode = "KWD",
            )
        }.getOrDefault(0)
        val receipt = TransferReceipt(
            id = "receipt",
            occurredAt = 0,
            location = "Sultana",
            checkNumber = "5512",
            amountMinor = receiptAmount,
            currencyCode = "JPY",
            exchangeRateSnapshot = "500",
            exactHomeMinor = exactHomeMinor,
            tipMinor = 325,
            tipCurrencyCode = "USD",
            tipExchangeRateSnapshot = "3.25",
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
            currencyCode = "JPY",
            status = "CORRECT",
            acceptedWithoutReceipt = false,
            matchedReceiptId = "receipt",
            matchedManually = false,
        )
        return TransferTrip(
            id = "trip",
            name = "Trip",
            homeCurrencyCode = "KWD",
            defaultTipMinor = 325,
            defaultTipCurrencyCode = "USD",
            defaultTipSelected = true,
            imageStorageMode = "ORIGINAL",
            createdAt = 0,
            currencies = listOf(
                TransferTripCurrency("KWD", "1", "FIXED", false),
                TransferTripCurrency("JPY", "500", "DAILY", true),
                TransferTripCurrency("USD", "3.25", "FIXED", false),
            ),
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
                    declaredTotalCurrencyCode = declaredTotal?.let { "JPY" },
                ),
            ),
        )
    }
}
