package de.shakie.billcheck

import android.app.Application
import de.shakie.billcheck.data.BillCheckDatabase
import de.shakie.billcheck.data.BillCheckRepository

class BillCheckApplication : Application() {
    val database by lazy { BillCheckDatabase.create(this) }
    val repository by lazy { BillCheckRepository(database) }
}

