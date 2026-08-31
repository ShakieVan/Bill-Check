package de.shakie.billcheck.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Embedded
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "trips", indices = [Index(value = ["sortPosition"], unique = true)])
data class TripEntity(
    @PrimaryKey val id: String,
    val sortPosition: Int,
    val name: String,
    /** Currency in which this trip is evaluated and summarized. */
    val homeCurrencyCode: String,
    val defaultTipMinor: Long,
    val defaultTipCurrencyCode: String,
    val defaultTipSelected: Boolean,
    val imageStorageMode: String,
    val createdAt: Long,
)

/**
 * A currency that can be used by receipts of a trip.
 *
 * [homeToCurrencyRate] is deliberately stored as decimal text and always means
 * `1 homeCurrencyCode = x currencyCode`.  This makes a rate unambiguous even
 * when the user's home currency is not EUR.
 */
@Entity(
    tableName = "trip_currencies",
    primaryKeys = ["tripId", "currencyCode"],
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tripId")],
)
data class TripCurrencyEntity(
    val tripId: String,
    val currencyCode: String,
    val homeToCurrencyRate: String,
    val exchangeRateMode: String,
    val isDefault: Boolean,
)

data class TripWithCurrencies(
    @Embedded val trip: TripEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tripId",
    )
    val currencies: List<TripCurrencyEntity>,
)

@Entity(
    tableName = "receipts",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tripId"), Index(value = ["tripId", "checkNumber"])],
)
data class ReceiptEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val occurredAt: Long,
    val location: String,
    val checkNumber: String,
    val amountMinor: Long,
    val currencyCode: String,
    /** Snapshot using the orientation `1 trip home currency = x receipt currency`. */
    val exchangeRateSnapshot: String,
    val exactHomeMinor: Long,
    val tipMinor: Long,
    val tipCurrencyCode: String,
    /** Independent snapshot for a tip in a currency other than the receipt currency. */
    val tipExchangeRateSnapshot: String,
    val imageUri: String?,
    val reviewState: String,
    val createdAt: Long,
)

object ReceiptReviewState {
    const val CONFIRMED = "CONFIRMED"
    const val REVIEW_REQUIRED_PREFIX = "REVIEW_REQUIRED"

    fun required(reasons: List<String>): String = buildString {
        append(REVIEW_REQUIRED_PREFIX)
        reasons.filter(String::isNotBlank).distinct().takeIf(List<String>::isNotEmpty)?.let {
            append(':')
            append(it.joinToString(","))
        }
    }

    fun needsReview(value: String): Boolean = value.startsWith(REVIEW_REQUIRED_PREFIX)
}

object BatchReceiptImportStatus {
    const val QUEUED = "QUEUED"
    const val PROCESSING = "PROCESSING"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
    const val CANCELLED = "CANCELLED"
}

@Entity(
    tableName = "receipt_items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("receiptId")],
)
data class ReceiptItemEntity(
    @PrimaryKey val id: String,
    val receiptId: String,
    val sortPosition: Int,
    val name: String,
    val amountMinor: Long,
    val currencyCode: String,
)

data class ReceiptWithItems(
    @Embedded val receipt: ReceiptEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "receiptId",
    )
    val items: List<ReceiptItemEntity>,
)

@Entity(
    tableName = "batch_receipt_imports",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tripId"), Index("batchId")],
)
data class BatchReceiptImportEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val tripId: String,
    val sortPosition: Int,
    val imageUri: String,
    val status: String,
    val receiptId: String? = null,
    val message: String? = null,
    val dismissed: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "reconciliations",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tripId")],
)
data class ReconciliationEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val title: String,
    val statementImageUri: String?,
    val createdAt: Long,
    val analysisSummary: String? = null,
    val analysisUpdatedAt: Long? = null,
    val declaredTotalMinor: Long? = null,
    val declaredTotalCurrencyCode: String? = null,
)

data class ReconciliationWithLines(
    @Embedded val reconciliation: ReconciliationEntity,
    @Relation(
        entity = StatementLineEntity::class,
        parentColumn = "id",
        entityColumn = "reconciliationId",
    )
    val lines: List<StatementLineWithMatches>,
)

@Entity(
    tableName = "statement_lines",
    foreignKeys = [
        ForeignKey(
            entity = ReconciliationEntity::class,
            parentColumns = ["id"],
            childColumns = ["reconciliationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("reconciliationId")],
)
data class StatementLineEntity(
    @PrimaryKey val id: String,
    val reconciliationId: String,
    val occurredOn: Long?,
    val description: String,
    val checkNumber: String,
    val amountMinor: Long,
    val currencyCode: String,
    val status: String,
    val acceptedWithoutReceipt: Boolean,
    val aiSuggestedReceiptId: String? = null,
    val aiConfidence: Int? = null,
    val aiReason: String? = null,
    val sourceDateText: String? = null,
    val dateAmbiguous: Boolean = false,
)

data class StatementLineWithMatches(
    @Embedded val line: StatementLineEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "statementLineId",
    )
    val matches: List<ReceiptMatchEntity>,
)

@Entity(
    tableName = "receipt_matches",
    primaryKeys = ["statementLineId", "receiptId"],
    foreignKeys = [
        ForeignKey(
            entity = StatementLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["statementLineId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["statementLineId"], unique = true),
        Index(value = ["receiptId"], unique = true),
    ],
)
data class ReceiptMatchEntity(
    val statementLineId: String,
    val receiptId: String,
    val matchedManually: Boolean,
)
