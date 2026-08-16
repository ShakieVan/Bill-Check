package de.shakie.billcheck.domain

data class TipSnapshot(
    val minor: Long,
    val currencyCode: String,
)

object ReceiptSnapshotRules {
    fun tipForEdit(
        existingTipMinor: Long,
        existingTipCurrencyCode: String,
        currentDefaultTipMinor: Long,
        currentDefaultTipCurrencyCode: String,
        selected: Boolean,
    ): TipSnapshot = when {
        !selected -> TipSnapshot(0, existingTipCurrencyCode)
        existingTipMinor > 0 -> TipSnapshot(existingTipMinor, existingTipCurrencyCode)
        else -> TipSnapshot(currentDefaultTipMinor, currentDefaultTipCurrencyCode)
    }
}
