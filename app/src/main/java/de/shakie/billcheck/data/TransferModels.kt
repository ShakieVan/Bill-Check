package de.shakie.billcheck.data

data class TransferPackage(
    val formatVersion: Int = 1,
    val exportedAt: Long,
    val trips: List<TransferTrip>,
)

data class TransferTrip(
    val id: String,
    val name: String,
    val foreignCurrencyCode: String,
    val defaultExchangeRate: String,
    val exchangeRateMode: String,
    val defaultTipMinor: Long,
    val defaultTipCurrencyCode: String,
    val defaultTipSelected: Boolean,
    val imageStorageMode: String,
    val createdAt: Long,
    val receipts: List<TransferReceipt>,
    val reconciliations: List<TransferReconciliation>,
)

data class TransferReceipt(
    val id: String,
    val occurredAt: Long,
    val location: String,
    val checkNumber: String,
    val foreignAmountMinor: Long,
    val foreignCurrencyCode: String,
    val exchangeRate: String,
    val exactEuroCents: Long,
    val tipMinor: Long,
    val tipCurrencyCode: String,
    val imageEntry: String?,
    val imageMimeType: String?,
    val reviewState: String,
    val createdAt: Long,
    val items: List<TransferReceiptItem>,
    val imageSourceUri: String? = null,
)

data class TransferReceiptItem(
    val id: String,
    val sortPosition: Int,
    val name: String,
    val amountMinor: Long,
    val currencyCode: String,
)

data class TransferReconciliation(
    val id: String,
    val title: String,
    val statementImageEntry: String?,
    val statementImageMimeType: String?,
    val createdAt: Long,
    val lines: List<TransferStatementLine>,
    val statementImageSourceUri: String? = null,
)

data class TransferStatementLine(
    val id: String,
    val occurredOn: Long?,
    val description: String,
    val checkNumber: String,
    val amountMinor: Long,
    val currencyCode: String,
    val status: String,
    val acceptedWithoutReceipt: Boolean,
    val matchedReceiptId: String?,
    val matchedManually: Boolean,
)

data class ImportTripPreview(
    val sourceId: String,
    val name: String,
    val receiptCount: Int,
    val reconciliationCount: Int,
)

data class ImportPreview(
    val format: TransferFormat,
    val trips: List<ImportTripPreview>,
)

enum class TransferFormat { BILL_CHECK, CSV }

enum class ExportFormat { BILL_CHECK, CSV, PDF }
