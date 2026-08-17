package de.shakie.billcheck.data

internal object TransferValidator {
    fun validate(source: TransferTrip) {
        require(source.name.length <= 500) { "Trip name is too long" }
        require(validCurrency(source.foreignCurrencyCode)) { "Invalid trip currency" }
        require(source.defaultExchangeRate.toBigDecimalOrNull()?.signum() == 1) { "Invalid exchange rate" }
        require(source.receipts.map { it.id }.distinct().size == source.receipts.size) { "Duplicate receipt IDs" }
        source.receipts.forEach { receipt ->
            require(receipt.foreignAmountMinor > 0) { "Receipt amount must be positive" }
            require(validCurrency(receipt.foreignCurrencyCode)) { "Invalid receipt currency" }
            require(receipt.exchangeRate.toBigDecimalOrNull()?.signum() == 1) { "Invalid receipt exchange rate" }
            require(receipt.tipMinor >= 0 && receipt.exactEuroCents >= 0) { "Invalid receipt calculated amount" }
            require(receipt.location.length <= 2_000 && receipt.checkNumber.length <= 500) {
                "Receipt text is too long"
            }
            receipt.items.forEach { item ->
                require(item.amountMinor >= 0 && validCurrency(item.currencyCode)) { "Invalid receipt item" }
            }
        }
        source.reconciliations.forEach { reconciliation ->
            require(reconciliation.title.length <= 500) { "Statement title is too long" }
            require(reconciliation.declaredTotalMinor == null || reconciliation.declaredTotalMinor > 0) {
                "Declared statement total must be positive"
            }
            require(
                reconciliation.declaredTotalCurrencyCode == null ||
                    validCurrency(reconciliation.declaredTotalCurrencyCode),
            ) { "Invalid declared statement currency" }
            require(reconciliation.lines.map { it.id }.distinct().size == reconciliation.lines.size) {
                "Duplicate statement line IDs"
            }
            val matchedIds = reconciliation.lines.mapNotNull { it.matchedReceiptId }
            require(matchedIds.distinct().size == matchedIds.size) {
                "A receipt is assigned more than once in the same reconciliation"
            }
            reconciliation.lines.forEach { line ->
                require(line.amountMinor > 0) { "Statement line amount must be positive" }
                require(validCurrency(line.currencyCode)) { "Invalid statement line currency" }
                require(line.description.length <= 2_000 && line.checkNumber.length <= 500) {
                    "Statement line text is too long"
                }
                require(line.matchedReceiptId == null || source.receipts.any { it.id == line.matchedReceiptId }) {
                    "Statement match references an unknown receipt"
                }
            }
        }
    }

    private fun validCurrency(value: String): Boolean = Regex("[A-Z]{3}").matches(value)
}
