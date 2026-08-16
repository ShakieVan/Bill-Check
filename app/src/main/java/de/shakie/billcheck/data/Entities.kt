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
    val foreignCurrencyCode: String,
    val defaultExchangeRate: String,
    val exchangeRateMode: String,
    val defaultTipMinor: Long,
    val defaultTipCurrencyCode: String,
    val defaultTipSelected: Boolean,
    val imageStorageMode: String,
    val createdAt: Long,
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
    val foreignAmountMinor: Long,
    val foreignCurrencyCode: String,
    val exchangeRate: String,
    val exactEuroCents: Long,
    val tipMinor: Long,
    val tipCurrencyCode: String,
    val imageUri: String?,
    val reviewState: String,
    val createdAt: Long,
)

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
    indices = [Index(value = ["receiptId"], unique = true)],
)
data class ReceiptMatchEntity(
    val statementLineId: String,
    val receiptId: String,
    val matchedManually: Boolean,
)
