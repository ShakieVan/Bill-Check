package de.shakie.billcheck.data

import de.shakie.billcheck.domain.MoneyCalculator
import java.util.UUID

class BillCheckRepository(database: BillCheckDatabase) {
    private val dao = database.dao()

    val trips = dao.observeTrips()

    fun receipts(tripId: String) = dao.observeReceipts(tripId)

    suspend fun createTrip(
        name: String,
        currencyCode: String,
        exchangeRate: String,
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
        occurredAt: Long = System.currentTimeMillis(),
    ) {
        val now = System.currentTimeMillis()
        val tipMinor = if (addDefaultTip) trip.defaultTipMinor else 0
        dao.insertReceipt(
            ReceiptEntity(
                id = UUID.randomUUID().toString(),
                tripId = trip.id,
                occurredAt = occurredAt,
                location = location.trim(),
                checkNumber = checkNumber.trim(),
                foreignAmountMinor = foreignAmountMinor,
                foreignCurrencyCode = trip.foreignCurrencyCode,
                exchangeRate = trip.defaultExchangeRate,
                exactEuroCents = MoneyCalculator.calculateExactEuroCents(
                    foreignAmountMinor = foreignAmountMinor,
                    exchangeRate = trip.defaultExchangeRate,
                    tipMinor = tipMinor,
                    tipCurrencyCode = trip.defaultTipCurrencyCode,
                ),
                tipMinor = tipMinor,
                tipCurrencyCode = trip.defaultTipCurrencyCode,
                imageUri = null,
                reviewState = "CONFIRMED",
                createdAt = now,
            ),
        )
    }

    suspend fun deleteReceipt(receipt: ReceiptEntity) = dao.deleteReceipt(receipt)
}
