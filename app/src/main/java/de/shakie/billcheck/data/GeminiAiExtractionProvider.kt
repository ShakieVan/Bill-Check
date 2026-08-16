package de.shakie.billcheck.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import de.shakie.billcheck.domain.AiDocumentType
import de.shakie.billcheck.domain.AiExtractionProvider
import de.shakie.billcheck.domain.AiExtractionResult
import de.shakie.billcheck.domain.ExtractedItem
import de.shakie.billcheck.domain.ExtractedReceipt
import de.shakie.billcheck.domain.ExtractedStatement
import de.shakie.billcheck.domain.ExtractedStatementLine
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GeminiAiExtractionProvider(private val context: Context) : AiExtractionProvider {
    override val id: String = "gemini"

    override suspend fun extract(
        imageUri: Uri,
        documentType: AiDocumentType,
        expectedCurrencyCode: String,
        apiKey: String,
        model: String,
    ): AiExtractionResult = withContext(Dispatchers.IO) {
        val payload = readImagePayload(imageUri)
        val request = buildRequest(documentType, expectedCurrencyCode, payload)
        val safeModel = model.trim().takeIf { MODEL_PATTERN.matches(it) }
            ?: error("Invalid Gemini model name")
        val connection = URL("$BASE_URL/$safeModel:generateContent").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 90_000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)
            connection.outputStream.bufferedWriter().use { it.write(request.toString()) }
            val responseCode = connection.responseCode
            val responseText = (if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(responseCode in 200..299) {
                parseApiError(responseCode, responseText)
            }
            parseResponse(documentType, responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildRequest(
        documentType: AiDocumentType,
        expectedCurrencyCode: String,
        payload: ImagePayload,
    ): JSONObject {
        val schema = if (documentType == AiDocumentType.RECEIPT) receiptSchema() else statementSchema()
        val prompt = GeminiPromptFactory.create(documentType, expectedCurrencyCode)
        return JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray()
                            .put(JSONObject().put("text", prompt))
                            .put(
                                JSONObject().put(
                                    "inline_data",
                                    JSONObject()
                                        .put("mime_type", payload.mimeType)
                                        .put("data", Base64.encodeToString(payload.bytes, Base64.NO_WRAP)),
                                ),
                            ),
                    ),
                ),
            )
            put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0)
                    .put("responseMimeType", "application/json")
                    .put("responseSchema", schema),
            )
        }
    }

    private fun parseResponse(type: AiDocumentType, responseText: String): AiExtractionResult {
        val response = JSONObject(responseText)
        val candidates = response.optJSONArray("candidates") ?: error("Gemini returned no candidates")
        check(candidates.length() > 0) { "Gemini returned no result" }
        val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        val text = (0 until parts.length()).asSequence()
            .map { parts.getJSONObject(it).optString("text") }
            .firstOrNull(String::isNotBlank)
            ?: error("Gemini returned no structured text")
        val json = JSONObject(text)
        return when (type) {
            AiDocumentType.RECEIPT -> AiExtractionResult.Receipt(
                ExtractedReceipt(
                    location = json.optString("location"),
                    checkNumber = json.optString("checkNumber"),
                    totalAmountText = json.optString("totalAmount"),
                    currencyCode = json.optString("currencyCode"),
                    occurredOn = json.optString("date"),
                    items = json.optJSONArray("items").toObjectList { item ->
                        ExtractedItem(item.optString("name"), item.optString("amount"))
                    },
                ),
            )
            AiDocumentType.STATEMENT -> AiExtractionResult.Statement(
                ExtractedStatement(
                    title = json.optString("title"),
                    lines = json.optJSONArray("lines").toObjectList { line ->
                        ExtractedStatementLine(
                            description = line.optString("description"),
                            checkNumber = line.optString("checkNumber"),
                            amountText = line.optString("amount"),
                            currencyCode = line.optString("currencyCode"),
                            occurredOn = line.optString("date"),
                        )
                    },
                ),
            )
        }
    }

    private fun readImagePayload(uri: Uri): ImagePayload {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        val original = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Image cannot be opened")
        if (original.size <= MAX_INLINE_BYTES) return ImagePayload(mime, original)

        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_IMAGE_EDGE) {
                val scale = MAX_IMAGE_EDGE.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1),
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        return ImagePayload(
            mimeType = "image/jpeg",
            bytes = ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 94, output)) { "Image conversion failed" }
                output.toByteArray()
            },
        )
    }

    private fun receiptSchema() = JSONObject(SCHEMA_RECEIPT)

    private fun statementSchema() = JSONObject(SCHEMA_STATEMENT)

    private fun parseApiError(responseCode: Int, response: String): String = runCatching {
        val message = JSONObject(response).getJSONObject("error").getString("message")
        "Gemini HTTP $responseCode: $message"
    }.getOrDefault("Gemini HTTP $responseCode")

    private data class ImagePayload(val mimeType: String, val bytes: ByteArray)

    private companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val MAX_INLINE_BYTES = 18 * 1024 * 1024
        const val MAX_IMAGE_EDGE = 3_072
        val MODEL_PATTERN = Regex("[A-Za-z0-9._-]{1,80}")
        val SCHEMA_RECEIPT = """
            {"type":"object","properties":{
              "location":{"type":"string","description":"Only the specific restaurant, bar, lounge, pool, or beach venue; never the hotel/resort name, city, or address"},
              "checkNumber":{"type":"string"},
              "totalAmount":{"type":"string"},"currencyCode":{"type":"string"},
              "date":{"type":"string"},"items":{"type":"array","items":{"type":"object",
              "properties":{"name":{"type":"string"},"amount":{"type":"string"}},
              "required":["name","amount"]}}},
              "required":["location","checkNumber","totalAmount","currencyCode","date","items"]}
        """
        val SCHEMA_STATEMENT = """
            {"type":"object","properties":{"title":{"type":"string"},
              "lines":{"type":"array","items":{"type":"object","properties":{
              "description":{"type":"string"},"checkNumber":{"type":"string"},
              "amount":{"type":"string"},"currencyCode":{"type":"string"},
              "date":{"type":"string"}},
              "required":["description","checkNumber","amount","currencyCode","date"]}}},
              "required":["title","lines"]}
        """
    }
}

private inline fun <T> JSONArray?.toObjectList(transform: (JSONObject) -> T): List<T> =
    if (this == null) emptyList() else (0 until length()).map { transform(getJSONObject(it)) }
