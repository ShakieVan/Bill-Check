package de.shakie.billcheck

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptEditorFormattingTest {
    @Test
    fun `quantity is shown once even when model repeats it in item name`() {
        assertEquals("5 × Cola", formatExtractedItemName("5", "Cola"))
        assertEquals("5 × Cola", formatExtractedItemName("5", "5 Cola"))
        assertEquals("5 × Cola", formatExtractedItemName("5", "5x Cola"))
        assertEquals("5 × Cola", formatExtractedItemName("5", "5 × Cola"))
        assertEquals("5.0 × Cola", formatExtractedItemName("5.0", "5 Cola"))
        assertEquals("5 × 500ml Water", formatExtractedItemName("5", "500ml Water"))
        assertEquals("5 × 5Star Hotel Water", formatExtractedItemName("5", "5Star Hotel Water"))
    }

    @Test
    fun `blank quantity leaves clean item name untouched`() {
        assertEquals("Cola - Can 330", formatExtractedItemName("", " Cola - Can 330 "))
    }
}
