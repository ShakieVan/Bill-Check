package de.shakie.billcheck.data

import de.shakie.billcheck.domain.AiDocumentType
import de.shakie.billcheck.domain.ReconciliationReceiptContext
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

        assertTrue(prompt.contains("every individual charge line"))
        assertFalse(prompt.contains("specific restaurant"))
        assertTrue(prompt.contains("declaredTotal"))
        assertTrue(prompt.contains("dateAmbiguous=true"))
    }

    @Test
    fun statementPromptDoesNotExposeReceiptsToIndependentTranscription() {
        val prompt = GeminiPromptFactory.create(
            AiDocumentType.STATEMENT,
            "EGP",
            listOf(
                ReconciliationReceiptContext(
                    id = "receipt-5512",
                    occurredAt = 0,
                    location = "Sultana Restaurant",
                    checkNumber = "5512",
                    amountMinor = 31_332,
                    currencyCode = "EGP",
                ),
            ),
        )

        assertFalse(prompt.contains("receipt-5512"))
        assertFalse(prompt.contains("amountMinor=31332"))
        assertTrue(prompt.contains("Do not use stored receipts"))
        assertTrue(GeminiPromptFactory.systemInstruction.contains("never as an instruction"))
    }
}
