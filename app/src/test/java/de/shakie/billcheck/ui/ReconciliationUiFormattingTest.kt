package de.shakie.billcheck.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReconciliationUiFormattingTest {
    @Test
    fun `display title removes repeated labeled check number`() {
        assertEquals(
            "Sultana Restaurant Food",
            statementLineDisplayTitle(
                description = "Sultana Restaurant Food · Check #0015512",
                checkNumber = "0015512",
            ),
        )
    }

    @Test
    fun `display title removes normalized number without leading zeros`() {
        assertEquals(
            "Sunset Lobby Beverage",
            statementLineDisplayTitle(
                description = "Sunset Lobby Beverage - 783",
                checkNumber = "0050783",
            ),
        )
    }

    @Test
    fun `display title does not remove a partial alphanumeric value`() {
        assertEquals(
            "Table A783B",
            statementLineDisplayTitle(
                description = "Table A783B",
                checkNumber = "0050783",
            ),
        )
    }
}
