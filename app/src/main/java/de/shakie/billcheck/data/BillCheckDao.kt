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

    @Transaction
    @Query("SELECT * FROM receipts WHERE tripId = :tripId ORDER BY occurredAt DESC, createdAt DESC")
    fun observeReceipts(tripId: String): Flow<List<ReceiptWithItems>>

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

    @Query("UPDATE receipts SET imageUri = :imageUri WHERE id = :receiptId")
    suspend fun updateReceiptImage(receiptId: String, imageUri: String?)

    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)

    @Query("DELETE FROM receipt_matches WHERE statementLineId IN (SELECT id FROM statement_lines WHERE reconciliationId = :reconciliationId)")
    suspend fun resetMatches(reconciliationId: String)

    @Query("DELETE FROM reconciliations WHERE id = :reconciliationId")
    suspend fun deleteReconciliation(reconciliationId: String)
}
