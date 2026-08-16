package de.shakie.billcheck.ui

import android.app.Application
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
    private val selectedTripId = MutableStateFlow<String?>(null)
    private val _exchangeRateLookup = MutableStateFlow<ExchangeRateLookupState>(ExchangeRateLookupState.Idle)
    val exchangeRateLookup: StateFlow<ExchangeRateLookupState> = _exchangeRateLookup.asStateFlow()
    private val _candidateSelection = MutableStateFlow(CandidateSelectionState())
    val candidateSelection: StateFlow<CandidateSelectionState> = _candidateSelection.asStateFlow()

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

    fun selectTrip(id: String) {
        selectedTripId.value = id
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
            selectedTripId.value = trip.id
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
