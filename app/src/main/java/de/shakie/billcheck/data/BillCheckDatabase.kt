package de.shakie.billcheck.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TripEntity::class,
        ReceiptEntity::class,
        ReceiptItemEntity::class,
        ReconciliationEntity::class,
        StatementLineEntity::class,
        ReceiptMatchEntity::class,
    ],
    version = 3,
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
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE trips ADD COLUMN exchangeRateMode TEXT NOT NULL DEFAULT 'FIXED'",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM receipt_matches WHERE rowid NOT IN " +
                        "(SELECT MIN(rowid) FROM receipt_matches GROUP BY statementLineId)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_receipt_matches_statementLineId " +
                        "ON receipt_matches(statementLineId)",
                )
            }
        }
    }
}
