package de.shakie.billcheck.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BillCheckDao {
    @Query("SELECT * FROM trips ORDER BY sortPosition")
    fun observeTrips(): Flow<List<TripEntity>>

    @Query("SELECT COALESCE(MAX(sortPosition), -1) + 1 FROM trips")
    suspend fun nextTripPosition(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrip(trip: TripEntity)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("UPDATE trips SET sortPosition = sortPosition + 1000000")
    suspend fun shiftTripPositionsForReorder()

    @Query("UPDATE trips SET sortPosition = :position WHERE id = :tripId")
    suspend fun updateTripPosition(tripId: String, position: Int)

    @Transaction
    suspend fun replaceTripOrder(tripIds: List<String>) {
        shiftTripPositionsForReorder()
        tripIds.forEachIndexed { index, tripId -> updateTripPosition(tripId, index) }
    }

    @Transaction
    @Query("SELECT * FROM receipts WHERE tripId = :tripId ORDER BY occurredAt DESC, createdAt DESC")
    fun observeReceipts(tripId: String): Flow<List<ReceiptWithItems>>

    @Query(
        """
        SELECT location FROM receipts
        WHERE tripId = :tripId AND TRIM(location) != ''
        GROUP BY location COLLATE NOCASE
        ORDER BY MAX(createdAt) DESC
        LIMIT 30
        """,
    )
    fun observeLocationSuggestions(tripId: String): Flow<List<String>>

    @Query(
        """
        SELECT receipt_items.name FROM receipt_items
        INNER JOIN receipts ON receipts.id = receipt_items.receiptId
        WHERE receipts.tripId = :tripId AND TRIM(receipt_items.name) != ''
        GROUP BY receipt_items.name COLLATE NOCASE
        ORDER BY MAX(receipts.createdAt) DESC
        LIMIT 50
        """,
    )
    fun observeItemNameSuggestions(tripId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceipt(receipt: ReceiptEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceiptItems(items: List<ReceiptItemEntity>)

    @Transaction
    suspend fun insertReceiptWithItems(
        receipt: ReceiptEntity,
        items: List<ReceiptItemEntity>,
    ) {
        insertReceipt(receipt)
        if (items.isNotEmpty()) insertReceiptItems(items)
    }

    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)

    @Query("DELETE FROM receipt_items WHERE receiptId = :receiptId")
    suspend fun deleteReceiptItems(receiptId: String)

    @Transaction
    suspend fun updateReceiptWithItems(
        receipt: ReceiptEntity,
        items: List<ReceiptItemEntity>,
    ) {
        updateReceipt(receipt)
        deleteReceiptItems(receipt.id)
        if (items.isNotEmpty()) insertReceiptItems(items)
    }

    @Query("UPDATE receipts SET imageUri = :imageUri WHERE id = :receiptId")
    suspend fun updateReceiptImage(receiptId: String, imageUri: String?)

    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)

    @Query("DELETE FROM receipt_matches WHERE statementLineId IN (SELECT id FROM statement_lines WHERE reconciliationId = :reconciliationId)")
    suspend fun resetMatches(reconciliationId: String)

    @Query("DELETE FROM reconciliations WHERE id = :reconciliationId")
    suspend fun deleteReconciliation(reconciliationId: String)
}
