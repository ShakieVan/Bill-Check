package de.shakie.billcheck.domain

import android.net.Uri

data class ExtractedItem(
    val name: String,
    val amountText: String,
)

data class ExtractedReceipt(
    val location: String,
    val checkNumber: String,
    val totalAmountText: String,
    val currencyCode: String,
    val occurredOn: String,
    val items: List<ExtractedItem>,
)

data class ExtractedStatementLine(
    val description: String,
    val checkNumber: String,
    val amountText: String,
    val currencyCode: String,
    val occurredOn: String,
)

data class ExtractedStatement(
    val title: String,
    val lines: List<ExtractedStatementLine>,
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
    ): AiExtractionResult
}
