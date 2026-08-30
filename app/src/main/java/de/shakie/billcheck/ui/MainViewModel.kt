package de.shakie.billcheck.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.shakie.billcheck.BillCheckApplication
import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.NewReceiptItem
import de.shakie.billcheck.data.ReceiptWithItems
import de.shakie.billcheck.data.ReconciliationWithLines
import de.shakie.billcheck.data.StatementLineEntity
import de.shakie.billcheck.data.TripEntity
import de.shakie.billcheck.data.TripCurrencyEntity
import de.shakie.billcheck.data.TripCurrencyInput
import de.shakie.billcheck.data.NewStatementLine
import de.shakie.billcheck.domain.MoneyCalculator
import de.shakie.billcheck.domain.CurrencyAmount
import de.shakie.billcheck.domain.CurrencyCatalog
import de.shakie.billcheck.domain.ExchangeRateQuote
import de.shakie.billcheck.domain.RankedReceiptCandidate
import de.shakie.billcheck.domain.AiDocumentType
import de.shakie.billcheck.domain.AiExtractionProvider
import de.shakie.billcheck.domain.AiExtractionResult
import de.shakie.billcheck.domain.ExtractedReceipt
import de.shakie.billcheck.domain.ExtractedStatement
import de.shakie.billcheck.data.OcrPage
import de.shakie.billcheck.data.GeminiModelInfo
import de.shakie.billcheck.data.LocalAiAuthType
import de.shakie.billcheck.data.LocalAiConnectionResult
import de.shakie.billcheck.data.LocalAiSettings
import de.shakie.billcheck.data.AI_PROVIDER_GEMINI
import de.shakie.billcheck.data.AI_PROVIDER_LOCAL
import de.shakie.billcheck.data.ExportFormat
import de.shakie.billcheck.data.ImportPreview
import de.shakie.billcheck.BillCheckWidget
import de.shakie.billcheck.update.UpdateCheckStatus
import de.shakie.billcheck.update.UpdateRelease
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job

enum class AppUpdateStatus {
    IDLE,
    CHECKING,
    NO_RELEASE,
    UP_TO_DATE,
    AVAILABLE,
    NO_COMPATIBLE_ASSET,
    DOWNLOADING,
    READY_TO_INSTALL,
    ERROR,
}

data class AppUpdateState(
    val status: AppUpdateStatus = AppUpdateStatus.IDLE,
    val release: UpdateRelease? = null,
    val message: String? = null,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val downloadedFilePath: String? = null,
)

data class MainUiState(
    val trips: List<TripEntity> = emptyList(),
    val selectedTrip: TripEntity? = null,
    val receipts: List<ReceiptWithItems> = emptyList(),
    val tripCurrencies: List<TripCurrencyEntity> = emptyList(),
    val exactHomeMinor: Long = 0,
    val roundedHomeMajor: Long = 0,
    val defaultHomeCurrencyCode: String = "EUR",
    val recentCurrencyCodes: List<String> = emptyList(),
    val locationSuggestions: List<String> = emptyList(),
    val itemNameSuggestions: List<String> = emptyList(),
    val reconciliations: List<ReconciliationWithLines> = emptyList(),
)

data class CandidateSelectionState(
    val lineId: String? = null,
    val candidates: List<RankedReceiptCandidate> = emptyList(),
    val loading: Boolean = false,
)

private data class ReceiptTextSuggestions(
    val locations: List<String> = emptyList(),
    val itemNames: List<String> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val billCheckApplication = application as BillCheckApplication
    private val repository = billCheckApplication.repository
    private val exchangeRateProvider = billCheckApplication.exchangeRateProvider
    private val aiSettingsStore = billCheckApplication.aiSettingsStore
    private val geminiAiExtractionProvider = billCheckApplication.geminiAiExtractionProvider
    private val localAiExtractionProvider = billCheckApplication.localAiExtractionProvider
    private val localTextRecognizer = billCheckApplication.localTextRecognizer
    private val geminiModelCatalog = billCheckApplication.geminiModelCatalog
    private val localAiSettingsStore = billCheckApplication.localAiSettingsStore
    private val localAiConnectionTester = billCheckApplication.localAiConnectionTester
    private val dataTransferManager = billCheckApplication.dataTransferManager
    private val appUpdateManager = billCheckApplication.appUpdateManager
    private val homeCurrencySettingsStore = billCheckApplication.homeCurrencySettingsStore
    private val widgetPreferences = application.getSharedPreferences(
        BillCheckWidget.SELECTED_TRIP_PREFERENCES,
        Context.MODE_PRIVATE,
    )
    private val selectedTripId = MutableStateFlow(
        widgetPreferences.getString(BillCheckWidget.SELECTED_TRIP_ID, null),
    )
    private val _exchangeRateLookup = MutableStateFlow<ExchangeRateLookupState>(ExchangeRateLookupState.Idle)
    val exchangeRateLookup: StateFlow<ExchangeRateLookupState> = _exchangeRateLookup.asStateFlow()
    private val _candidateSelection = MutableStateFlow(CandidateSelectionState())
    val candidateSelection: StateFlow<CandidateSelectionState> = _candidateSelection.asStateFlow()
    private val _aiSettings = MutableStateFlow(aiSettingsStore.read())
    val aiSettings = _aiSettings.asStateFlow()
    private val _aiExtraction = MutableStateFlow<AiExtractionState>(AiExtractionState.Idle)
    val aiExtraction = _aiExtraction.asStateFlow()
    private val _reconciliationAnalysis = MutableStateFlow<ReconciliationAnalysisState>(ReconciliationAnalysisState.Idle)
    val reconciliationAnalysis = _reconciliationAnalysis.asStateFlow()
    private val _localOcr = MutableStateFlow<LocalOcrState>(LocalOcrState.Idle)
    val localOcr = _localOcr.asStateFlow()
    private val _geminiModels = MutableStateFlow<GeminiModelsState>(GeminiModelsState.Idle)
    val geminiModels = _geminiModels.asStateFlow()
    private val _localAiSettings = MutableStateFlow(localAiSettingsStore.read())
    val localAiSettings = _localAiSettings.asStateFlow()
    private val _localAiConnection = MutableStateFlow<LocalAiConnectionState>(LocalAiConnectionState.Idle)
    val localAiConnection = _localAiConnection.asStateFlow()
    private val _transfer = MutableStateFlow<TransferState>(TransferState.Idle)
    val transfer = _transfer.asStateFlow()
    private val _appUpdate = MutableStateFlow(AppUpdateState())
    val appUpdate = _appUpdate.asStateFlow()
    private var updateDownloadJob: Job? = null
    private val currencyPreferences = MutableStateFlow(
        CurrencyPreferences(
            homeCurrencyCode = homeCurrencySettingsStore.read(),
            recentCurrencyCodes = homeCurrencySettingsStore.recentCurrencyCodes(),
        ),
    )

    private val trips = repository.trips.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val selectedTrip = combine(trips, selectedTripId) { currentTrips, selectedId ->
        currentTrips.firstOrNull { it.id == selectedId } ?: currentTrips.firstOrNull()
    }

    private val receipts = selectedTrip.flatMapLatest { trip ->
        trip?.let { repository.receipts(it.id) } ?: flowOf(emptyList())
    }

    private val tripCurrencies = selectedTrip.flatMapLatest { trip ->
        trip?.let { repository.tripCurrencies(it.id) } ?: flowOf(emptyList())
    }

    private val textSuggestions = selectedTrip.flatMapLatest { trip ->
        trip?.let {
            combine(
                repository.locationSuggestions(it.id),
                repository.itemNameSuggestions(it.id),
            ) { locations, itemNames -> ReceiptTextSuggestions(locations, itemNames) }
        } ?: flowOf(ReceiptTextSuggestions())
    }

    private val reconciliations = selectedTrip.flatMapLatest { trip ->
        trip?.let { repository.reconciliations(it.id) } ?: flowOf(emptyList())
    }

    private val uiCore = combine(
        trips,
        selectedTrip,
        receipts,
        textSuggestions,
        reconciliations,
    ) { currentTrips, currentTrip, currentReceipts, suggestions, currentReconciliations ->
        MainUiCore(currentTrips, currentTrip, currentReceipts, suggestions, currentReconciliations)
    }

    val uiState = combine(uiCore, tripCurrencies, currencyPreferences) { core, currencies, preferences ->
        val homeCode = core.selectedTrip?.homeCurrencyCode ?: preferences.homeCurrencyCode
        MainUiState(
            trips = core.trips,
            selectedTrip = core.selectedTrip,
            receipts = core.receipts,
            tripCurrencies = currencies,
            exactHomeMinor = MoneyCalculator.exactTripHomeMinor(core.receipts.map { it.receipt }),
            roundedHomeMajor = MoneyCalculator.roundedUpTripHomeMajor(
                core.receipts.map { it.receipt },
                homeCode,
            ),
            defaultHomeCurrencyCode = preferences.homeCurrencyCode,
            recentCurrencyCodes = preferences.recentCurrencyCodes,
            locationSuggestions = core.suggestions.locations,
            itemNameSuggestions = core.suggestions.itemNames,
            reconciliations = core.reconciliations,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MainUiState(),
    )

    init {
        viewModelScope.launch {
            uiState.collect { BillCheckWidget.updateAll(application) }
        }
    }

    fun selectTrip(id: String) {
        setSelectedTrip(id)
    }

    fun saveHomeCurrency(currencyCode: String) {
        runCatching { homeCurrencySettingsStore.save(currencyCode) }.onSuccess {
            currencyPreferences.value = CurrencyPreferences(
                homeCurrencyCode = homeCurrencySettingsStore.read(),
                recentCurrencyCodes = homeCurrencySettingsStore.recentCurrencyCodes(),
            )
        }
    }

    fun createTrip(
        name: String,
        homeCurrencyCode: String,
        currencies: List<TripCurrencyInput>,
        defaultTipMinor: Long,
        defaultTipCurrencyCode: String,
        defaultTipSelected: Boolean,
    ): Boolean {
        val normalizedCurrencies = normalizeTripCurrencies(homeCurrencyCode, currencies) ?: return false
        viewModelScope.launch {
            val trip = repository.createTrip(
                name = name,
                homeCurrencyCode = homeCurrencyCode,
                currencies = normalizedCurrencies,
                defaultTipMinor = defaultTipMinor,
                defaultTipCurrencyCode = defaultTipCurrencyCode,
                defaultTipSelected = defaultTipSelected,
            )
            normalizedCurrencies.forEach { homeCurrencySettingsStore.recordUsed(it.currencyCode) }
            refreshCurrencyPreferences()
            setSelectedTrip(trip.id)
        }
        return true
    }

    fun moveTrip(tripId: String, positions: Int) {
        if (positions == 0) return
        val orderedTrips = uiState.value.trips.toMutableList()
        val fromIndex = orderedTrips.indexOfFirst { it.id == tripId }
        if (fromIndex < 0) return
        val toIndex = (fromIndex + positions).coerceIn(0, orderedTrips.lastIndex)
        if (fromIndex == toIndex) return
        val moved = orderedTrips.removeAt(fromIndex)
        orderedTrips.add(toIndex, moved)
        viewModelScope.launch { repository.reorderTrips(orderedTrips.map { it.id }) }
    }

    fun deleteTrip(trip: TripEntity) {
        val currentTrips = uiState.value.trips
        val index = currentTrips.indexOfFirst { it.id == trip.id }
        if (index < 0) return
        val replacement = currentTrips.getOrNull(index + 1) ?: currentTrips.getOrNull(index - 1)
        viewModelScope.launch {
            repository.deleteTrip(trip)
            if (replacement == null) {
                selectedTripId.value = null
                widgetPreferences.edit().remove(BillCheckWidget.SELECTED_TRIP_ID).apply()
                BillCheckWidget.updateAll(getApplication())
            } else {
                setSelectedTrip(replacement.id)
            }
        }
    }

    fun updateTrip(
        existing: TripEntity,
        name: String,
        currencies: List<TripCurrencyInput>,
        defaultTipMinor: Long,
        defaultTipCurrencyCode: String,
        defaultTipSelected: Boolean,
    ): Boolean {
        val normalizedCurrencies = normalizeTripCurrencies(existing.homeCurrencyCode, currencies)
            ?: return false

        viewModelScope.launch {
            repository.updateTrip(
                existing = existing,
                name = name,
                currencies = normalizedCurrencies,
                defaultTipMinor = defaultTipMinor,
                defaultTipCurrencyCode = defaultTipCurrencyCode,
                defaultTipSelected = defaultTipSelected,
            )
            normalizedCurrencies.forEach { homeCurrencySettingsStore.recordUsed(it.currencyCode) }
            refreshCurrencyPreferences()
        }
        return true
    }

    fun addCurrencyToSelectedTrip(
        currencyCode: String,
        homeToCurrencyRate: String,
        useDailyRate: Boolean,
    ): Boolean {
        val trip = uiState.value.selectedTrip ?: return false
        if (uiState.value.tripCurrencies.any { it.currencyCode == currencyCode }) return true
        val inputs = uiState.value.tripCurrencies.map {
            TripCurrencyInput(it.currencyCode, it.homeToCurrencyRate, it.exchangeRateMode, it.isDefault)
        } + TripCurrencyInput(
            currencyCode = currencyCode,
            homeToCurrencyRate = homeToCurrencyRate,
            exchangeRateMode = if (useDailyRate) "DAILY" else "FIXED",
            isDefault = false,
        )
        return updateTrip(
            existing = trip,
            name = trip.name,
            currencies = inputs,
            defaultTipMinor = trip.defaultTipMinor,
            defaultTipCurrencyCode = trip.defaultTipCurrencyCode,
            defaultTipSelected = trip.defaultTipSelected,
        )
    }

    fun requestExchangeRate(baseCurrencyCode: String, targetCurrencyCode: String) {
        val base = baseCurrencyCode.trim().uppercase(Locale.ROOT)
        val target = targetCurrencyCode.trim().uppercase(Locale.ROOT)
        if (base.length != 3 || target.length != 3) return
        _exchangeRateLookup.value = ExchangeRateLookupState.Loading(base, target)
        viewModelScope.launch {
            runCatching { exchangeRateProvider.latestRate(base, target) }
                .onSuccess { quote ->
                    val loading = _exchangeRateLookup.value as? ExchangeRateLookupState.Loading
                    if (loading?.base == base && loading.target == target) {
                        _exchangeRateLookup.value = ExchangeRateLookupState.Success(quote)
                    }
                }
                .onFailure {
                    val loading = _exchangeRateLookup.value as? ExchangeRateLookupState.Loading
                    if (loading?.base == base && loading.target == target) {
                        _exchangeRateLookup.value = ExchangeRateLookupState.Error(base, target)
                    }
                }
        }
    }

    fun clearExchangeRateLookup() {
        _exchangeRateLookup.value = ExchangeRateLookupState.Idle
    }

    fun addReceipt(
        location: String,
        checkNumber: String,
        amountText: String,
        currencyCode: String,
        occurredOnText: String,
        addDefaultTip: Boolean,
        itemDrafts: List<ReceiptItemDraft>,
        imageUri: String? = null,
    ): Boolean {
        val trip = uiState.value.selectedTrip ?: return false
        val currency = uiState.value.tripCurrencies.firstOrNull { it.currencyCode == currencyCode }
            ?: return false
        val tipCurrency = if (addDefaultTip && trip.defaultTipMinor > 0) {
            uiState.value.tripCurrencies.firstOrNull {
                it.currencyCode == trip.defaultTipCurrencyCode
            } ?: return false
        } else {
            null
        }
        val input = parseReceiptInput(amountText, itemDrafts, currency.currencyCode) ?: return false
        val occurredAt = parseReceiptDate(occurredOnText) ?: return false
        viewModelScope.launch {
            val receiptRate = currentRate(trip, currency)
            val tipMinor = if (addDefaultTip) trip.defaultTipMinor else 0
            val tipRate = tipCurrency?.let { currentRate(trip, it) } ?: "1"
            repository.addReceipt(
                trip = trip,
                location = location,
                checkNumber = checkNumber,
                amountMinor = input.totalMinor,
                currencyCode = currency.currencyCode,
                exchangeRateSnapshot = receiptRate,
                tipMinor = tipMinor,
                tipCurrencyCode = tipCurrency?.currencyCode ?: trip.homeCurrencyCode,
                tipExchangeRateSnapshot = tipRate,
                items = input.items,
                imageUri = imageUri,
                occurredAt = occurredAt,
            )
            homeCurrencySettingsStore.recordUsed(currency.currencyCode)
            refreshCurrencyPreferences()
        }
        return true
    }

    fun deleteReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch { repository.deleteReceipt(receipt) }
    }

    fun updateReceipt(
        existing: ReceiptWithItems,
        location: String,
        checkNumber: String,
        amountText: String,
        currencyCode: String,
        occurredOnText: String,
        addDefaultTip: Boolean,
        itemDrafts: List<ReceiptItemDraft>,
    ): Boolean {
        val trip = uiState.value.selectedTrip ?: return false
        val currency = uiState.value.tripCurrencies.firstOrNull { it.currencyCode == currencyCode }
            ?: return false
        val input = parseReceiptInput(amountText, itemDrafts, currency.currencyCode) ?: return false
        val occurredAt = parseReceiptDate(occurredOnText) ?: return false
        viewModelScope.launch {
            val old = existing.receipt
            val tipMinor = when {
                !addDefaultTip -> 0
                old.tipMinor > 0 -> old.tipMinor
                else -> trip.defaultTipMinor
            }
            val tipCurrencyCode = when {
                tipMinor == 0L -> trip.homeCurrencyCode
                old.tipMinor > 0 -> old.tipCurrencyCode
                else -> trip.defaultTipCurrencyCode
            }
            val tipCurrency = uiState.value.tripCurrencies.first { it.currencyCode == tipCurrencyCode }
            repository.updateReceipt(
                trip = trip,
                existing = old,
                location = location,
                checkNumber = checkNumber,
                amountMinor = input.totalMinor,
                currencyCode = currency.currencyCode,
                exchangeRateSnapshot = if (currency.currencyCode == old.currencyCode) null else currentRate(trip, currency),
                occurredAt = occurredAt,
                tipMinor = tipMinor,
                tipCurrencyCode = tipCurrency.currencyCode,
                tipExchangeRateSnapshot = when {
                    tipMinor == 0L -> if (old.tipCurrencyCode == trip.homeCurrencyCode) null else "1"
                    tipCurrency.currencyCode == old.tipCurrencyCode -> null
                    else -> currentRate(trip, tipCurrency)
                },
                items = input.items,
            )
            homeCurrencySettingsStore.recordUsed(currency.currencyCode)
            refreshCurrencyPreferences()
        }
        return true
    }

    fun updateReceiptImage(receiptId: String, imageUri: String?) {
        viewModelScope.launch { repository.updateReceiptImage(receiptId, imageUri) }
    }

    fun saveAiSettings(providerId: String, apiKey: String?, model: String) {
        aiSettingsStore.save(providerId, apiKey, model)
        _aiSettings.value = aiSettingsStore.read()
    }

    fun clearAiApiKey() {
        aiSettingsStore.clearApiKey()
        _aiSettings.value = aiSettingsStore.read()
    }

    fun saveLocalAiSettings(
        baseUrl: String,
        model: String,
        authType: LocalAiAuthType,
        username: String,
        credential: String?,
    ) {
        localAiSettingsStore.save(
            LocalAiSettings(
                baseUrl = baseUrl,
                model = model,
                authType = authType,
                username = username,
            ),
            credential,
        )
        _localAiSettings.value = localAiSettingsStore.read()
        _localAiConnection.value = LocalAiConnectionState.Idle
    }

    fun clearLocalAiCredential() {
        localAiSettingsStore.clearCredential()
        _localAiSettings.value = localAiSettingsStore.read()
        _localAiConnection.value = LocalAiConnectionState.Idle
    }

    fun clearLocalAiConnectionResult() {
        if (_localAiConnection.value !is LocalAiConnectionState.Testing) {
            _localAiConnection.value = LocalAiConnectionState.Idle
        }
    }

    fun testLocalAiConnection(
        baseUrl: String,
        model: String,
        authType: LocalAiAuthType,
        username: String,
        credential: String?,
    ) {
        val accessCredential = credential?.takeIf(String::isNotBlank)
            ?: localAiSettingsStore.credential()
        if (accessCredential.isNullOrBlank()) {
            _localAiConnection.value = LocalAiConnectionState.MissingCredential
            return
        }
        val settings = LocalAiSettings(
            baseUrl = baseUrl,
            model = model,
            authType = authType,
            username = username,
        )
        _localAiConnection.value = LocalAiConnectionState.Testing
        viewModelScope.launch {
            runCatching { localAiConnectionTester.test(settings, accessCredential) }
                .onSuccess { _localAiConnection.value = LocalAiConnectionState.Success(it) }
                .onFailure {
                    _localAiConnection.value = LocalAiConnectionState.Error(
                        it.message?.take(200).orEmpty(),
                    )
                }
        }
    }

    fun loadGeminiModels() {
        val key = aiSettingsStore.apiKey()
        if (key.isNullOrBlank()) {
            _geminiModels.value = GeminiModelsState.MissingApiKey
            return
        }
        _geminiModels.value = GeminiModelsState.Loading
        viewModelScope.launch {
            runCatching { geminiModelCatalog.list(key) }
                .onSuccess { _geminiModels.value = GeminiModelsState.Success(it) }
                .onFailure {
                    _geminiModels.value = GeminiModelsState.Error(it.message.orEmpty())
                }
        }
    }

    fun analyzeReceipt(imageUri: String, expectedCurrencyCode: String? = null) {
        val trip = uiState.value.selectedTrip ?: return
        val runtime = activeAiRuntime()
        if (runtime == null) {
            _aiExtraction.value = AiExtractionState.MissingCredential
            return
        }
        _aiExtraction.value = AiExtractionState.Loading(imageUri)
        viewModelScope.launch {
            runCatching {
                runtime.provider.extract(
                    imageUri = Uri.parse(imageUri),
                    documentType = AiDocumentType.RECEIPT,
                    expectedCurrencyCode = expectedCurrencyCode
                        ?: uiState.value.tripCurrencies.firstOrNull { it.isDefault }?.currencyCode
                        ?: trip.homeCurrencyCode,
                    apiKey = runtime.credential,
                    model = runtime.model,
                ) as AiExtractionResult.Receipt
            }.onSuccess {
                _aiExtraction.value = AiExtractionState.ReceiptSuccess(imageUri, it.value)
            }.onFailure {
                _aiExtraction.value = AiExtractionState.Error(
                    imageUri,
                    it.message?.take(300).orEmpty(),
                )
            }
        }
    }

    fun analyzeStatement(reconciliationId: String, imageUri: String) {
        val trip = uiState.value.selectedTrip ?: return
        val runtime = activeAiRuntime()
        if (runtime == null) {
            _aiExtraction.value = AiExtractionState.MissingCredential
            return
        }
        _aiExtraction.value = AiExtractionState.Loading(imageUri)
        viewModelScope.launch {
            runCatching {
                runtime.provider.extract(
                    imageUri = Uri.parse(imageUri),
                    documentType = AiDocumentType.STATEMENT,
                    expectedCurrencyCode = uiState.value.tripCurrencies
                        .firstOrNull { it.isDefault }?.currencyCode ?: trip.homeCurrencyCode,
                    apiKey = runtime.credential,
                    model = runtime.model,
                ) as AiExtractionResult.Statement
            }.onSuccess {
                _aiExtraction.value = AiExtractionState.StatementSuccess(
                    imageUri,
                    reconciliationId,
                    it.value,
                )
            }.onFailure {
                _aiExtraction.value = AiExtractionState.Error(
                    imageUri,
                    it.message?.take(300).orEmpty(),
                )
            }
        }
    }

    fun applyExtractedStatement() {
        val extraction = _aiExtraction.value as? AiExtractionState.StatementSuccess ?: return
        val trip = uiState.value.selectedTrip ?: return
        val reconciliation = uiState.value.reconciliations.firstOrNull {
            it.reconciliation.id == extraction.reconciliationId
        }?.reconciliation ?: return
        viewModelScope.launch {
            runCatching {
                repository.applyExtractedStatement(
                    reconciliation,
                    extraction.statement,
                    uiState.value.tripCurrencies.firstOrNull { it.isDefault }?.currencyCode
                        ?: trip.homeCurrencyCode,
                )
            }.onSuccess {
                _aiExtraction.value = AiExtractionState.Idle
            }.onFailure { error ->
                _aiExtraction.value = AiExtractionState.Error(
                    extraction.imageUri,
                    error.message?.take(1_000).orEmpty(),
                )
            }
        }
    }

    fun clearAiExtraction() {
        _aiExtraction.value = AiExtractionState.Idle
    }

    fun analyzeLocally(imageUri: String) {
        _localOcr.value = LocalOcrState.Loading(imageUri)
        viewModelScope.launch {
            runCatching { localTextRecognizer.recognize(Uri.parse(imageUri)) }
                .onSuccess { _localOcr.value = LocalOcrState.Success(imageUri, it) }
                .onFailure {
                    Log.e("BillCheckOcr", "Offline text recognition failed", it)
                    _localOcr.value = LocalOcrState.Error(
                        imageUri,
                        it.message?.take(300).orEmpty(),
                    )
                }
        }
    }

    fun clearLocalOcr() {
        _localOcr.value = LocalOcrState.Idle
    }

    fun exportData(uri: Uri, tripIds: Set<String>, format: ExportFormat) {
        _transfer.value = TransferState.Working
        viewModelScope.launch {
            runCatching { dataTransferManager.export(uri, tripIds, format) }
                .onSuccess { _transfer.value = TransferState.ExportSuccess(format) }
                .onFailure { _transfer.value = TransferState.Error(it.message.orEmpty()) }
        }
    }

    fun previewImport(uri: Uri) {
        _transfer.value = TransferState.Working
        viewModelScope.launch {
            runCatching { dataTransferManager.previewImport(uri) }
                .onSuccess { _transfer.value = TransferState.ImportReady(it) }
                .onFailure { _transfer.value = TransferState.Error(it.message.orEmpty()) }
        }
    }

    fun importSelectedTrips(sourceTripIds: Set<String>) {
        _transfer.value = TransferState.Working
        viewModelScope.launch {
            runCatching { dataTransferManager.importSelected(sourceTripIds) }
                .onSuccess { importedIds ->
                    importedIds.firstOrNull()?.let(::setSelectedTrip)
                    _transfer.value = TransferState.ImportSuccess(importedIds.size)
                }
                .onFailure { _transfer.value = TransferState.Error(it.message.orEmpty()) }
        }
    }

    fun clearTransferState() {
        if (_transfer.value is TransferState.ImportReady) dataTransferManager.clearPendingImport()
        _transfer.value = TransferState.Idle
    }

    fun checkForAppUpdate(force: Boolean) {
        if (_appUpdate.value.status == AppUpdateStatus.CHECKING) return
        viewModelScope.launch {
            _appUpdate.value = AppUpdateState(status = AppUpdateStatus.CHECKING)
            val result = appUpdateManager.check(force)
            val release = result.release
            val downloaded = release?.let(appUpdateManager::downloadedApkFor)
            _appUpdate.value = when (result.status) {
                UpdateCheckStatus.UPDATE_AVAILABLE -> AppUpdateState(
                    status = if (downloaded != null) {
                        AppUpdateStatus.READY_TO_INSTALL
                    } else {
                        AppUpdateStatus.AVAILABLE
                    },
                    release = release,
                    downloadedFilePath = downloaded?.absolutePath,
                )
                UpdateCheckStatus.UP_TO_DATE -> AppUpdateState(
                    status = if (release == null && !force) {
                        AppUpdateStatus.IDLE
                    } else {
                        AppUpdateStatus.UP_TO_DATE
                    },
                    release = release,
                )
                UpdateCheckStatus.NO_RELEASE -> AppUpdateState(status = AppUpdateStatus.NO_RELEASE)
                UpdateCheckStatus.NO_COMPATIBLE_ASSET -> AppUpdateState(
                    status = AppUpdateStatus.NO_COMPATIBLE_ASSET,
                    release = release,
                )
                UpdateCheckStatus.CHECK_FAILED -> AppUpdateState(
                    status = AppUpdateStatus.ERROR,
                    message = result.message,
                )
            }
        }
    }

    fun downloadAppUpdate() {
        val release = _appUpdate.value.release ?: return
        val asset = release.compatibleAsset ?: return
        if (updateDownloadJob?.isActive == true) return
        updateDownloadJob = viewModelScope.launch {
            _appUpdate.value = AppUpdateState(
                status = AppUpdateStatus.DOWNLOADING,
                release = release,
                totalBytes = asset.sizeBytes,
            )
            runCatching {
                appUpdateManager.download(release, asset) { downloaded, total ->
                    _appUpdate.value = _appUpdate.value.copy(
                        downloadedBytes = downloaded,
                        totalBytes = total,
                    )
                }
            }.onSuccess { file ->
                _appUpdate.value = AppUpdateState(
                    status = AppUpdateStatus.READY_TO_INSTALL,
                    release = release,
                    downloadedFilePath = file.absolutePath,
                )
            }.onFailure { throwable ->
                if (throwable is kotlinx.coroutines.CancellationException) {
                    _appUpdate.value = AppUpdateState(AppUpdateStatus.AVAILABLE, release)
                } else {
                    _appUpdate.value = AppUpdateState(
                        status = AppUpdateStatus.ERROR,
                        release = release,
                        message = throwable.message ?: throwable.javaClass.simpleName,
                    )
                }
            }
        }
    }

    fun cancelAppUpdateDownload() {
        updateDownloadJob?.cancel()
    }

    fun deleteDownloadedAppUpdate() {
        val release = _appUpdate.value.release ?: return
        appUpdateManager.deleteDownloadedApk(release)
        _appUpdate.value = AppUpdateState(AppUpdateStatus.AVAILABLE, release)
    }

    private suspend fun currentRate(
        trip: TripEntity,
        currency: TripCurrencyEntity,
    ): String = when {
        currency.currencyCode == trip.homeCurrencyCode -> "1"
        currency.exchangeRateMode == "DAILY" -> runCatching {
            exchangeRateProvider.latestRate(
                trip.homeCurrencyCode,
                currency.currencyCode,
            ).targetUnitsPerBase
        }.getOrDefault(currency.homeToCurrencyRate)
        else -> currency.homeToCurrencyRate
    }

    private fun refreshCurrencyPreferences() {
        currencyPreferences.value = CurrencyPreferences(
            homeCurrencyCode = homeCurrencySettingsStore.read(),
            recentCurrencyCodes = homeCurrencySettingsStore.recentCurrencyCodes(),
        )
    }

    private fun setSelectedTrip(id: String) {
        selectedTripId.value = id
        widgetPreferences.edit().putString(BillCheckWidget.SELECTED_TRIP_ID, id).apply()
        BillCheckWidget.updateAll(getApplication())
    }

    fun createReconciliation(title: String, imageUri: String? = null) {
        val trip = uiState.value.selectedTrip ?: return
        viewModelScope.launch { repository.createReconciliation(trip, title, imageUri) }
    }

    fun updateReconciliation(id: String, title: String, imageUri: String?) {
        viewModelScope.launch { repository.updateReconciliation(id, title, imageUri) }
    }

    fun addStatementLine(
        reconciliationId: String,
        description: String,
        checkNumber: String,
        amountText: String,
        currencyCode: String,
        occurredOn: Long? = null,
    ): Boolean {
        if (currencyCode.trim().uppercase(Locale.ROOT) !in supportedCurrencyCodes) return false
        val amountMinor = parseMinor(amountText, currencyCode)?.takeIf { it > 0 } ?: return false
        viewModelScope.launch {
            repository.addStatementLine(
                reconciliationId,
                NewStatementLine(description, checkNumber, amountMinor, currencyCode, occurredOn),
            )
        }
        return true
    }

    fun updateStatementLine(
        existing: StatementLineEntity,
        description: String,
        checkNumber: String,
        amountText: String,
        currencyCode: String,
        occurredOn: Long? = null,
    ): Boolean {
        if (currencyCode.trim().uppercase(Locale.ROOT) !in supportedCurrencyCodes) return false
        val amountMinor = parseMinor(amountText, currencyCode)?.takeIf { it > 0 } ?: return false
        viewModelScope.launch {
            repository.updateStatementLine(
                existing,
                NewStatementLine(description, checkNumber, amountMinor, currencyCode, occurredOn),
            )
        }
        return true
    }

    fun deleteStatementLine(line: StatementLineEntity) {
        viewModelScope.launch { repository.deleteStatementLine(line) }
    }

    fun setStatementLineAccepted(line: StatementLineEntity, accepted: Boolean) {
        viewModelScope.launch { repository.setAccepted(line, accepted) }
    }

    fun loadCandidates(line: StatementLineEntity) {
        val tripId = uiState.value.selectedTrip?.id ?: return
        _candidateSelection.value = CandidateSelectionState(line.id, loading = true)
        viewModelScope.launch {
            val candidates = repository.rankCandidates(tripId, line)
            if (_candidateSelection.value.lineId == line.id) {
                _candidateSelection.value = CandidateSelectionState(line.id, candidates)
            }
        }
    }

    fun clearCandidateSelection() {
        _candidateSelection.value = CandidateSelectionState()
    }

    fun assignReceipt(line: StatementLineEntity, receipt: ReceiptEntity) {
        viewModelScope.launch {
            repository.assignReceipt(line, receipt, manually = true)
            clearCandidateSelection()
        }
    }

    fun clearLineMatch(line: StatementLineEntity) {
        viewModelScope.launch { repository.clearLineMatch(line) }
    }

    fun runReconciliation(reconciliation: ReconciliationWithLines) {
        val tripId = uiState.value.selectedTrip?.id ?: return
        _reconciliationAnalysis.value = ReconciliationAnalysisState.Running(
            reconciliation.reconciliation.id,
        )
        viewModelScope.launch {
            runCatching {
                val report = repository.runAutomaticReconciliation(tripId, reconciliation)
                val runtime = activeAiRuntime()
                if (runtime != null) {
                    val summary = runtime.provider.summarizeReconciliation(
                        report = report,
                        apiKey = runtime.credential,
                        model = runtime.model,
                    )
                    repository.storeReconciliationSummary(report.reconciliationId, summary)
                }
            }.onSuccess {
                _reconciliationAnalysis.value = ReconciliationAnalysisState.Idle
            }.onFailure { error ->
                _reconciliationAnalysis.value = ReconciliationAnalysisState.Error(
                    reconciliation.reconciliation.id,
                    error.message?.take(300).orEmpty(),
                )
            }
        }
    }

    fun resetReconciliation(reconciliation: ReconciliationWithLines) {
        viewModelScope.launch { repository.resetReconciliation(reconciliation) }
    }

    fun deleteReconciliation(reconciliationId: String) {
        viewModelScope.launch { repository.deleteReconciliation(reconciliationId) }
    }

    private fun activeAiRuntime(): AiRuntime? {
        val settings = aiSettingsStore.read()
        return when (settings.providerId) {
            AI_PROVIDER_LOCAL -> {
                val credential = localAiSettingsStore.credential()?.takeIf(String::isNotBlank)
                    ?: return null
                val localSettings = localAiSettingsStore.read()
                AiRuntime(localAiExtractionProvider, credential, localSettings.model)
            }
            AI_PROVIDER_GEMINI -> {
                val credential = aiSettingsStore.apiKey()?.takeIf(String::isNotBlank)
                    ?: return null
                AiRuntime(geminiAiExtractionProvider, credential, settings.model)
            }
            else -> null
        }
    }

    companion object {
        fun parseMinor(value: String, currencyCode: String = "EUR"): Long? =
            CurrencyAmount.parseMajorToMinor(value, currencyCode)?.takeIf { it >= 0 }

        fun parseReceiptInput(
            totalText: String,
            drafts: List<ReceiptItemDraft>,
            currencyCode: String = "EUR",
        ): ParsedReceiptInput? {
            val items = buildList {
                drafts.filterNot { it.name.isBlank() && it.amountText.isBlank() }.forEach { draft ->
                    val amountMinor = parseMinor(draft.amountText, currencyCode) ?: return null
                    add(
                        NewReceiptItem(
                            name = draft.name.trim().ifBlank { "Posten" },
                            amountMinor = amountMinor,
                        ),
                    )
                }
            }
            val totalMinor = parseMinor(totalText, currencyCode)
                ?: items.takeIf { it.isNotEmpty() }?.let { parsedItems ->
                    runCatching {
                        parsedItems.fold(0L) { total, item -> Math.addExact(total, item.amountMinor) }
                    }.getOrNull()
                }
                ?: return null
            return ParsedReceiptInput(totalMinor, items).takeIf { it.totalMinor > 0 }
        }

        fun parseReceiptDate(value: String): Long? {
            val trimmed = value.trim()
            val date = sequenceOf(
                DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT),
                DateTimeFormatter.ISO_LOCAL_DATE,
            ).mapNotNull { formatter ->
                runCatching { LocalDate.parse(trimmed, formatter) }.getOrNull()
            }.firstOrNull() ?: return null
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        private fun normalizeTripCurrencies(
            homeCurrencyCode: String,
            currencies: List<TripCurrencyInput>,
        ): List<TripCurrencyInput>? = runCatching {
            val home = CurrencyAmount.normalizeCode(homeCurrencyCode)
            require(home in supportedCurrencyCodes)
            val normalized = currencies.map { input ->
                val code = CurrencyAmount.normalizeCode(input.currencyCode)
                require(code in supportedCurrencyCodes)
                val rate = if (code == home) {
                    "1"
                } else {
                    normalizeDecimal(input.homeToCurrencyRate)
                        ?.toBigDecimalOrNull()
                        ?.takeIf { it > BigDecimal.ZERO }
                        ?.stripTrailingZeros()
                        ?.toPlainString()
                        ?: error("Invalid exchange rate")
                }
                input.copy(
                    currencyCode = code,
                    homeToCurrencyRate = rate,
                    exchangeRateMode = if (code == home) "FIXED" else input.exchangeRateMode,
                )
            }
            require(normalized.map { it.currencyCode }.distinct().size == normalized.size)
            require(normalized.any { it.currencyCode == home })
            require(normalized.count { it.isDefault } == 1)
            normalized
        }.getOrNull()

        private val supportedCurrencyCodes by lazy {
            CurrencyCatalog.entries(Locale.ROOT).mapTo(hashSetOf()) { it.code }
        }

        private fun normalizeDecimal(value: String): String? = value
            .trim()
            .replace(" ", "")
            .replace(',', '.')
            .takeIf { it.isNotBlank() }
    }
}

data class ReceiptItemDraft(
    val name: String,
    val amountText: String,
)

data class ParsedReceiptInput(
    val totalMinor: Long,
    val items: List<NewReceiptItem>,
)

sealed interface ExchangeRateLookupState {
    data object Idle : ExchangeRateLookupState
    data class Loading(val base: String, val target: String) : ExchangeRateLookupState
    data class Success(val quote: ExchangeRateQuote) : ExchangeRateLookupState
    data class Error(val base: String, val target: String) : ExchangeRateLookupState
}

sealed interface AiExtractionState {
    data object Idle : AiExtractionState
    data object MissingCredential : AiExtractionState
    data class Loading(val imageUri: String) : AiExtractionState
    data class ReceiptSuccess(val imageUri: String, val receipt: ExtractedReceipt) : AiExtractionState
    data class StatementSuccess(
        val imageUri: String,
        val reconciliationId: String,
        val statement: ExtractedStatement,
    ) : AiExtractionState
    data class Error(val imageUri: String, val message: String) : AiExtractionState
}

sealed interface ReconciliationAnalysisState {
    data object Idle : ReconciliationAnalysisState
    data class Running(val reconciliationId: String) : ReconciliationAnalysisState
    data class Error(val reconciliationId: String, val message: String) : ReconciliationAnalysisState
}

sealed interface LocalOcrState {
    data object Idle : LocalOcrState
    data class Loading(val imageUri: String) : LocalOcrState
    data class Success(val imageUri: String, val page: OcrPage) : LocalOcrState
    data class Error(val imageUri: String, val message: String) : LocalOcrState
}

sealed interface GeminiModelsState {
    data object Idle : GeminiModelsState
    data object Loading : GeminiModelsState
    data object MissingApiKey : GeminiModelsState
    data class Success(val models: List<GeminiModelInfo>) : GeminiModelsState
    data class Error(val message: String) : GeminiModelsState
}

private data class AiRuntime(
    val provider: AiExtractionProvider,
    val credential: String,
    val model: String,
)

private data class CurrencyPreferences(
    val homeCurrencyCode: String,
    val recentCurrencyCodes: List<String>,
)

private data class MainUiCore(
    val trips: List<TripEntity>,
    val selectedTrip: TripEntity?,
    val receipts: List<ReceiptWithItems>,
    val suggestions: ReceiptTextSuggestions,
    val reconciliations: List<ReconciliationWithLines>,
)

sealed interface LocalAiConnectionState {
    data object Idle : LocalAiConnectionState
    data object Testing : LocalAiConnectionState
    data object MissingCredential : LocalAiConnectionState
    data class Success(val result: LocalAiConnectionResult) : LocalAiConnectionState
    data class Error(val message: String) : LocalAiConnectionState
}

sealed interface TransferState {
    data object Idle : TransferState
    data object Working : TransferState
    data class ExportSuccess(val format: ExportFormat) : TransferState
    data class ImportReady(val preview: ImportPreview) : TransferState
    data class ImportSuccess(val tripCount: Int) : TransferState
    data class Error(val message: String) : TransferState
}
