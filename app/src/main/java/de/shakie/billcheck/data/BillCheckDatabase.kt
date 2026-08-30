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
        ReconciliationEntity::class,
        StatementLineEntity::class,
        ReceiptMatchEntity::class,
    ],
    version = 6,
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
                )
                // Version 6 deliberately replaces the pre-release single-currency
                // schema. Bill Check was not yet live, so retaining a second legacy
                // model and its ambiguous tip-rate semantics would be riskier.
                .fallbackToDestructiveMigration(dropAllTables = true)
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

    }
}
