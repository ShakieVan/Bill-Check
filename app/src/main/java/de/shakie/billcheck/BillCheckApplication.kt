package de.shakie.billcheck

import android.app.Application
import de.shakie.billcheck.data.BillCheckDatabase
import de.shakie.billcheck.data.BillCheckRepository
import de.shakie.billcheck.data.OpenExchangeRateProvider
import de.shakie.billcheck.data.AiSettingsStore
import de.shakie.billcheck.data.GeminiAiExtractionProvider
import de.shakie.billcheck.data.LocalTextRecognizer
import de.shakie.billcheck.data.GeminiModelCatalog
import de.shakie.billcheck.data.DataTransferManager
import de.shakie.billcheck.update.AppUpdateManager

class BillCheckApplication : Application() {
    val database by lazy { BillCheckDatabase.create(this) }
    val repository by lazy { BillCheckRepository(database) }
    val exchangeRateProvider by lazy { OpenExchangeRateProvider(this) }
    val aiSettingsStore by lazy { AiSettingsStore(this) }
    val aiExtractionProvider by lazy { GeminiAiExtractionProvider(this) }
    val localTextRecognizer by lazy { LocalTextRecognizer(this) }
    val geminiModelCatalog by lazy { GeminiModelCatalog() }
    val dataTransferManager by lazy { DataTransferManager(this, database) }
    val appUpdateManager by lazy { AppUpdateManager(this) }
}
