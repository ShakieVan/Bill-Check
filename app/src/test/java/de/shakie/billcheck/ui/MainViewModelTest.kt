package de.shakie.billcheck.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class MainViewModelTest {
    @Test
    fun `decimal input accepts German and English separators`() {
        assertEquals(1234L, MainViewModel.parseMinor("12,34"))
        assertEquals(1234L, MainViewModel.parseMinor("12.34"))
    }

    @Test
    fun `decimal input rejects precision beyond currency minor units`() {
        assertNull(MainViewModel.parseMinor("1,235"))
    }

    @Test
    fun `blank input is rejected`() {
        assertNull(MainViewModel.parseMinor("  "))
    }

    @Test
    fun `item sum becomes receipt total when total is blank`() {
        val input = MainViewModel.parseReceiptInput(
            totalText = "",
            drafts = listOf(
                ReceiptItemDraft("Coffee", "120,50"),
                ReceiptItemDraft("Water", "30,00"),
            ),
        )

        assertEquals(15_050L, input?.totalMinor)
        assertEquals(listOf("Coffee", "Water"), input?.items?.map { it.name })
    }

    @Test
    fun `explicit total may differ from item sum`() {
        val input = MainViewModel.parseReceiptInput(
            totalText = "160,00",
            drafts = listOf(ReceiptItemDraft("Food", "150,00")),
        )

        assertEquals(16_000L, input?.totalMinor)
        assertEquals(15_000L, input?.items?.single()?.amountMinor)
    }

    @Test
    fun `partially filled item is rejected`() {
        assertNull(
            MainViewModel.parseReceiptInput(
                totalText = "100,00",
                drafts = listOf(ReceiptItemDraft("Coffee", "")),
            ),
        )
    }

    @Test
    fun `receipt date accepts localized and extracted ISO formats`() {
        val localized = MainViewModel.parseReceiptDate("26.12.2024")
        val extracted = MainViewModel.parseReceiptDate("2024-12-26")

        assertEquals(localized, extracted)
        assertEquals(
            "2024-12-26",
            Instant.ofEpochMilli(requireNotNull(localized))
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString(),
        )
    }

    @Test
    fun `invalid receipt date is rejected`() {
        assertNull(MainViewModel.parseReceiptDate("31.02.2024"))
    }

    @Test
    fun `receipt date and time are stored in the same local timestamp`() {
        val occurredAt = MainViewModel.parseReceiptDateTime("26.12.2024", "19:42")
        val local = Instant.ofEpochMilli(requireNotNull(occurredAt)).atZone(ZoneId.systemDefault())

        assertEquals("2024-12-26", local.toLocalDate().toString())
        assertEquals("19:42", local.toLocalTime().toString())
        assertNull(MainViewModel.parseReceiptDateTime("26.12.2024", "24:00"))
        assertNull(MainViewModel.parseReceiptDateTime("26.12.2024", "9:7"))
    }
}
