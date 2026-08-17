package de.shakie.billcheck.data

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillCheckMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    @After
    fun cleanDatabase() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun migration4To5AddsCompletenessAndScopesReceiptUniquenessPerReconciliation() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DATABASE_NAME)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) = createVersion4Schema(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        helper.writableDatabase.apply {
            execSQL("INSERT INTO trips VALUES ('trip',0,'Trip','EGP','55.5','FIXED',0,'EUR',0,'ORIGINAL',0)")
            execSQL("INSERT INTO receipts VALUES ('receipt','trip',0,'Sultana','5512',31332,'EGP','55.5',0,0,'EUR',NULL,'CONFIRMED',0)")
            execSQL("INSERT INTO reconciliations VALUES ('r1','trip','Interim',NULL,0,NULL,NULL)")
            execSQL("INSERT INTO reconciliations VALUES ('r2','trip','Final',NULL,1,NULL,NULL)")
            execSQL("INSERT INTO statement_lines VALUES ('l1','r1',0,'Sultana','0015512',31332,'EGP','NOT_FOUND',0,NULL,NULL,NULL)")
            execSQL("INSERT INTO statement_lines VALUES ('l2','r2',0,'Sultana','0015512',31332,'EGP','NOT_FOUND',0,NULL,NULL,NULL)")

            BillCheckDatabase.MIGRATION_4_5.migrate(this)
            execSQL("INSERT INTO receipt_matches VALUES ('l1','receipt',0)")
            execSQL("INSERT INTO receipt_matches VALUES ('l2','receipt',0)")
            query("SELECT COUNT(*) FROM receipt_matches WHERE receiptId='receipt'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(2, cursor.getInt(0))
            }
            query("SELECT dateAmbiguous FROM statement_lines WHERE id='l1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            version = 5
        }
        helper.close()

        val roomDatabase = Room.databaseBuilder(context, BillCheckDatabase::class.java, DATABASE_NAME)
            .addMigrations(BillCheckDatabase.MIGRATION_4_5)
            .build()
        roomDatabase.openHelper.writableDatabase
        roomDatabase.close()
    }

    private fun createVersion4Schema(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("CREATE TABLE trips (id TEXT NOT NULL PRIMARY KEY, sortPosition INTEGER NOT NULL, name TEXT NOT NULL, foreignCurrencyCode TEXT NOT NULL, defaultExchangeRate TEXT NOT NULL, exchangeRateMode TEXT NOT NULL, defaultTipMinor INTEGER NOT NULL, defaultTipCurrencyCode TEXT NOT NULL, defaultTipSelected INTEGER NOT NULL, imageStorageMode TEXT NOT NULL, createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX index_trips_sortPosition ON trips(sortPosition)")
        db.execSQL("CREATE TABLE receipts (id TEXT NOT NULL PRIMARY KEY, tripId TEXT NOT NULL, occurredAt INTEGER NOT NULL, location TEXT NOT NULL, checkNumber TEXT NOT NULL, foreignAmountMinor INTEGER NOT NULL, foreignCurrencyCode TEXT NOT NULL, exchangeRate TEXT NOT NULL, exactEuroCents INTEGER NOT NULL, tipMinor INTEGER NOT NULL, tipCurrencyCode TEXT NOT NULL, imageUri TEXT, reviewState TEXT NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(tripId) REFERENCES trips(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_receipts_tripId ON receipts(tripId)")
        db.execSQL("CREATE INDEX index_receipts_tripId_checkNumber ON receipts(tripId,checkNumber)")
        db.execSQL("CREATE TABLE receipt_items (id TEXT NOT NULL PRIMARY KEY, receiptId TEXT NOT NULL, sortPosition INTEGER NOT NULL, name TEXT NOT NULL, amountMinor INTEGER NOT NULL, currencyCode TEXT NOT NULL, FOREIGN KEY(receiptId) REFERENCES receipts(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_receipt_items_receiptId ON receipt_items(receiptId)")
        db.execSQL("CREATE TABLE reconciliations (id TEXT NOT NULL PRIMARY KEY, tripId TEXT NOT NULL, title TEXT NOT NULL, statementImageUri TEXT, createdAt INTEGER NOT NULL, analysisSummary TEXT, analysisUpdatedAt INTEGER, FOREIGN KEY(tripId) REFERENCES trips(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_reconciliations_tripId ON reconciliations(tripId)")
        db.execSQL("CREATE TABLE statement_lines (id TEXT NOT NULL PRIMARY KEY, reconciliationId TEXT NOT NULL, occurredOn INTEGER, description TEXT NOT NULL, checkNumber TEXT NOT NULL, amountMinor INTEGER NOT NULL, currencyCode TEXT NOT NULL, status TEXT NOT NULL, acceptedWithoutReceipt INTEGER NOT NULL, aiSuggestedReceiptId TEXT, aiConfidence INTEGER, aiReason TEXT, FOREIGN KEY(reconciliationId) REFERENCES reconciliations(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_statement_lines_reconciliationId ON statement_lines(reconciliationId)")
        db.execSQL("CREATE TABLE receipt_matches (statementLineId TEXT NOT NULL, receiptId TEXT NOT NULL, matchedManually INTEGER NOT NULL, PRIMARY KEY(statementLineId,receiptId), FOREIGN KEY(statementLineId) REFERENCES statement_lines(id) ON DELETE CASCADE, FOREIGN KEY(receiptId) REFERENCES receipts(id) ON DELETE CASCADE)")
        db.execSQL("CREATE UNIQUE INDEX index_receipt_matches_statementLineId ON receipt_matches(statementLineId)")
        db.execSQL("CREATE UNIQUE INDEX index_receipt_matches_receiptId ON receipt_matches(receiptId)")
    }

    private companion object {
        const val DATABASE_NAME = "migration-test"
    }
}
