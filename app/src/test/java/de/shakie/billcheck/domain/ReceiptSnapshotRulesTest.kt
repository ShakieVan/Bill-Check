package de.shakie.billcheck.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptSnapshotRulesTest {
    @Test
    fun `selected existing tip keeps its historical amount and currency`() {
        val result = ReceiptSnapshotRules.tipForEdit(
            existingTipMinor = 100,
            existingTipCurrencyCode = "EUR",
            currentDefaultTipMinor = 200,
            currentDefaultTipCurrencyCode = "EGP",
            selected = true,
        )

        assertEquals(TipSnapshot(100, "EUR"), result)
    }

    @Test
    fun `deselected existing tip becomes zero in its historical currency`() {
        val result = ReceiptSnapshotRules.tipForEdit(
            existingTipMinor = 100,
            existingTipCurrencyCode = "EUR",
            currentDefaultTipMinor = 200,
            currentDefaultTipCurrencyCode = "EGP",
            selected = false,
        )

        assertEquals(TipSnapshot(0, "EUR"), result)
    }

    @Test
    fun `newly selected tip uses current trip default when receipt had no tip`() {
        val result = ReceiptSnapshotRules.tipForEdit(
            existingTipMinor = 0,
            existingTipCurrencyCode = "EUR",
            currentDefaultTipMinor = 200,
            currentDefaultTipCurrencyCode = "EGP",
            selected = true,
        )

        assertEquals(TipSnapshot(200, "EGP"), result)
    }
}
