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

    @Transaction
    @Query("SELECT * FROM trips ORDER BY sortPosition")
    fun observeTripsWithCurrencies(): Flow<List<TripWithCurrencies>>

    @Transaction
    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    suspend fun getTripWithCurrencies(tripId: String): TripWithCurrencies?

    @Query("SELECT * FROM trip_currencies WHERE tripId = :tripId ORDER BY isDefault DESC, currencyCode")
    fun observeTripCurrencies(tripId: String): Flow<List<TripCurrencyEntity>>

    @Query("SELECT * FROM trip_currencies WHERE tripId = :tripId ORDER BY isDefault DESC, currencyCode")
    suspend fun getTripCurrencies(tripId: String): List<TripCurrencyEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTripCurrencies(currencies: List<TripCurrencyEntity>)

    @Query("DELETE FROM trip_currencies WHERE tripId = :tripId")
    suspend fun deleteTripCurrencies(tripId: String)

    @Transaction
    suspend fun replaceTripCurrencies(
        tripId: String,
        homeCurrencyCode: String,
        currencies: List<TripCurrencyEntity>,
    ) {
        require(currencies.all { it.tripId == tripId }) { "Currency belongs to another trip" }
        de.shakie.billcheck.domain.TripCurrencyRules.requireValid(homeCurrencyCode, currencies)
        deleteTripCurrencies(tripId)
        insertTripCurrencies(currencies)
    }

    @Query("SELECT COALESCE(MAX(sortPosition), -1) + 1 FROM trips")
    suspend fun nextTripPosition(): Int

    @Query("SELECT * FROM trips WHERE id IN (:tripIds) ORDER BY sortPosition")
    suspend fun getTrips(tripIds: List<String>): List<TripEntity>

    @Query("SELECT * FROM trips ORDER BY sortPosition")
    suspend fun getAllTrips(): List<TripEntity>

    @Query("SELECT name FROM trips")
    suspend fun getTripNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrip(trip: TripEntity)

    @Transaction
    suspend fun insertTripWithCurrencies(
        trip: TripEntity,
        currencies: List<TripCurrencyEntity>,
    ) {
        require(currencies.all { it.tripId == trip.id }) { "Currency belongs to another trip" }
        de.shakie.billcheck.domain.TripCurrencyRules.requireValid(trip.homeCurrencyCode, currencies)
        insertTrip(trip)
        insertTripCurrencies(currencies)
    }

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Transaction
    suspend fun updateTripWithCurrencies(
        trip: TripEntity,
        currencies: List<TripCurrencyEntity>,
    ) {
        updateTrip(trip)
        replaceTripCurrencies(trip.id, trip.homeCurrencyCode, currencies)
    }

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
        "SELECT currencyCode FROM receipts WHERE tripId = :tripId " +
            "UNION SELECT tipCurrencyCode FROM receipts WHERE tripId = :tripId AND tipMinor > 0",
    )
    fun observeUsedReceiptCurrencyCodes(tripId: String): Flow<List<String>>

    @Transaction
    @Query("SELECT * FROM receipts WHERE tripId = :tripId ORDER BY occurredAt, createdAt")
    suspend fun getReceiptsWithItems(tripId: String): List<ReceiptWithItems>

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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceipts(receipts: List<ReceiptEntity>)

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

    @Query("SELECT * FROM receipts WHERE id = :receiptId LIMIT 1")
    suspend fun getReceipt(receiptId: String): ReceiptEntity?

    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)

    @Query(
        "SELECT * FROM batch_receipt_imports " +
            "WHERE tripId = :tripId AND dismissed = 0 ORDER BY createdAt, sortPosition",
    )
    fun observeVisibleBatchImports(tripId: String): Flow<List<BatchReceiptImportEntity>>

    @Query(
        "SELECT * FROM batch_receipt_imports " +
            "WHERE status IN ('QUEUED', 'PROCESSING') ORDER BY createdAt, sortPosition LIMIT 1",
    )
    suspend fun nextPendingBatchImport(): BatchReceiptImportEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBatchImports(items: List<BatchReceiptImportEntity>)

    @Update
    suspend fun updateBatchImport(item: BatchReceiptImportEntity)

    @Query("UPDATE batch_receipt_imports SET dismissed = 1 WHERE tripId = :tripId")
    suspend fun dismissVisibleBatchImports(tripId: String)

    @Query(
        "UPDATE batch_receipt_imports SET status = 'CANCELLED', updatedAt = :updatedAt " +
            "WHERE tripId = :tripId AND status IN ('QUEUED', 'PROCESSING')",
    )
    suspend fun cancelPendingBatchImportsForTrip(tripId: String, updatedAt: Long)

    @Transaction
    suspend fun replaceVisibleBatchImports(
        tripId: String,
        items: List<BatchReceiptImportEntity>,
    ) {
        require(items.all { it.tripId == tripId })
        cancelPendingBatchImportsForTrip(tripId, System.currentTimeMillis())
        dismissVisibleBatchImports(tripId)
        insertBatchImports(items)
    }

    @Query(
        "UPDATE batch_receipt_imports SET status = 'QUEUED', message = NULL, " +
            "updatedAt = :updatedAt WHERE id = :itemId",
    )
    suspend fun retryBatchImport(itemId: String, updatedAt: Long)

    @Query(
        "UPDATE batch_receipt_imports SET status = 'CANCELLED', " +
            "message = NULL, updatedAt = :updatedAt " +
            "WHERE batchId = :batchId AND status IN ('QUEUED', 'PROCESSING')",
    )
    suspend fun cancelPendingBatchImports(batchId: String, updatedAt: Long)

    @Query("UPDATE batch_receipt_imports SET message = NULL WHERE receiptId = :receiptId")
    suspend fun markBatchImportReviewed(receiptId: String)

    @Query("DELETE FROM batch_receipt_imports WHERE receiptId = :receiptId")
    suspend fun deleteBatchImportForReceipt(receiptId: String)

    @Transaction
    @Query("SELECT * FROM reconciliations WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun observeReconciliations(tripId: String): Flow<List<ReconciliationWithLines>>

    @Transaction
    @Query("SELECT * FROM reconciliations WHERE tripId = :tripId ORDER BY createdAt")
    suspend fun getReconciliations(tripId: String): List<ReconciliationWithLines>

    @Transaction
    @Query("SELECT * FROM reconciliations WHERE id = :reconciliationId LIMIT 1")
    suspend fun getReconciliation(reconciliationId: String): ReconciliationWithLines?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReconciliation(reconciliation: ReconciliationEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReconciliations(reconciliations: List<ReconciliationEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStatementLine(line: StatementLineEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStatementLines(lines: List<StatementLineEntity>)

    @Query("DELETE FROM statement_lines WHERE reconciliationId = :reconciliationId")
    suspend fun deleteStatementLines(reconciliationId: String)

    @Transaction
    suspend fun replaceStatementLines(
        reconciliationId: String,
        lines: List<StatementLineEntity>,
    ) {
        deleteStatementLines(reconciliationId)
        if (lines.isNotEmpty()) insertStatementLines(lines)
    }

    @Update
    suspend fun updateStatementLine(line: StatementLineEntity)

    @Delete
    suspend fun deleteStatementLine(line: StatementLineEntity)

    @Query("UPDATE reconciliations SET title = :title, statementImageUri = :imageUri WHERE id = :reconciliationId")
    suspend fun updateReconciliation(reconciliationId: String, title: String, imageUri: String?)

    @Update
    suspend fun updateReconciliationEntity(reconciliation: ReconciliationEntity)

    @Query("UPDATE reconciliations SET analysisSummary = :summary, analysisUpdatedAt = :updatedAt WHERE id = :reconciliationId")
    suspend fun updateReconciliationAnalysis(reconciliationId: String, summary: String?, updatedAt: Long?)

    @Query("UPDATE reconciliations SET analysisSummary = NULL, analysisUpdatedAt = NULL WHERE id = :reconciliationId")
    suspend fun clearReconciliationAnalysis(reconciliationId: String)

    @Query("UPDATE reconciliations SET analysisSummary = NULL, analysisUpdatedAt = NULL WHERE tripId = :tripId")
    suspend fun clearTripAnalyses(tripId: String)

    @Query("SELECT * FROM receipts WHERE tripId = :tripId ORDER BY occurredAt DESC, createdAt DESC")
    suspend fun getReceipts(tripId: String): List<ReceiptEntity>

    @Query(
        "SELECT receipt_matches.* FROM receipt_matches " +
            "INNER JOIN statement_lines ON statement_lines.id = receipt_matches.statementLineId " +
            "INNER JOIN reconciliations ON reconciliations.id = statement_lines.reconciliationId " +
            "WHERE reconciliations.tripId = :tripId",
    )
    suspend fun getTripMatches(tripId: String): List<ReceiptMatchEntity>

    @Query(
        "SELECT receipt_matches.* FROM receipt_matches " +
            "INNER JOIN statement_lines ON statement_lines.id = receipt_matches.statementLineId " +
            "WHERE statement_lines.reconciliationId = :reconciliationId",
    )
    suspend fun getReconciliationMatches(reconciliationId: String): List<ReceiptMatchEntity>

    @Query(
        "SELECT statement_lines.* FROM statement_lines " +
            "INNER JOIN receipt_matches ON receipt_matches.statementLineId = statement_lines.id " +
            "WHERE receipt_matches.receiptId = :receiptId",
    )
    suspend fun getStatementLinesForReceipt(receiptId: String): List<StatementLineEntity>

    @Query(
        "DELETE FROM receipt_matches WHERE statementLineId = :statementLineId OR " +
            "(receiptId = :receiptId AND statementLineId IN " +
            "(SELECT id FROM statement_lines WHERE reconciliationId = :reconciliationId))",
    )
    suspend fun clearConflictingMatches(
        statementLineId: String,
        receiptId: String,
        reconciliationId: String,
    )

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceiptMatch(match: ReceiptMatchEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceiptMatches(matches: List<ReceiptMatchEntity>)

    @Transaction
    suspend fun insertTransferredTrip(
        trip: TripEntity,
        receipts: List<ReceiptEntity>,
        items: List<ReceiptItemEntity>,
        reconciliations: List<ReconciliationEntity>,
        lines: List<StatementLineEntity>,
        matches: List<ReceiptMatchEntity>,
        currencies: List<TripCurrencyEntity>,
    ) {
        require(currencies.all { it.tripId == trip.id }) { "Currency belongs to another trip" }
        de.shakie.billcheck.domain.TripCurrencyRules.requireValid(trip.homeCurrencyCode, currencies)
        insertTrip(trip)
        insertTripCurrencies(currencies)
        if (receipts.isNotEmpty()) insertReceipts(receipts)
        if (items.isNotEmpty()) insertReceiptItems(items)
        if (reconciliations.isNotEmpty()) insertReconciliations(reconciliations)
        if (lines.isNotEmpty()) insertStatementLines(lines)
        if (matches.isNotEmpty()) insertReceiptMatches(matches)
    }

    @Transaction
    suspend fun replaceReceiptMatch(match: ReceiptMatchEntity, reconciliationId: String) {
        clearConflictingMatches(match.statementLineId, match.receiptId, reconciliationId)
        insertReceiptMatch(match)
    }

    @Transaction
    suspend fun applyExtractedStatement(
        reconciliation: ReconciliationEntity,
        lines: List<StatementLineEntity>,
    ) {
        updateReconciliationEntity(reconciliation)
        replaceStatementLines(reconciliation.id, lines)
    }

    @Query("DELETE FROM receipt_matches WHERE statementLineId = :statementLineId")
    suspend fun deleteStatementLineMatch(statementLineId: String)

    @Query("DELETE FROM receipt_matches WHERE statementLineId IN (SELECT id FROM statement_lines WHERE reconciliationId = :reconciliationId)")
    suspend fun resetMatches(reconciliationId: String)

    @Query("DELETE FROM reconciliations WHERE id = :reconciliationId")
    suspend fun deleteReconciliation(reconciliationId: String)
}
