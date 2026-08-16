package de.shakie.billcheck.data

import de.shakie.billcheck.domain.AiDocumentType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiPromptFactoryTest {
    @Test
    fun receiptPromptRequestsVenueWithoutHotelOrAddress() {
        val prompt = GeminiPromptFactory.create(AiDocumentType.RECEIPT, "EGP")

        assertTrue(prompt.contains("only the specific restaurant"))
        assertTrue(prompt.contains("Exclude the hotel or resort name"))
        assertTrue(prompt.contains("expected currency is\nEGP"))
    }

    @Test
    fun statementPromptDoesNotContainReceiptLocationRule() {
        val prompt = GeminiPromptFactory.create(AiDocumentType.STATEMENT, "EUR")

        assertTrue(prompt.contains("every charge line"))
        assertFalse(prompt.contains("specific restaurant"))
    }
}
