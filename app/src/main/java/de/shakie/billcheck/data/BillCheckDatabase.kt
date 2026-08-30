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
        TripCurrencyEntity::class,
        ReceiptEntity::class,
        ReceiptItemEntity::class,
        BatchReceiptImportEntity::class,
        ReconciliationEntity::class,
        StatementLineEntity::class,
        ReceiptMatchEntity::class,
    ],
    version = 7,
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
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                )
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reconciliations ADD COLUMN analysisSummary TEXT")
                db.execSQL("ALTER TABLE reconciliations ADD COLUMN analysisUpdatedAt INTEGER")
                db.execSQL("ALTER TABLE statement_lines ADD COLUMN aiSuggestedReceiptId TEXT")
                db.execSQL("ALTER TABLE statement_lines ADD COLUMN aiConfidence INTEGER")
                db.execSQL("ALTER TABLE statement_lines ADD COLUMN aiReason TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reconciliations ADD COLUMN declaredTotalMinor INTEGER")
                db.execSQL("ALTER TABLE reconciliations ADD COLUMN declaredTotalCurrencyCode TEXT")
                db.execSQL("ALTER TABLE statement_lines ADD COLUMN sourceDateText TEXT")
                db.execSQL(
                    "ALTER TABLE statement_lines ADD COLUMN dateAmbiguous INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("DROP INDEX IF EXISTS index_receipt_matches_receiptId")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_receipt_matches_receiptId " +
                        "ON receipt_matches(receiptId)",
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN homeCurrencyCode TEXT NOT NULL DEFAULT 'EUR'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS trip_currencies (" +
                        "tripId TEXT NOT NULL, currencyCode TEXT NOT NULL, " +
                        "homeToCurrencyRate TEXT NOT NULL, exchangeRateMode TEXT NOT NULL, " +
                        "isDefault INTEGER NOT NULL, PRIMARY KEY(tripId, currencyCode), " +
                        "FOREIGN KEY(tripId) REFERENCES trips(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "INSERT INTO trip_currencies " +
                        "SELECT id, 'EUR', '1', 'FIXED', CASE WHEN foreignCurrencyCode = 'EUR' THEN 1 ELSE 0 END " +
                        "FROM trips",
                )
                db.execSQL(
                    "INSERT OR REPLACE INTO trip_currencies " +
                        "SELECT id, foreignCurrencyCode, defaultExchangeRate, exchangeRateMode, 1 FROM trips",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_currencies_tripId ON trip_currencies(tripId)")

                db.execSQL("ALTER TABLE receipts ADD COLUMN amountMinor INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE receipts SET amountMinor = foreignAmountMinor")
                db.execSQL("ALTER TABLE receipts ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'EUR'")
                db.execSQL("UPDATE receipts SET currencyCode = foreignCurrencyCode")
                db.execSQL("ALTER TABLE receipts ADD COLUMN exchangeRateSnapshot TEXT NOT NULL DEFAULT '1'")
                db.execSQL("UPDATE receipts SET exchangeRateSnapshot = exchangeRate")
                db.execSQL("ALTER TABLE receipts ADD COLUMN exactHomeMinor INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE receipts SET exactHomeMinor = exactEuroCents")
                db.execSQL("ALTER TABLE receipts ADD COLUMN tipExchangeRateSnapshot TEXT NOT NULL DEFAULT '1'")
                db.execSQL(
                    "UPDATE receipts SET tipExchangeRateSnapshot = CASE " +
                        "WHEN tipCurrencyCode = foreignCurrencyCode THEN exchangeRate ELSE '1' END",
                )
                db.execSQL("ALTER TABLE receipts DROP COLUMN foreignAmountMinor")
                db.execSQL("ALTER TABLE receipts DROP COLUMN foreignCurrencyCode")
                db.execSQL("ALTER TABLE receipts DROP COLUMN exchangeRate")
                db.execSQL("ALTER TABLE receipts DROP COLUMN exactEuroCents")

                db.execSQL("ALTER TABLE trips DROP COLUMN foreignCurrencyCode")
                db.execSQL("ALTER TABLE trips DROP COLUMN defaultExchangeRate")
                db.execSQL("ALTER TABLE trips DROP COLUMN exchangeRateMode")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS batch_receipt_imports (" +
                        "id TEXT NOT NULL, batchId TEXT NOT NULL, tripId TEXT NOT NULL, " +
                        "sortPosition INTEGER NOT NULL, imageUri TEXT NOT NULL, status TEXT NOT NULL, " +
                        "receiptId TEXT, message TEXT, dismissed INTEGER NOT NULL, " +
                        "createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id), " +
                        "FOREIGN KEY(tripId) REFERENCES trips(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_batch_receipt_imports_tripId " +
                        "ON batch_receipt_imports(tripId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_batch_receipt_imports_batchId " +
                        "ON batch_receipt_imports(batchId)",
                )
            }
        }

    }
}
