package de.shakie.billcheck.data

import de.shakie.billcheck.domain.MoneyCalculator
import de.shakie.billcheck.domain.RankedReceiptCandidate
import de.shakie.billcheck.domain.ReconciliationMatcher
import de.shakie.billcheck.domain.ReconciliationStatus
import java.util.UUID

data class NewReceiptItem(
    val name: String,
    val amountMinor: Long,
)

data class NewStatementLine(
    val description: String,
    val checkNumber: String,
    val amountMinor: Long,
    val currencyCode: String,
    val occurredOn: Long? = null,
)

class BillCheckRepository(database: BillCheckDatabase) {
    private val dao = database.dao()

    val trips = dao.observeTrips()

    fun receipts(tripId: String) = dao.observeReceipts(tripId)

    fun locationSuggestions(tripId: String) = dao.observeLocationSuggestions(tripId)

    fun itemNameSuggestions(tripId: String) = dao.observeItemNameSuggestions(tripId)

    fun reconciliations(tripId: String) = dao.observeReconciliations(tripId)

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

    suspend fun updateTrip(
        existing: TripEntity,
        name: String,
        currencyCode: String,
        exchangeRate: String,
        exchangeRateMode: String,
        defaultTipMinor: Long,
        defaultTipCurrencyCode: String,
    ) {
        dao.updateTrip(
            existing.copy(
                name = name.trim().ifBlank { existing.name },
                foreignCurrencyCode = currencyCode,
                defaultExchangeRate = exchangeRate,
                exchangeRateMode = exchangeRateMode,
                defaultTipMinor = defaultTipMinor,
                defaultTipCurrencyCode = defaultTipCurrencyCode,
            ),
        )
    }

    suspend fun reorderTrips(orderedTripIds: List<String>) =
        dao.replaceTripOrder(orderedTripIds)

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

    suspend fun createReconciliation(
        trip: TripEntity,
        title: String,
        imageUri: String? = null,
    ): ReconciliationEntity {
        val now = System.currentTimeMillis()
        return ReconciliationEntity(
            id = UUID.randomUUID().toString(),
            tripId = trip.id,
            title = title.trim().ifBlank { "Rechnung" },
            statementImageUri = imageUri,
            createdAt = now,
        ).also { dao.insertReconciliation(it) }
    }

    suspend fun updateReconciliation(
        reconciliationId: String,
        title: String,
        imageUri: String?,
    ) = dao.updateReconciliation(reconciliationId, title.trim().ifBlank { "Rechnung" }, imageUri)

    suspend fun addStatementLine(
        reconciliationId: String,
        input: NewStatementLine,
    ) {
        dao.insertStatementLine(
            StatementLineEntity(
                id = UUID.randomUUID().toString(),
                reconciliationId = reconciliationId,
                occurredOn = input.occurredOn,
                description = input.description.trim(),
                checkNumber = input.checkNumber.trim(),
                amountMinor = input.amountMinor,
                currencyCode = input.currencyCode,
                status = ReconciliationStatus.NOT_FOUND,
                acceptedWithoutReceipt = false,
            ),
        )
    }

    suspend fun updateStatementLine(
        existing: StatementLineEntity,
        input: NewStatementLine,
    ) {
        dao.deleteStatementLineMatch(existing.id)
        dao.updateStatementLine(
            existing.copy(
                occurredOn = input.occurredOn,
                description = input.description.trim(),
                checkNumber = input.checkNumber.trim(),
                amountMinor = input.amountMinor,
                currencyCode = input.currencyCode,
                status = ReconciliationStatus.NOT_FOUND,
                acceptedWithoutReceipt = false,
            ),
        )
    }

    suspend fun deleteStatementLine(line: StatementLineEntity) = dao.deleteStatementLine(line)

    suspend fun setAccepted(line: StatementLineEntity, accepted: Boolean) {
        dao.deleteStatementLineMatch(line.id)
        dao.updateStatementLine(
            line.copy(
                acceptedWithoutReceipt = accepted,
                status = if (accepted) ReconciliationStatus.ACCEPTED else ReconciliationStatus.NOT_FOUND,
            ),
        )
    }

    suspend fun rankCandidates(
        tripId: String,
        line: StatementLineEntity,
    ): List<RankedReceiptCandidate> {
        val usedReceiptIds = dao.getTripMatches(tripId).mapTo(mutableSetOf()) { it.receiptId }
        return ReconciliationMatcher.rank(
            line,
            dao.getReceipts(tripId).filterNot { it.id in usedReceiptIds },
        )
    }

    suspend fun assignReceipt(line: StatementLineEntity, receipt: ReceiptEntity, manually: Boolean) {
        dao.replaceReceiptMatch(ReceiptMatchEntity(line.id, receipt.id, manually))
        dao.updateStatementLine(
            line.copy(
                status = ReconciliationMatcher.suggestedStatus(line, receipt),
                acceptedWithoutReceipt = false,
            ),
        )
    }

    suspend fun clearLineMatch(line: StatementLineEntity) {
        dao.deleteStatementLineMatch(line.id)
        dao.updateStatementLine(
            line.copy(status = ReconciliationStatus.NOT_FOUND, acceptedWithoutReceipt = false),
        )
    }

    suspend fun runAutomaticReconciliation(
        tripId: String,
        reconciliation: ReconciliationWithLines,
    ) {
        dao.resetMatches(reconciliation.reconciliation.id)
        val usedElsewhere = dao.getTripMatches(tripId).mapTo(mutableSetOf()) { it.receiptId }
        val available = dao.getReceipts(tripId).filterNot { it.id in usedElsewhere }.toMutableList()
        reconciliation.lines.sortedBy { it.line.occurredOn ?: Long.MAX_VALUE }.forEach { related ->
            val line = related.line
            if (line.acceptedWithoutReceipt) return@forEach
            val ranked = ReconciliationMatcher.rank(line, available)
            val strong = ranked.firstOrNull()?.receipt?.takeIf {
                ReconciliationMatcher.isStrongAutomaticMatch(line, it)
            }
            if (strong != null) {
                assignReceipt(line, strong, manually = false)
                available.removeAll { it.id == strong.id }
            } else {
                val status = ranked.firstOrNull()?.receipt?.let {
                    ReconciliationMatcher.suggestedStatus(line, it)
                }?.takeIf { it == ReconciliationStatus.AMOUNT_MISMATCH }
                    ?: ReconciliationStatus.NOT_FOUND
                dao.updateStatementLine(line.copy(status = status, acceptedWithoutReceipt = false))
            }
        }
    }

    suspend fun resetReconciliation(reconciliation: ReconciliationWithLines) {
        dao.resetMatches(reconciliation.reconciliation.id)
        reconciliation.lines.forEach { related ->
            dao.updateStatementLine(
                related.line.copy(
                    status = if (related.line.acceptedWithoutReceipt) {
                        ReconciliationStatus.ACCEPTED
                    } else {
                        ReconciliationStatus.NOT_FOUND
                    },
                ),
            )
        }
    }

    suspend fun deleteReconciliation(reconciliationId: String) =
        dao.deleteReconciliation(reconciliationId)
}
