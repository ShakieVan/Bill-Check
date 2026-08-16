package de.shakie.billcheck.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainViewModelTest {
    @Test
    fun `decimal input accepts German and English separators`() {
        assertEquals(1234L, MainViewModel.parseMinor("12,34"))
        assertEquals(1234L, MainViewModel.parseMinor("12.34"))
    }

    @Test
    fun `decimal input rounds to currency minor units`() {
        assertEquals(124L, MainViewModel.parseMinor("1,235"))
    }

    @Test
    fun `blank input is rejected`() {
        assertNull(MainViewModel.parseMinor("  "))
    }
}
