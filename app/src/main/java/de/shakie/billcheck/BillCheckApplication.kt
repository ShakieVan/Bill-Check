package de.shakie.billcheck

import android.app.Application
import de.shakie.billcheck.data.BillCheckDatabase
import de.shakie.billcheck.data.BillCheckRepository
import de.shakie.billcheck.data.OpenExchangeRateProvider

class BillCheckApplication : Application() {
    val database by lazy { BillCheckDatabase.create(this) }
    val repository by lazy { BillCheckRepository(database) }
    val exchangeRateProvider by lazy { OpenExchangeRateProvider(this) }
}
