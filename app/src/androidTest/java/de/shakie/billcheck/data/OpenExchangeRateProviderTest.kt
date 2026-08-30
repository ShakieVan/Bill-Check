package de.shakie.billcheck.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenExchangeRateProviderTest {
    @Test
    fun identicalBaseAndTargetReturnOneWithoutNetwork() = runBlocking {
        val provider = OpenExchangeRateProvider(
            InstrumentationRegistry.getInstrumentation().targetContext,
        )

        val quote = provider.latestRate("eur", "EUR")

        assertEquals("EUR", quote.baseCurrencyCode)
        assertEquals("EUR", quote.targetCurrencyCode)
        assertEquals("1", quote.targetUnitsPerBase)
        assertTrue(quote.fromCache)
    }

    @Test
    fun unsupportedBaseIsRejectedBeforeNetwork() {
        val provider = OpenExchangeRateProvider(
            InstrumentationRegistry.getInstrumentation().targetContext,
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { provider.latestRate("AAA", "EUR") }
        }
    }
}
