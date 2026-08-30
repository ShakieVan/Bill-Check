package de.shakie.billcheck.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MultiCurrencyRepositoryTest {
    @Test
    fun deletingTripCascadesAppDataButNeedsNoImageDeletion() {
        runBlocking {
            val database = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                BillCheckDatabase::class.java,
            ).allowMainThreadQueries().build()
            try {
                val repository = BillCheckRepository(database)
                val trip = repository.createTrip(
                    "Disposable",
                    "EUR",
                    listOf(TripCurrencyInput("EUR", "1", "FIXED", true)),
                    defaultTipMinor = 0,
                    defaultTipCurrencyCode = "EUR",
                )
                repository.addReceipt(
                    trip = trip,
                    location = "Test",
                    checkNumber = "1",
                    amountMinor = 100,
                    currencyCode = "EUR",
                    exchangeRateSnapshot = "1",
                    tipMinor = 0,
                    tipCurrencyCode = "EUR",
                    tipExchangeRateSnapshot = "1",
                    imageUri = "content://media/external/images/media/123",
                )
                repository.createReconciliation(trip, "Final")

                repository.deleteTrip(trip)

                assertEquals(emptyList<TripEntity>(), repository.trips.first())
                assertEquals(emptyList<ReceiptWithItems>(), repository.receipts(trip.id).first())
                assertEquals(emptyList<ReconciliationWithLines>(), repository.reconciliations(trip.id).first())
            } finally {
                database.close()
            }
        }
    }

    @Test
    fun tripAndCurrenciesAreInsertedAtomicallyAndReceiptKeepsIndependentSnapshots() {
        runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            BillCheckDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            val repository = BillCheckRepository(database)
            val trip = repository.createTrip(
                name = "Egypt",
                homeCurrencyCode = "EUR",
                currencies = listOf(
                    TripCurrencyInput("EUR", "1.00", "FIXED", false),
                    TripCurrencyInput("EGP", "55.5", "DAILY", true),
                    TripCurrencyInput("USD", "1.08", "FIXED", false),
                ),
                defaultTipMinor = 100,
                defaultTipCurrencyCode = "USD",
            )
            assertEquals(listOf("EGP", "EUR", "USD"), repository.tripCurrencies(trip.id).first().map { it.currencyCode })

            repository.addReceipt(
                trip = trip,
                location = "Sultana",
                checkNumber = "10720",
                amountMinor = 55_500,
                currencyCode = "EGP",
                exchangeRateSnapshot = "55.5",
                tipMinor = 0,
                tipCurrencyCode = "USD",
                tipExchangeRateSnapshot = "1.08",
                items = listOf(NewReceiptItem("Dinner", 55_500)),
            )
            val stored = repository.receipts(trip.id).first().single()
            assertEquals("55.5", stored.receipt.exchangeRateSnapshot)
            assertEquals("EUR", stored.receipt.tipCurrencyCode)
            assertEquals("1", stored.receipt.tipExchangeRateSnapshot)
            assertEquals(1_000, stored.receipt.exactHomeMinor)
            assertEquals("EGP", stored.items.single().currencyCode)

            repository.updateReceipt(
                trip = trip,
                existing = stored.receipt,
                location = "Sultana Rest.",
                checkNumber = "10720",
                amountMinor = 55_500,
                currencyCode = "EGP",
                exchangeRateSnapshot = "999", // ignored: currency did not change
                occurredAt = stored.receipt.occurredAt,
                tipMinor = 540,
                tipCurrencyCode = "USD",
                tipExchangeRateSnapshot = "1.08",
                items = listOf(NewReceiptItem("Dinner", 55_500)),
            )
            val updated = repository.receipts(trip.id).first().single()
            assertEquals("55.5", updated.receipt.exchangeRateSnapshot)
            assertEquals("1.08", updated.receipt.tipExchangeRateSnapshot)
            assertEquals(1_500, updated.receipt.exactHomeMinor)
            assertEquals("EGP", updated.items.single().currencyCode)

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.updateTrip(
                        existing = trip,
                        name = trip.name,
                        currencies = listOf(
                            TripCurrencyInput("EUR", "1", "FIXED", false),
                            TripCurrencyInput("EGP", "55.5", "DAILY", true),
                        ),
                        defaultTipMinor = 0,
                        defaultTipCurrencyCode = "EUR",
                        defaultTipSelected = false,
                    )
                }
            }
        } finally {
            database.close()
        }
        }
    }

    @Test
    fun zeroTipDoesNotProtectAnOtherwiseUnusedTipCurrency() {
        runBlocking {
            val database = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                BillCheckDatabase::class.java,
            ).allowMainThreadQueries().build()
            try {
                val repository = BillCheckRepository(database)
                val trip = repository.createTrip(
                    "Egypt",
                    "EUR",
                    listOf(
                        TripCurrencyInput("EUR", "1", "FIXED", false),
                        TripCurrencyInput("EGP", "55.5", "FIXED", true),
                        TripCurrencyInput("USD", "1.08", "FIXED", false),
                    ),
                    defaultTipMinor = 100,
                    defaultTipCurrencyCode = "USD",
                )
                repository.addReceipt(
                    trip, "Sultana", "10720", 10_000, "EGP", "55.5", 0, "USD", "1.08",
                )

                repository.updateTrip(
                    existing = trip,
                    name = trip.name,
                    currencies = listOf(
                        TripCurrencyInput("EUR", "1", "FIXED", false),
                        TripCurrencyInput("EGP", "55.5", "FIXED", true),
                    ),
                    defaultTipMinor = 0,
                    defaultTipCurrencyCode = "EUR",
                    defaultTipSelected = false,
                )

                assertEquals(
                    setOf("EGP", "EUR"),
                    repository.tripCurrencies(trip.id).first().mapTo(hashSetOf()) { it.currencyCode },
                )
            } finally {
                database.close()
            }
        }
    }

    @Test
    fun changingReceiptCurrencyRequiresExplicitNewSnapshot() {
        runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            BillCheckDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            val repository = BillCheckRepository(database)
            val trip = repository.createTrip(
                "Egypt",
                "EUR",
                listOf(
                    TripCurrencyInput("EUR", "1", "FIXED", false),
                    TripCurrencyInput("EGP", "55.5", "FIXED", true),
                ),
                defaultTipMinor = 0,
                defaultTipCurrencyCode = "EUR",
            )
            repository.addReceipt(
                trip, "Sultana", "10720", 10_000, "EGP", "55.5", 0, "EUR", "1",
            )
            val existing = repository.receipts(trip.id).first().single().receipt

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.updateReceipt(
                        trip, existing, "Sultana", "10720", 10_000, "EUR", null,
                        existing.occurredAt, 0, "EUR", null, emptyList(),
                    )
                }
            }
        } finally {
            database.close()
        }
        }
    }

    @Test
    fun currencyUsedByExistingReceiptCannotBeRemovedFromTrip() {
        runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            BillCheckDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            val repository = BillCheckRepository(database)
            val trip = repository.createTrip(
                "Egypt",
                "EUR",
                listOf(
                    TripCurrencyInput("EUR", "1", "FIXED", false),
                    TripCurrencyInput("EGP", "55.5", "FIXED", true),
                ),
                defaultTipMinor = 0,
                defaultTipCurrencyCode = "EUR",
            )
            repository.addReceipt(
                trip, "Sultana", "10720", 10_000, "EGP", "55.5", 0, "EUR", "1",
            )

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.updateTrip(
                        existing = trip,
                        name = trip.name,
                        currencies = listOf(TripCurrencyInput("EUR", "1", "FIXED", true)),
                        defaultTipMinor = 0,
                        defaultTipCurrencyCode = "EUR",
                        defaultTipSelected = false,
                    )
                }
            }
            assertEquals(
                setOf("EGP", "EUR"),
                repository.tripCurrencies(trip.id).first().mapTo(hashSetOf()) { it.currencyCode },
            )
        } finally {
            database.close()
        }
        }
    }
}
