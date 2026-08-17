package de.shakie.billcheck.domain

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StatementExtractionValidatorTest {
    @Test
    fun `utopia partial extraction keeps declared total for completeness failure`() {
        val validated = StatementExtractionValidator.validate(
            ExtractedStatement(
                title = "Utopia final statement",
                declaredTotalAmountText = "7404.20",
                declaredTotalCurrencyCode = "EGP",
                lines = listOf(line(amount = "313.32", check = "0015512")),
            ),
            fallbackCurrencyCode = "EGP",
            zoneId = ZoneOffset.UTC,
        )

        assertEquals(740_420L, validated.declaredTotalMinor)
        assertEquals(31_332L, validated.lines.single().amountMinor)
        assertEquals("0015512", validated.lines.single().checkNumber)
    }

    @Test
    fun `one invalid ai line rejects the complete extraction`() {
        val invalidValues = listOf("-313.32", "0", "1,044.40", "1.044,40", "NaN", "1e309", "999999999999999999999")

        invalidValues.forEach { value ->
            val error = assertThrows(StatementExtractionValidationException::class.java) {
                StatementExtractionValidator.validate(
                    ExtractedStatement(
                        title = "Existing statement must survive",
                        lines = listOf(line(amount = "313.32"), line(amount = value)),
                    ),
                    "EGP",
                    ZoneOffset.UTC,
                )
            }
            assertTrue(error.problems.any { it.contains("Line 2") })
        }
    }

    @Test
    fun `ambiguous date preserves source without guessing`() {
        val validated = StatementExtractionValidator.validate(
            ExtractedStatement(
                title = "Ambiguous",
                lines = listOf(
                    line(
                        amount = "1.00",
                        printedDate = "01/02/25",
                        normalizedDate = "",
                        ambiguous = true,
                    ),
                ),
            ),
            "EGP",
            ZoneOffset.UTC,
        )

        assertNull(validated.lines.single().occurredOn)
        assertEquals("01/02/25", validated.lines.single().sourceDateText)
        assertTrue(validated.lines.single().dateAmbiguous)
    }

    @Test
    fun `invalid calendar date is rejected instead of normalized`() {
        assertThrows(StatementExtractionValidationException::class.java) {
            StatementExtractionValidator.validate(
                ExtractedStatement(
                    title = "Bad date",
                    lines = listOf(line(amount = "1.00", normalizedDate = "2025-02-31")),
                ),
                "EGP",
                ZoneOffset.UTC,
            )
        }
    }

    private fun line(
        amount: String,
        check: String = "5512",
        printedDate: String = "26.12.24",
        normalizedDate: String = "2024-12-26",
        ambiguous: Boolean = false,
    ) = ExtractedStatementLine(
        description = "Sultana Restaurant Food",
        checkNumber = check,
        amountText = amount,
        currencyCode = "EGP",
        occurredOn = normalizedDate,
        sourceDateText = printedDate,
        dateAmbiguous = ambiguous,
    )
}
