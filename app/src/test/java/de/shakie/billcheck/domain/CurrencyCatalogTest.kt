package de.shakie.billcheck.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyCatalogTest {
    private val germanEntries = CurrencyCatalog.entries(Locale.GERMAN)

    @Test
    fun `catalog contains travel currencies but not non-tender codes`() {
        val codes = germanEntries.map(CurrencyCatalogEntry::code).toSet()

        assertTrue("EUR" in codes)
        assertTrue("EGP" in codes)
        assertTrue("USD" in codes)
        assertTrue("JPY" in codes)
        assertFalse("XXX" in codes)
        assertFalse("XAU" in codes)
        assertTrue(germanEntries.all { it.regions.isNotEmpty() })
        assertTrue(germanEntries.all { it.defaultFractionDigits >= 0 })
    }

    @Test
    fun `entries are localized`() {
        val euro = germanEntries.single { it.code == "EUR" }

        assertEquals("Euro", euro.name)
        assertTrue("Deutschland" in euro.regions)
    }

    @Test
    fun `search covers code name and region ignoring case and accents`() {
        assertEquals("USD", CurrencyCatalog.search(germanEntries, "usd").single().code)
        assertEquals("EGP", CurrencyCatalog.search(germanEntries, "agypten").single().code)
        assertTrue(CurrencyCatalog.search(germanEntries, "Pfund Ägypten").any { it.code == "EGP" })
    }

    @Test
    fun `all search terms must match`() {
        val result = CurrencyCatalog.search(germanEntries, "Pfund Japan")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `preferred codes keep supplied order before alphabetical remainder`() {
        val result = CurrencyCatalog.search(
            entries = germanEntries,
            query = "",
            prioritizedCodes = listOf("jpy", "USD", "JPY"),
        )

        assertEquals(listOf("JPY", "USD"), result.take(2).map(CurrencyCatalogEntry::code))
        val withoutPreferred = CurrencyCatalog.search(germanEntries, "")
            .filterNot { it.code == "JPY" || it.code == "USD" }
        assertEquals(withoutPreferred, result.drop(2))
    }
}
