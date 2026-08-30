package de.shakie.billcheck.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyAmountTest {
    @Test
    fun `OCR amount normalization respects ISO fraction digits`() {
        assertEquals("1234", CurrencyAmount.normalizeOcrMajorText("1.234", "JPY"))
        assertEquals("12.34", CurrencyAmount.normalizeOcrMajorText("12,34 €", "EUR"))
        assertEquals("12.345", CurrencyAmount.normalizeOcrMajorText("KWD 12.345", "KWD"))
    }
    @Test
    fun `fraction digits follow ISO currency metadata`() {
        assertEquals(0, CurrencyAmount.fractionDigits("JPY"))
        assertEquals(2, CurrencyAmount.fractionDigits("EUR"))
        assertEquals(3, CurrencyAmount.fractionDigits("KWD"))
    }

    @Test
    fun `parser accepts comma or point without grouping ambiguity`() {
        assertEquals(1234L, CurrencyAmount.parseMajorToMinor("12,34", "EUR"))
        assertEquals(1234L, CurrencyAmount.parseMajorToMinor("12.34", "EUR"))
        assertEquals(12L, CurrencyAmount.parseMajorToMinor("12", "JPY"))
        assertEquals(12_345L, CurrencyAmount.parseMajorToMinor("12.345", "KWD"))
    }

    @Test
    fun `parser rejects grouping and excess precision instead of rounding`() {
        assertNull(CurrencyAmount.parseMajorToMinor("1.234", "JPY"))
        assertNull(CurrencyAmount.parseMajorToMinor("1.234,56", "EUR"))
        assertNull(CurrencyAmount.parseMajorToMinor("1,234.56", "EUR"))
        assertNull(CurrencyAmount.parseMajorToMinor("1 234,56", "EUR"))
        assertNull(CurrencyAmount.parseMajorToMinor("1.2345", "KWD"))
    }

    @Test
    fun `formatter uses currency-specific scale`() {
        val kwd = CurrencyAmount.formatMinor(1_234, "KWD", Locale.GERMANY)
        val jpy = CurrencyAmount.formatMinor(1_234, "JPY", Locale.GERMANY)
        assertTrue(kwd.startsWith("1,234"))
        assertTrue(kwd.endsWith("KWD"))
        assertTrue(jpy.startsWith("1.234"))
    }
}
