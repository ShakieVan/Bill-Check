package de.shakie.billcheck.data

import de.shakie.billcheck.domain.AiDocumentType
import de.shakie.billcheck.domain.ReconciliationReceiptContext
import org.json.JSONArray
import org.json.JSONObject
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
        assertTrue(prompt.contains("not evidence or a fallback"))
        assertTrue(prompt.contains("return an empty currencyCode"))
        assertTrue(prompt.contains("printed item quantity separately"))
        assertTrue(prompt.contains("zero to three distinct candidates"))
        assertFalse(prompt.contains("transcriptLines"))
        assertTrue(prompt.contains("separately printed final amount"))
        assertTrue(prompt.contains("complete visible labelled total line"))
        assertTrue(prompt.contains("HH:mm in 24-hour format"))
        assertTrue(prompt.contains("Never infer the time"))
    }

    @Test
    fun statementPromptDoesNotContainReceiptLocationRule() {
        val prompt = GeminiPromptFactory.create(AiDocumentType.STATEMENT, "EUR")

        assertTrue(prompt.contains("every individual charge line"))
        assertFalse(prompt.contains("specific restaurant"))
        assertTrue(prompt.contains("declaredTotal"))
        assertTrue(prompt.contains("dateAmbiguous=true"))
        assertTrue(prompt.contains("Never repeat its check number"))
        assertTrue(prompt.contains("each has a separate field"))
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

    @Test
    fun receiptSchemaKeepsShapeButRemovesUnsupportedGeminiKeywords() {
        val schema = localReceiptSchema().forGeminiResponseSchema()

        assertFalse(schema.getJSONObject("properties").has("transcriptLines"))
        assertTrue(schema.getJSONObject("properties").has("time"))
        assertFalse(schema.containsKeyRecursively("additionalProperties"))
        assertFalse(schema.containsKeyRecursively("maximum"))
        assertFalse(schema.containsKeyRecursively("maxItems"))
    }

    private fun JSONObject.containsKeyRecursively(name: String): Boolean {
        if (has(name)) return true
        return keys().asSequence().any { key ->
            when (val child = opt(key)) {
                is JSONObject -> child.containsKeyRecursively(name)
                is JSONArray -> (0 until child.length()).any { index ->
                    (child.opt(index) as? JSONObject)?.containsKeyRecursively(name) == true
                }
                else -> false
            }
        }
    }
}
