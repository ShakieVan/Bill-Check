package de.shakie.billcheck.data

import de.shakie.billcheck.domain.AiDocumentType
import de.shakie.billcheck.domain.AiExtractionResult
import de.shakie.billcheck.domain.AiSuggestionCertainty
import de.shakie.billcheck.domain.VerifiedReconciliationReport
import de.shakie.billcheck.domain.VerifiedReconciliationEntry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAiExtractionProviderTest {
    @Test
    fun `receipt request uses vision content and strict json schema`() {
        val request = buildLocalExtractionRequest(
            documentType = AiDocumentType.RECEIPT,
            expectedCurrencyCode = "EGP",
            model = "qwen3.8-27b-q8",
            mimeType = "image/jpeg",
            imageBytes = byteArrayOf(1, 2, 3),
        )

        assertEquals("qwen3.8-27b-q8", request.getString("model"))
        assertEquals("none", request.getString("reasoning_effort"))
        assertEquals(6_000, request.getInt("max_tokens"))
        val messages = request.getJSONArray("messages")
        assertEquals("system", messages.getJSONObject(0).getString("role"))
        val content = messages.getJSONObject(1).getJSONArray("content")
        assertEquals("text", content.getJSONObject(0).getString("type"))
        assertEquals("image_url", content.getJSONObject(1).getString("type"))
        assertEquals(
            "data:image/jpeg;base64,AQID",
            content.getJSONObject(1).getJSONObject("image_url").getString("url"),
        )
        val format = request.getJSONObject("response_format")
        assertEquals("json_schema", format.getString("type"))
        val jsonSchema = format.getJSONObject("json_schema")
        assertTrue(jsonSchema.getBoolean("strict"))
        val receiptSchema = jsonSchema.getJSONObject("schema")
        val properties = receiptSchema.getJSONObject("properties")
        assertEquals(3, properties.getJSONObject("location").getJSONObject("properties")
            .getJSONObject("candidates").getInt("maxItems"))
        assertTrue(properties.has("transcriptLines"))
        assertTrue(properties.getJSONObject("items").getJSONObject("items")
            .getJSONObject("properties").has("quantity"))
    }

    @Test
    fun `transcript fallback request is OCR focused and strictly structured`() {
        val request = buildLocalTranscriptRequest(
            model = "qwen3.8-27b-q8",
            mimeType = "image/jpeg",
            imageBytes = byteArrayOf(1, 2, 3),
        )

        assertEquals(4_000, request.getInt("max_tokens"))
        val prompt = request.getJSONArray("messages").getJSONObject(1)
            .getJSONArray("content").getJSONObject(0).getString("text")
        assertTrue(prompt.contains("OCR only"))
        assertTrue(prompt.contains("covered"))
        val schema = request.getJSONObject("response_format")
            .getJSONObject("json_schema").getJSONObject("schema")
        assertTrue(schema.getJSONObject("properties").has("lines"))
    }

    @Test
    fun `transcript fallback preserves visible venue and quantity lines`() {
        val content = JSONObject().put(
            "lines",
            JSONArray()
                .put(
                    JSONObject().put("text", "**Sultana Rest.**").put(
                        "bbox",
                        JSONObject().put("left", 100).put("top", 120)
                            .put("right", 430).put("bottom", 170),
                    ),
                )
                .put(
                    JSONObject().put("text", "5 Cola - Can 330").put(
                        "bbox",
                        JSONObject().put("left", 120).put("top", 500)
                            .put("right", 700).put("bottom", 550),
                    ),
                ),
        )

        val lines = parseLocalTranscriptResponse(completionResponse(content))

        assertEquals(listOf("**Sultana Rest.**", "5 Cola - Can 330"), lines.map { it.text })
        assertEquals(120, lines.first().boundingBox.top)
    }

    @Test
    fun `local receipt response is parsed and deterministically normalized`() {
        val content = JSONObject()
            .put("location", "  Sunset Lobby ")
            .put("checkNumber", "CHK 000123")
            .put("totalAmount", " 20,50 ")
            .put("currencyCode", "egp")
            .put("date", "2026-08-20")
            .put(
                "items",
                JSONArray().put(
                    JSONObject().put("name", " Coffee ").put("amount", "20.-"),
                ),
            )
        val response = completionResponse(content)

        val parsed = parseLocalExtractionResponse(AiDocumentType.RECEIPT, response)
        val normalized = normalizeLocalExtraction(parsed) as AiExtractionResult.Receipt

        assertEquals("Sunset Lobby", normalized.value.location)
        assertEquals("000123", normalized.value.checkNumber)
        assertEquals("20.50", normalized.value.totalAmountText)
        assertEquals("EGP", normalized.value.currencyCode)
        assertEquals("Coffee", normalized.value.items.single().name)
        assertEquals("20.-", normalized.value.items.single().amountText)
        assertEquals("", normalized.value.items.single().quantityText)
        assertEquals("Sunset Lobby", normalized.value.locationSuggestions.preferred)
        assertEquals(1, normalized.value.locationSuggestions.candidates.size)
    }

    @Test
    fun `structured candidates quantities and transcript geometry are parsed`() {
        val content = JSONObject()
            .put(
                "location",
                suggestedField(
                    preferred = "Sultana Rest.",
                    ambiguous = true,
                    candidates = listOf(
                        candidate("Sultana Rest.", "Sultana Rest.**", "medium", 100, 80, 430, 140),
                        candidate("Beach Club", "Beach Club***", "medium", 120, 10, 400, 60),
                    ),
                ),
            )
            .put("checkNumber", suggestedField("5595", false, listOf(candidate("5595", "Check 5595"))))
            .put("totalAmount", suggestedField("100.00", false, listOf(candidate("100.00", "100.00"))))
            .put("currencyCode", "EGP")
            .put("date", suggestedField("2026-08-20", false, listOf(candidate("2026-08-20", "20/08/26"))))
            .put(
                "items",
                JSONArray().put(
                    JSONObject()
                        .put("quantity", suggestedField("5", false, listOf(candidate("5", "5"))))
                        .put(
                            "name",
                            suggestedField(
                                "Cola - Can 330",
                                false,
                                listOf(candidate("Cola - Can 330", "Cola - Can 330")),
                            ),
                        )
                        .put(
                            "amount",
                            suggestedField("100.00", false, listOf(candidate("100.00", "100.00"))),
                        ),
                ),
            )
            .put(
                "transcriptLines",
                JSONArray().put(
                    JSONObject()
                        .put("text", "Sultana Rest.**")
                        .put("bbox", bbox(100, 80, 430, 140)),
                ),
            )

        val parsed = parseLocalExtractionResponse(
            AiDocumentType.RECEIPT,
            completionResponse(content),
        ) as AiExtractionResult.Receipt

        assertEquals("Sultana Rest.", parsed.value.location)
        assertTrue(parsed.value.locationSuggestions.ambiguous)
        assertEquals(listOf("Sultana Rest.", "Beach Club"),
            parsed.value.locationSuggestions.candidates.map { it.value })
        assertEquals("Sultana Rest.**",
            parsed.value.locationSuggestions.candidates.first().evidenceText)
        assertEquals(100, parsed.value.locationSuggestions.candidates.first().boundingBox?.left)
        assertEquals("5", parsed.value.items.single().quantityText)
        assertEquals("Cola - Can 330", parsed.value.items.single().nameSuggestions.preferred)
        assertEquals("Sultana Rest.**", parsed.value.transcriptLines.single().text)
        assertEquals(430, parsed.value.transcriptLines.single().boundingBox.right)
    }

    @Test
    fun `all zero candidate box means unavailable geometry`() {
        val parsed = parseReceiptExtraction(
            JSONObject()
                .put("location", suggestedField("Lobby", false, listOf(candidate("Lobby", "Lobby"))))
                .put("checkNumber", suggestedField("", false, emptyList()))
                .put("totalAmount", suggestedField("", false, emptyList()))
                .put("currencyCode", "EGP")
                .put("date", suggestedField("", false, emptyList()))
                .put("items", JSONArray())
                .put("transcriptLines", JSONArray()),
        )

        assertNull(parsed.locationSuggestions.candidates.single().boundingBox)
        assertFalse(parsed.locationSuggestions.ambiguous)
    }

    @Test
    fun `preferred value without matching candidate is retained as low confidence fallback`() {
        val response = JSONObject()
            .put("location", suggestedField("Lobby", false, listOf(candidate("Beach", "Beach"))))
            .put("checkNumber", suggestedField("", false, emptyList()))
            .put("totalAmount", suggestedField("", false, emptyList()))
            .put("currencyCode", "EGP")
            .put("date", suggestedField("", false, emptyList()))
            .put("items", JSONArray())
            .put("transcriptLines", JSONArray())

        val parsed = parseReceiptExtraction(response)

        assertEquals("Lobby", parsed.location)
        assertEquals(listOf("Lobby", "Beach"), parsed.locationSuggestions.candidates.map { it.value })
        assertEquals(AiSuggestionCertainty.LOW, parsed.locationSuggestions.candidates.first().certainty)
    }

    @Test
    fun `candidate whitespace case and duplicates are normalized without losing receipt`() {
        val response = JSONObject()
            .put(
                "location",
                suggestedField(
                    " Sultana Rest. ",
                    true,
                    listOf(
                        candidate("sultana rest.", " Sultana Rest.** "),
                        candidate(" SULTANA REST. ", "SULTANA REST."),
                    ),
                ),
            )
            .put("checkNumber", suggestedField("", false, emptyList()))
            .put("totalAmount", suggestedField("", false, emptyList()))
            .put("currencyCode", "EGP")
            .put("date", suggestedField("", false, emptyList()))
            .put("items", JSONArray())
            .put("transcriptLines", JSONArray())

        val parsed = parseReceiptExtraction(response)

        assertEquals("Sultana Rest.", parsed.location)
        assertEquals(1, parsed.locationSuggestions.candidates.size)
        assertFalse(parsed.locationSuggestions.ambiguous)
    }

    @Test
    fun `incomplete completion is rejected`() {
        val response = JSONObject().put(
            "choices",
            JSONArray().put(
                JSONObject()
                    .put("finish_reason", "length")
                    .put("message", JSONObject().put("content", "{}")),
            ),
        ).toString()

        val error = runCatching { localAiResponseContent(response) }.exceptionOrNull()

        assertTrue(error?.message.orEmpty().contains("incomplete"))
    }

    @Test
    fun `summary prompt forbids positive verdicts while discrepancies remain`() {
        val report = VerifiedReconciliationReport(
            reconciliationId = "test",
            title = "Test",
            languageCode = "de",
            recognizedLineCount = 3,
            declaredTotalMinor = null,
            declaredTotalCurrencyCode = null,
            declaredTotalDifferenceMinor = null,
            totalCheck = "UNAVAILABLE",
            auditWarnings = emptyList(),
            entries = listOf(
                VerifiedReconciliationEntry(
                    kind = VerifiedReconciliationReport.KIND_STATEMENT_ONLY,
                    occurredAt = null,
                    description = "Open charge",
                    statementCheckNumber = "1",
                    receiptCheckNumber = null,
                    statementAmountMinor = 100,
                    receiptAmountMinor = null,
                    currencyCode = "EGP",
                    status = "NOT_FOUND",
                ),
                VerifiedReconciliationEntry(
                    kind = VerifiedReconciliationReport.KIND_RECEIPT_ONLY,
                    occurredAt = null,
                    description = "Open receipt",
                    statementCheckNumber = null,
                    receiptCheckNumber = "2",
                    statementAmountMinor = null,
                    receiptAmountMinor = 200,
                    currencyCode = "EGP",
                    status = "NOT_FOUND",
                ),
            ),
        )

        val request = buildLocalSummaryRequest(report, "qwen3.8-27b-q8")
        val prompt = request.getJSONArray("messages").getJSONObject(0).getString("content")
            .replace(Regex("\\s+"), " ")

        assertTrue(prompt.contains("Never call the overall reconciliation correct"))
        assertTrue(prompt.contains("Do not invent causes"))
    }

    private fun completionResponse(content: JSONObject): String = JSONObject().put(
        "choices",
        JSONArray().put(
            JSONObject()
                .put("finish_reason", "stop")
                .put("message", JSONObject().put("content", content.toString())),
        ),
    ).toString()

    private fun suggestedField(
        preferred: String,
        ambiguous: Boolean,
        candidates: List<JSONObject>,
    ): JSONObject = JSONObject()
        .put("preferred", preferred)
        .put("ambiguous", ambiguous)
        .put("candidates", JSONArray(candidates))

    private fun candidate(
        value: String,
        evidence: String,
        certainty: String = "high",
        left: Int = 0,
        top: Int = 0,
        right: Int = 0,
        bottom: Int = 0,
    ): JSONObject = JSONObject()
        .put("value", value)
        .put("evidenceText", evidence)
        .put("certainty", certainty)
        .put("bbox", bbox(left, top, right, bottom))

    private fun bbox(left: Int, top: Int, right: Int, bottom: Int): JSONObject = JSONObject()
        .put("left", left)
        .put("top", top)
        .put("right", right)
        .put("bottom", bottom)
}
