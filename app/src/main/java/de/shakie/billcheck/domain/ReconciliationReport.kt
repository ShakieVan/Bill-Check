package de.shakie.billcheck.domain

data class ReconciliationReceiptContext(
    val id: String,
    val occurredAt: Long,
    val location: String,
    val checkNumber: String,
    val amountMinor: Long,
    val currencyCode: String,
)

data class VerifiedReconciliationEntry(
    val kind: String,
    val occurredAt: Long?,
    val description: String,
    val statementCheckNumber: String?,
    val receiptCheckNumber: String?,
    val statementAmountMinor: Long?,
    val receiptAmountMinor: Long?,
    val currencyCode: String,
    val status: String,
)

data class VerifiedReconciliationReport(
    val reconciliationId: String,
    val title: String,
    val languageCode: String,
    val entries: List<VerifiedReconciliationEntry>,
    val recognizedLineCount: Int = entries.count { it.kind != KIND_RECEIPT_ONLY },
    val declaredTotalMinor: String? = null,
    val declaredTotalCurrencyCode: String? = null,
    val declaredTotalDifferenceMinor: String? = null,
    val totalCheck: String = StatementTotalCheck.UNAVAILABLE.name,
    val auditWarnings: List<String> = emptyList(),
) {
    val correctCount: Int get() = entries.count { it.status == ReconciliationStatus.CORRECT }
    val acceptedCount: Int get() = entries.count { it.status == ReconciliationStatus.ACCEPTED }
    val uncertainCount: Int get() = entries.count { it.status == ReconciliationStatus.UNCERTAIN }
    val amountMismatchCount: Int get() = entries.count { it.status == ReconciliationStatus.AMOUNT_MISMATCH }
    val statementOnlyCount: Int get() = entries.count { it.kind == KIND_STATEMENT_ONLY }
    val receiptOnlyCount: Int get() = entries.count { it.kind == KIND_RECEIPT_ONLY }

    companion object {
        const val KIND_MATCHED = "MATCHED"
        const val KIND_STATEMENT_ONLY = "STATEMENT_ONLY"
        const val KIND_RECEIPT_ONLY = "RECEIPT_ONLY"
        const val KIND_ACCEPTED = "ACCEPTED"
    }
}
