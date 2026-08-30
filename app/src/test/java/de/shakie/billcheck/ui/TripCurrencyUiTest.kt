package de.shakie.billcheck.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TripCurrencyUiTest {
    @Test
    fun `home currency is always protected first`() {
        val currency = editable("EUR", isDefault = true)

        assertEquals(
            TripCurrencyRemovalProtection.HOME,
            tripCurrencyRemovalProtection(
                currency,
                homeCurrencyCode = "eur",
                tipCurrencyCode = "EUR",
                usedCurrencyCodes = setOf("EUR"),
            ),
        )
    }

    @Test
    fun `default and tip currencies are protected`() {
        assertEquals(
            TripCurrencyRemovalProtection.DEFAULT,
            tripCurrencyRemovalProtection(editable("EGP", isDefault = true), "EUR", "EGP", emptySet()),
        )
        assertEquals(
            TripCurrencyRemovalProtection.TIP,
            tripCurrencyRemovalProtection(editable("USD"), "EUR", "usd", emptySet()),
        )
    }

    @Test
    fun `currency used by an existing receipt is protected`() {
        assertEquals(
            TripCurrencyRemovalProtection.USED,
            tripCurrencyRemovalProtection(editable("JPY"), "EUR", "USD", setOf("jpy")),
        )
    }

    @Test
    fun `unused trip currency can be deleted`() {
        assertEquals(
            TripCurrencyRemovalProtection.NONE,
            tripCurrencyRemovalProtection(editable("JPY"), "EUR", "USD", emptySet()),
        )
    }

    @Test
    fun `currency label is normalized and localized when possible`() {
        assertEquals("EUR · Euro", buildCurrencyLabel(" eur ", "Euro"))
        assertEquals("EGP", buildCurrencyLabel("egp", null))
    }

    private fun editable(code: String, isDefault: Boolean = false) = EditableTripCurrency(
        code = code,
        rate = "1",
        mode = EditableExchangeRateMode.FIXED,
        isDefault = isDefault,
    )
}
