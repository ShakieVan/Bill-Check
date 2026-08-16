package de.shakie.billcheck.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TripEntity::class,
        ReceiptEntity::class,
        ReceiptItemEntity::class,
        ReconciliationEntity::class,
        StatementLineEntity::class,
        ReceiptMatchEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class BillCheckDatabase : RoomDatabase() {
    abstract fun dao(): BillCheckDao

    companion object {
        fun create(context: Context): BillCheckDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                BillCheckDatabase::class.java,
                "bill-check.db",
            ).build()
    }
}

