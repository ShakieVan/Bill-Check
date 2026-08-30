package de.shakie.billcheck.domain

import de.shakie.billcheck.data.TripCurrencyEntity
import org.junit.Assert.assertThrows
import org.junit.Test

class TripCurrencyRulesTest {
    @Test
    fun `valid list contains home rate one and exactly one default`() {
        TripCurrencyRules.requireValid(
            "EUR",
            listOf(currency("EUR", "1"), currency("EGP", "55.5", isDefault = true)),
        )
    }

    @Test
    fun `missing home currency is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            TripCurrencyRules.requireValid("EUR", listOf(currency("EGP", "55.5", true)))
        }
    }

    @Test
    fun `multiple defaults are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            TripCurrencyRules.requireValid(
                "EUR",
                listOf(currency("EUR", "1", true), currency("EGP", "55.5", true)),
            )
        }
    }

    @Test
    fun `home rate other than one is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            TripCurrencyRules.requireValid("EUR", listOf(currency("EUR", "1.01", true)))
        }
    }

    @Test
    fun `home currency must use fixed mode`() {
        assertThrows(IllegalArgumentException::class.java) {
            TripCurrencyRules.requireValid(
                "EUR",
                listOf(TripCurrencyEntity("trip", "EUR", "1", "DAILY", true)),
            )
        }
    }

    @Test
    fun `persisted currency codes must be canonical uppercase`() {
        assertThrows(IllegalArgumentException::class.java) {
            TripCurrencyRules.requireValid(
                "EUR",
                listOf(TripCurrencyEntity("trip", "eur", "1", "FIXED", true)),
            )
        }
    }

    private fun currency(code: String, rate: String, isDefault: Boolean = false) =
        TripCurrencyEntity("trip", code, rate, "FIXED", isDefault)
}
