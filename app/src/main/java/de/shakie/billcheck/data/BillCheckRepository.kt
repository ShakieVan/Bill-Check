package de.shakie.billcheck.data

import de.shakie.billcheck.domain.MoneyCalculator
import java.util.UUID

data class NewReceiptItem(
    val name: String,
    val amountMinor: Long,
)

class BillCheckRepository(database: BillCheckDatabase) {
    private val dao = database.dao()

    val trips = dao.observeTrips()

    fun receipts(tripId: String) = dao.observeReceipts(tripId)

    suspend fun createTrip(
        name: String,
        currencyCode: String,
        exchangeRate: String,
        exchangeRateMode: String,
        defaultTipMinor: Long = 100,
        defaultTipCurrencyCode: String = "EUR",
    ): TripEntity {
        val now = System.currentTimeMillis()
        return TripEntity(
            id = UUID.randomUUID().toString(),
            sortPosition = dao.nextTripPosition(),
            name = name.trim().ifBlank { "Reise 1" },
            foreignCurrencyCode = currencyCode,
            defaultExchangeRate = exchangeRate,
            exchangeRateMode = exchangeRateMode,
            defaultTipMinor = defaultTipMinor,
            defaultTipCurrencyCode = defaultTipCurrencyCode,
            defaultTipSelected = false,
            imageStorageMode = "ORIGINAL",
            createdAt = now,
        ).also { dao.insertTrip(it) }
    }

    suspend fun addReceipt(
        trip: TripEntity,
        location: String,
        checkNumber: String,
        foreignAmountMinor: Long,
        addDefaultTip: Boolean,
        items: List<NewReceiptItem> = emptyList(),
        exchangeRate: String = trip.defaultExchangeRate,
        imageUri: String? = null,
        occurredAt: Long = System.currentTimeMillis(),
    ) {
        val now = System.currentTimeMillis()
        val tipMinor = if (addDefaultTip) trip.defaultTipMinor else 0
        val receiptId = UUID.randomUUID().toString()
        val receipt = ReceiptEntity(
                id = receiptId,
                tripId = trip.id,
                occurredAt = occurredAt,
                location = location.trim(),
                checkNumber = checkNumber.trim(),
                foreignAmountMinor = foreignAmountMinor,
                foreignCurrencyCode = trip.foreignCurrencyCode,
                exchangeRate = exchangeRate,
                exactEuroCents = MoneyCalculator.calculateExactEuroCents(
                    foreignAmountMinor = foreignAmountMinor,
                    exchangeRate = exchangeRate,
                    tipMinor = tipMinor,
                    tipCurrencyCode = trip.defaultTipCurrencyCode,
                ),
                tipMinor = tipMinor,
                tipCurrencyCode = trip.defaultTipCurrencyCode,
                imageUri = imageUri,
                reviewState = "CONFIRMED",
                createdAt = now,
            )
        val receiptItems = items.mapIndexed { index, item ->
            ReceiptItemEntity(
                id = UUID.randomUUID().toString(),
                receiptId = receiptId,
                sortPosition = index,
                name = item.name.trim(),
                amountMinor = item.amountMinor,
                currencyCode = trip.foreignCurrencyCode,
            )
        }
        dao.insertReceiptWithItems(
            receipt = receipt,
            items = receiptItems,
        )
    }

    suspend fun deleteReceipt(receipt: ReceiptEntity) = dao.deleteReceipt(receipt)

    suspend fun updateReceipt(
        trip: TripEntity,
        existing: ReceiptEntity,
        location: String,
        checkNumber: String,
        foreignAmountMinor: Long,
        addDefaultTip: Boolean,
        items: List<NewReceiptItem>,
    ) {
        val tipMinor = if (addDefaultTip) trip.defaultTipMinor else 0
        val receipt = existing.copy(
            location = location.trim(),
            checkNumber = checkNumber.trim(),
            foreignAmountMinor = foreignAmountMinor,
            exactEuroCents = MoneyCalculator.calculateExactEuroCents(
                foreignAmountMinor = foreignAmountMinor,
                exchangeRate = existing.exchangeRate,
                tipMinor = tipMinor,
                tipCurrencyCode = trip.defaultTipCurrencyCode,
            ),
            tipMinor = tipMinor,
            tipCurrencyCode = trip.defaultTipCurrencyCode,
        )
        val receiptItems = items.mapIndexed { index, item ->
            ReceiptItemEntity(
                id = UUID.randomUUID().toString(),
                receiptId = existing.id,
                sortPosition = index,
                name = item.name.trim(),
                amountMinor = item.amountMinor,
                currencyCode = existing.foreignCurrencyCode,
            )
        }
        dao.updateReceiptWithItems(receipt, receiptItems)
    }

    suspend fun updateReceiptImage(receiptId: String, imageUri: String?) =
        dao.updateReceiptImage(receiptId, imageUri)
}
