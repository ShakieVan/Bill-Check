package de.shakie.billcheck.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.shakie.billcheck.BillCheckApplication
import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.NewReceiptItem
import de.shakie.billcheck.data.ReceiptWithItems
import de.shakie.billcheck.data.TripEntity
import de.shakie.billcheck.domain.MoneyCalculator
import de.shakie.billcheck.domain.ExchangeRateQuote
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
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val billCheckApplication = application as BillCheckApplication
    private val repository = billCheckApplication.repository
    private val exchangeRateProvider = billCheckApplication.exchangeRateProvider
    private val selectedTripId = MutableStateFlow<String?>(null)
    private val _exchangeRateLookup = MutableStateFlow<ExchangeRateLookupState>(ExchangeRateLookupState.Idle)
    val exchangeRateLookup: StateFlow<ExchangeRateLookupState> = _exchangeRateLookup.asStateFlow()

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

    val uiState = combine(trips, selectedTrip, receipts) { currentTrips, currentTrip, currentReceipts ->
        MainUiState(
            trips = currentTrips,
            selectedTrip = currentTrip,
            receipts = currentReceipts,
            exactEuroCents = MoneyCalculator.exactTripEuroCents(currentReceipts.map { it.receipt }),
            roundedEuro = MoneyCalculator.roundedUpTripEuro(currentReceipts.map { it.receipt }),
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
            )
        }
        return true
    }

    fun deleteReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch { repository.deleteReceipt(receipt) }
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
