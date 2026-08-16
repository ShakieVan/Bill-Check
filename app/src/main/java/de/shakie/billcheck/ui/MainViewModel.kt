package de.shakie.billcheck.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.shakie.billcheck.BillCheckApplication
import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.NewReceiptItem
import de.shakie.billcheck.data.ReceiptWithItems
import de.shakie.billcheck.data.ReconciliationWithLines
import de.shakie.billcheck.data.StatementLineEntity
import de.shakie.billcheck.data.TripEntity
import de.shakie.billcheck.data.NewStatementLine
import de.shakie.billcheck.domain.MoneyCalculator
import de.shakie.billcheck.domain.ExchangeRateQuote
import de.shakie.billcheck.domain.RankedReceiptCandidate
import de.shakie.billcheck.domain.AiDocumentType
import de.shakie.billcheck.domain.AiExtractionResult
import de.shakie.billcheck.domain.ExtractedReceipt
import de.shakie.billcheck.domain.ExtractedStatement
import de.shakie.billcheck.data.OcrToken
import de.shakie.billcheck.data.GeminiModelInfo
import de.shakie.billcheck.data.ExportFormat
import de.shakie.billcheck.data.ImportPreview
import de.shakie.billcheck.BillCheckWidget
import de.shakie.billcheck.update.UpdateCheckStatus
import de.shakie.billcheck.update.UpdateRelease
import java.math.BigDecimal
import java.math.RoundingMode
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
    val exactEuroCents: Long = 0,
    val roundedEuro: Long = 0,
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
    private val aiExtractionProvider = billCheckApplication.aiExtractionProvider
    private val localTextRecognizer = billCheckApplication.localTextRecognizer
    private val geminiModelCatalog = billCheckApplication.geminiModelCatalog
    private val dataTransferManager = billCheckApplication.dataTransferManager
    private val appUpdateManager = billCheckApplication.appUpdateManager
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
    private val _localOcr = MutableStateFlow<LocalOcrState>(LocalOcrState.Idle)
    val localOcr = _localOcr.asStateFlow()
    private val _geminiModels = MutableStateFlow<GeminiModelsState>(GeminiModelsState.Idle)
    val geminiModels = _geminiModels.asStateFlow()
    private val _transfer = MutableStateFlow<TransferState>(TransferState.Idle)
    val transfer = _transfer.asStateFlow()
    private val _appUpdate = MutableStateFlow(AppUpdateState())
    val appUpdate = _appUpdate.asStateFlow()
    private var updateDownloadJob: Job? = null

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

    val uiState = combine(
        trips,
        selectedTrip,
        receipts,
        textSuggestions,
        reconciliations,
    ) { currentTrips, currentTrip, currentReceipts, suggestions, currentReconciliations ->
        MainUiState(
            trips = currentTrips,
            selectedTrip = currentTrip,
            receipts = currentReceipts,
            exactEuroCents = MoneyCalculator.exactTripEuroCents(currentReceipts.map { it.receipt }),
            roundedEuro = MoneyCalculator.roundedUpTripEuro(currentReceipts.map { it.receipt }),
            locationSuggestions = suggestions.locations,
            itemNameSuggestions = suggestions.itemNames,
            reconciliations = currentReconciliations,
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

    fun createTrip(
        name: String,
        currencyCode: String,
        exchangeRate: String,
        useDailyRate: Boolean,
        defaultTipMinor: Long,
        defaultTipCurrencyCode: String,
    ) {
        viewModelScope.launch {
            val trip = repository.createTrip(
                name = name,
                currencyCode = currencyCode,
                exchangeRate = normalizeDecimal(exchangeRate) ?: "55.5",
                exchangeRateMode = if (useDailyRate) "DAILY" else "FIXED",
                defaultTipMinor = defaultTipMinor,
                defaultTipCurrencyCode = defaultTipCurrencyCode,
            )
            setSelectedTrip(trip.id)
        }
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

    fun updateTrip(
        existing: TripEntity,
        name: String,
        currencyCode: String,
        exchangeRate: String,
        useDailyRate: Boolean,
        defaultTipMinor: Long,
        defaultTipCurrencyCode: String,
    ): Boolean {
        val normalizedRate = normalizeDecimal(exchangeRate)
            ?.toBigDecimalOrNull()
            ?.takeIf { it > BigDecimal.ZERO }
            ?.stripTrailingZeros()
            ?.toPlainString()
            ?: return false
        val normalizedCurrency = currencyCode.trim().uppercase(Locale.ROOT)
        val normalizedTipCurrency = defaultTipCurrencyCode.trim().uppercase(Locale.ROOT)
        if (normalizedCurrency.length != 3 || normalizedTipCurrency.length != 3) return false

        viewModelScope.launch {
            repository.updateTrip(
                existing = existing,
                name = name,
                currencyCode = normalizedCurrency,
                exchangeRate = normalizedRate,
                exchangeRateMode = if (useDailyRate) "DAILY" else "FIXED",
                defaultTipMinor = defaultTipMinor,
                defaultTipCurrencyCode = normalizedTipCurrency,
            )
        }
        return true
    }

    fun requestExchangeRate(currencyCode: String) {
        val target = currencyCode.trim().uppercase(Locale.ROOT)
        if (target.length != 3) return
        _exchangeRateLookup.value = ExchangeRateLookupState.Loading(target)
        viewModelScope.launch {
            runCatching { exchangeRateProvider.latestForeignPerEuro(target) }
                .onSuccess { quote ->
                    if ((_exchangeRateLookup.value as? ExchangeRateLookupState.Loading)?.target == target) {
                        _exchangeRateLookup.value = ExchangeRateLookupState.Success(quote)
                    }
                }
                .onFailure {
                    if ((_exchangeRateLookup.value as? ExchangeRateLookupState.Loading)?.target == target) {
                        _exchangeRateLookup.value = ExchangeRateLookupState.Error(target)
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
        foreignAmountText: String,
        addDefaultTip: Boolean,
        itemDrafts: List<ReceiptItemDraft>,
        imageUri: String? = null,
    ): Boolean {
        val trip = uiState.value.selectedTrip ?: return false
        val input = parseReceiptInput(foreignAmountText, itemDrafts) ?: return false
        viewModelScope.launch {
            val receiptRate = if (trip.exchangeRateMode == "DAILY") {
                runCatching {
                    exchangeRateProvider.latestForeignPerEuro(trip.foreignCurrencyCode).foreignPerEuro
                }.getOrDefault(trip.defaultExchangeRate)
            } else {
                trip.defaultExchangeRate
            }
            repository.addReceipt(
                trip = trip,
                location = location,
                checkNumber = checkNumber,
                foreignAmountMinor = input.totalMinor,
                addDefaultTip = addDefaultTip,
                items = input.items,
                exchangeRate = receiptRate,
                imageUri = imageUri,
            )
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
        foreignAmountText: String,
        addDefaultTip: Boolean,
        itemDrafts: List<ReceiptItemDraft>,
    ): Boolean {
        val trip = uiState.value.selectedTrip ?: return false
        val input = parseReceiptInput(foreignAmountText, itemDrafts) ?: return false
        viewModelScope.launch {
            repository.updateReceipt(
                trip = trip,
                existing = existing.receipt,
                location = location,
                checkNumber = checkNumber,
                foreignAmountMinor = input.totalMinor,
                addDefaultTip = addDefaultTip,
                items = input.items,
            )
        }
        return true
    }

    fun updateReceiptImage(receiptId: String, imageUri: String?) {
        viewModelScope.launch { repository.updateReceiptImage(receiptId, imageUri) }
    }

    fun saveAiSettings(apiKey: String?, model: String) {
        aiSettingsStore.saveGemini(apiKey, model)
        _aiSettings.value = aiSettingsStore.read()
    }

    fun clearAiApiKey() {
        aiSettingsStore.clearApiKey()
        _aiSettings.value = aiSettingsStore.read()
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

    fun analyzeReceipt(imageUri: String) {
        val trip = uiState.value.selectedTrip ?: return
        val settings = aiSettingsStore.read()
        val key = aiSettingsStore.apiKey()
        if (key.isNullOrBlank()) {
            _aiExtraction.value = AiExtractionState.MissingApiKey
            return
        }
        _aiExtraction.value = AiExtractionState.Loading(imageUri)
        viewModelScope.launch {
            runCatching {
                aiExtractionProvider.extract(
                    imageUri = Uri.parse(imageUri),
                    documentType = AiDocumentType.RECEIPT,
                    expectedCurrencyCode = trip.foreignCurrencyCode,
                    apiKey = key,
                    model = settings.model,
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
        val settings = aiSettingsStore.read()
        val key = aiSettingsStore.apiKey()
        if (key.isNullOrBlank()) {
            _aiExtraction.value = AiExtractionState.MissingApiKey
            return
        }
        _aiExtraction.value = AiExtractionState.Loading(imageUri)
        viewModelScope.launch {
            runCatching {
                aiExtractionProvider.extract(
                    imageUri = Uri.parse(imageUri),
                    documentType = AiDocumentType.STATEMENT,
                    expectedCurrencyCode = trip.foreignCurrencyCode,
                    apiKey = key,
                    model = settings.model,
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
            repository.applyExtractedStatement(
                reconciliation,
                extraction.statement,
                trip.foreignCurrencyCode,
            )
            _aiExtraction.value = AiExtractionState.Idle
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
        val amountMinor = parseMinor(amountText)?.takeIf { it > 0 } ?: return false
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
        val amountMinor = parseMinor(amountText)?.takeIf { it > 0 } ?: return false
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
        viewModelScope.launch { repository.runAutomaticReconciliation(tripId, reconciliation) }
    }

    fun resetReconciliation(reconciliation: ReconciliationWithLines) {
        viewModelScope.launch { repository.resetReconciliation(reconciliation) }
    }

    fun deleteReconciliation(reconciliationId: String) {
        viewModelScope.launch { repository.deleteReconciliation(reconciliationId) }
    }

    companion object {
        fun parseMinor(value: String): Long? = normalizeDecimal(value)
            ?.toBigDecimalOrNull()
            ?.takeIf { it >= BigDecimal.ZERO }
            ?.movePointRight(2)
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.longValueExact()

        fun parseReceiptInput(
            totalText: String,
            drafts: List<ReceiptItemDraft>,
        ): ParsedReceiptInput? {
            val items = buildList {
                drafts.filterNot { it.name.isBlank() && it.amountText.isBlank() }.forEach { draft ->
                    val amountMinor = parseMinor(draft.amountText) ?: return null
                    add(
                        NewReceiptItem(
                            name = draft.name.trim().ifBlank { "Posten" },
                            amountMinor = amountMinor,
                        ),
                    )
                }
            }
            val totalMinor = parseMinor(totalText)
                ?: items.takeIf { it.isNotEmpty() }?.sumOf { it.amountMinor }
                ?: return null
            return ParsedReceiptInput(totalMinor, items).takeIf { it.totalMinor > 0 }
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
    data class Loading(val target: String) : ExchangeRateLookupState
    data class Success(val quote: ExchangeRateQuote) : ExchangeRateLookupState
    data class Error(val target: String) : ExchangeRateLookupState
}

sealed interface AiExtractionState {
    data object Idle : AiExtractionState
    data object MissingApiKey : AiExtractionState
    data class Loading(val imageUri: String) : AiExtractionState
    data class ReceiptSuccess(val imageUri: String, val receipt: ExtractedReceipt) : AiExtractionState
    data class StatementSuccess(
        val imageUri: String,
        val reconciliationId: String,
        val statement: ExtractedStatement,
    ) : AiExtractionState
    data class Error(val imageUri: String, val message: String) : AiExtractionState
}

sealed interface LocalOcrState {
    data object Idle : LocalOcrState
    data class Loading(val imageUri: String) : LocalOcrState
    data class Success(val imageUri: String, val tokens: List<OcrToken>) : LocalOcrState
    data class Error(val imageUri: String, val message: String) : LocalOcrState
}

sealed interface GeminiModelsState {
    data object Idle : GeminiModelsState
    data object Loading : GeminiModelsState
    data object MissingApiKey : GeminiModelsState
    data class Success(val models: List<GeminiModelInfo>) : GeminiModelsState
    data class Error(val message: String) : GeminiModelsState
}

sealed interface TransferState {
    data object Idle : TransferState
    data object Working : TransferState
    data class ExportSuccess(val format: ExportFormat) : TransferState
    data class ImportReady(val preview: ImportPreview) : TransferState
    data class ImportSuccess(val tripCount: Int) : TransferState
    data class Error(val message: String) : TransferState
}
