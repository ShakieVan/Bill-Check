package de.shakie.billcheck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptAiReviewRulesTest {
    private val untouched = ReceiptEditorBaseline(
        location = "",
        checkNumber = "",
        amountText = "",
        currencyCode = "EGP",
        occurredOn = "30.08.2026",
        items = listOf("" to ""),
    )

    @Test
    fun `only a virgin unchanged editor applies an analysis automatically`() {
        val virginRequest = ReceiptAnalysisRequest(untouched, wasVirgin = true)

        assertTrue(shouldAutoApplyReceiptAnalysis(virginRequest, untouched))
        assertFalse(
            shouldAutoApplyReceiptAnalysis(
                virginRequest,
                untouched.copy(location = "Sultana Rest."),
            ),
        )
        assertFalse(
            shouldAutoApplyReceiptAnalysis(
                ReceiptAnalysisRequest(untouched, wasVirgin = false),
                untouched,
            ),
        )
    }

    @Test
    fun `only fields changed while analysis ran are protected`() {
        val current = untouched.copy(
            location = "Manuell gewählt",
            amountText = "125.50",
            items = listOf("1 × Cola" to "25.00"),
        )

        assertEquals(
            setOf(ReceiptAiField.LOCATION, ReceiptAiField.AMOUNT),
            changedReceiptAiFields(untouched, current),
        )
    }

    @Test
    fun `time edited while analysis runs is protected independently from date`() {
        val current = untouched.copy(occurredTime = "19:42")

        assertEquals(
            setOf(ReceiptAiField.TIME),
            changedReceiptAiFields(untouched, current),
        )
    }

    @Test
    fun `returning to the request value removes field protection`() {
        val changedThenRestored = untouched.copy(location = untouched.location)

        assertTrue(changedReceiptAiFields(untouched, changedThenRestored).isEmpty())
    }

    @Test
    fun `unsupported detected currency can be suppressed explicitly`() {
        assertEquals(
            setOf(ReceiptAiField.CURRENCY),
            changedReceiptAiFields(untouched, untouched, suppressCurrency = true),
        )
    }
}
