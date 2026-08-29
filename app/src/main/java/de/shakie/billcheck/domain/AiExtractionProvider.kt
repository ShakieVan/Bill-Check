package de.shakie.billcheck.domain

import android.net.Uri

data class ExtractedItem(
    val name: String,
    val amountText: String,
    /** Printed item count. Empty when no count is visible; never inferred from the price. */
    val quantityText: String = "",
    val nameSuggestions: ExtractedFieldSuggestions = ExtractedFieldSuggestions.single(name),
    val amountSuggestions: ExtractedFieldSuggestions = ExtractedFieldSuggestions.single(amountText),
    val quantitySuggestions: ExtractedFieldSuggestions = ExtractedFieldSuggestions.single(quantityText),
)

enum class AiSuggestionCertainty {
    HIGH,
    MEDIUM,
    LOW,
}

enum class SuggestionSource {
    AI,
    LOCAL_OCR,
    HISTORY,
    MANUAL_IMAGE,
}

/**
 * Coordinates normalized to the source image (0..1000 on both axes).
 *
 * AI boxes are deliberately treated as coarse evidence locations. Character-accurate geometry is
 * supplied by the on-device OCR layer and must not be inferred from these boxes.
 */
data class NormalizedBoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

data class ExtractedFieldCandidate(
    val value: String,
    val evidenceText: String,
    val certainty: AiSuggestionCertainty,
    val boundingBox: NormalizedBoundingBox? = null,
    val source: SuggestionSource = SuggestionSource.AI,
)

data class ExtractedFieldSuggestions(
    val preferred: String,
    val ambiguous: Boolean,
    val candidates: List<ExtractedFieldCandidate>,
) {
    companion object {
        fun single(value: String): ExtractedFieldSuggestions = ExtractedFieldSuggestions(
            preferred = value,
            ambiguous = false,
            candidates = if (value.isBlank()) emptyList() else listOf(
                ExtractedFieldCandidate(
                    value = value,
                    evidenceText = value,
                    certainty = AiSuggestionCertainty.HIGH,
                ),
            ),
        )
    }
}

data class ExtractedTranscriptLine(
    val text: String,
    val boundingBox: NormalizedBoundingBox,
)

data class ExtractedReceipt(
    val location: String,
    val checkNumber: String,
    val totalAmountText: String,
    val currencyCode: String,
    val occurredOn: String,
    val items: List<ExtractedItem>,
    val locationSuggestions: ExtractedFieldSuggestions = ExtractedFieldSuggestions.single(location),
    val checkNumberSuggestions: ExtractedFieldSuggestions = ExtractedFieldSuggestions.single(checkNumber),
    val totalAmountSuggestions: ExtractedFieldSuggestions = ExtractedFieldSuggestions.single(totalAmountText),
    val occurredOnSuggestions: ExtractedFieldSuggestions = ExtractedFieldSuggestions.single(occurredOn),
    val transcriptLines: List<ExtractedTranscriptLine> = emptyList(),
)

data class ExtractedStatementLine(
    val description: String,
    val checkNumber: String,
    val amountText: String,
    val currencyCode: String,
    val occurredOn: String,
    val suggestedReceiptId: String = "",
    val matchConfidence: Int = 0,
    val matchReason: String = "",
    val sourceDateText: String = "",
    val dateAmbiguous: Boolean = false,
)

data class ExtractedStatement(
    val title: String,
    val lines: List<ExtractedStatementLine>,
    val declaredTotalAmountText: String = "",
    val declaredTotalCurrencyCode: String = "",
)

sealed interface AiExtractionResult {
    data class Receipt(val value: ExtractedReceipt) : AiExtractionResult
    data class Statement(val value: ExtractedStatement) : AiExtractionResult
}

enum class AiDocumentType { RECEIPT, STATEMENT }

interface AiExtractionProvider {
    val id: String

    suspend fun extract(
        imageUri: Uri,
        documentType: AiDocumentType,
        expectedCurrencyCode: String,
        apiKey: String,
        model: String,
        receiptContext: List<ReconciliationReceiptContext> = emptyList(),
    ): AiExtractionResult

    suspend fun summarizeReconciliation(
        report: VerifiedReconciliationReport,
        apiKey: String,
        model: String,
    ): String
}
