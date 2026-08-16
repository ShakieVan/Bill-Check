package de.shakie.billcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.ReceiptWithItems
import de.shakie.billcheck.data.TripEntity
import de.shakie.billcheck.domain.MoneyCalculator
import de.shakie.billcheck.ui.MainUiState
import de.shakie.billcheck.ui.MainViewModel
import de.shakie.billcheck.ui.ReceiptItemDraft
import de.shakie.billcheck.ui.ExchangeRateLookupState
import de.shakie.billcheck.ui.theme.BillCheckTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BillCheckTheme {
                BillCheckApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillCheckApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exchangeRateLookup by viewModel.exchangeRateLookup.collectAsStateWithLifecycle()
    var showCreateTrip by remember { mutableStateOf(false) }
    var showManualReceipt by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.not_yet_available)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Bill Check", fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { padding ->
        if (state.trips.isEmpty()) {
            EmptyTrips(
                modifier = Modifier.padding(padding),
                onCreate = { showCreateTrip = true },
            )
        } else {
            Dashboard(
                state = state,
                modifier = Modifier.padding(padding),
                onSelectTrip = viewModel::selectTrip,
                onCreateTrip = { showCreateTrip = true },
                onManualReceipt = { showManualReceipt = true },
                onCamera = { scope.launch { snackbar.showSnackbar(comingSoon) } },
                onGallery = { scope.launch { snackbar.showSnackbar(comingSoon) } },
                onDeleteReceipt = viewModel::deleteReceipt,
            )
        }
    }

    if (showCreateTrip) {
        CreateTripDialog(
            suggestedName = stringResource(R.string.trip_default_name),
            exchangeRateLookup = exchangeRateLookup,
            onLookupRate = viewModel::requestExchangeRate,
            onDismiss = {
                showCreateTrip = false
                viewModel.clearExchangeRateLookup()
            },
            onSave = { name, currency, rate, useDailyRate, tipMinor, tipCurrency ->
                viewModel.createTrip(name, currency, rate, useDailyRate, tipMinor, tipCurrency)
                showCreateTrip = false
                viewModel.clearExchangeRateLookup()
            },
        )
    }

    state.selectedTrip?.let { trip ->
        if (showManualReceipt) {
            ManualReceiptDialog(
                trip = trip,
                onDismiss = { showManualReceipt = false },
                onSave = { location, check, amount, tip, itemDrafts ->
                    viewModel.addReceipt(location, check, amount, tip, itemDrafts).also { saved ->
                        if (saved) showManualReceipt = false
                    }
                },
            )
        }
    }
}

@Composable
private fun EmptyTrips(modifier: Modifier, onCreate: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.first_trip_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.first_trip_text),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreate) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.create_trip))
        }
    }
}

@Composable
private fun Dashboard(
    state: MainUiState,
    modifier: Modifier,
    onSelectTrip: (String) -> Unit,
    onCreateTrip: () -> Unit,
    onManualReceipt: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDeleteReceipt: (ReceiptEntity) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TripSelector(
                trips = state.trips,
                selected = state.selectedTrip,
                onSelect = onSelectTrip,
                onCreate = onCreateTrip,
            )
        }
        item {
            Summary(state)
        }
        item {
            ReceiptActions(onCamera, onGallery, onManualReceipt)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.receipt_list),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    state.receipts.size.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.receipts.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(
                        stringResource(R.string.no_receipts),
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(state.receipts, key = { it.receipt.id }) { receipt ->
                ReceiptCard(receipt, onDeleteReceipt)
            }
        }
    }
}

@Composable
private fun TripSelector(
    trips: List<TripEntity>,
    selected: TripEntity?,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.name.orEmpty(), modifier = Modifier.weight(1f))
            Icon(Icons.Default.ExpandMore, contentDescription = stringResource(R.string.select_trip))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            trips.forEach { trip ->
                DropdownMenuItem(
                    text = { Text(trip.name) },
                    onClick = {
                        onSelect(trip.id)
                        expanded = false
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_trip)) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCreate()
                },
            )
        }
    }
}

@Composable
private fun Summary(state: MainUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.rounded_total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "${state.roundedEuro} €",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                stringResource(R.string.rounded_total_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallSummary(
                    label = stringResource(R.string.exact_total),
                    value = formatEuroCents(state.exactEuroCents),
                    modifier = Modifier.weight(1f),
                )
                SmallSummary(
                    label = stringResource(R.string.receipts_count),
                    value = state.receipts.size.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SmallSummary(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReceiptActions(onCamera: () -> Unit, onGallery: () -> Unit, onManual: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onCamera, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.take_photo))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onGallery, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.choose_image))
            }
            OutlinedButton(onClick = onManual, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.manual_entry))
            }
        }
    }
}

@Composable
private fun ReceiptCard(
    receiptWithItems: ReceiptWithItems,
    onDelete: (ReceiptEntity) -> Unit,
) {
    val receipt = receiptWithItems.receipt
    val exactCents = MoneyCalculator.exactEuroCents(receipt)
    val rounded = MoneyCalculator.roundedUpEuro(exactCents)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    receipt.location.ifBlank { stringResource(R.string.add_receipt) },
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    buildString {
                        append(formatDate(receipt.occurredAt))
                        if (receipt.checkNumber.isNotBlank()) append("  ·  #${receipt.checkNumber}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    formatMinor(receipt.foreignAmountMinor, receipt.foreignCurrencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(
                        R.string.receipt_exchange_rate,
                        receipt.exchangeRate,
                        receipt.foreignCurrencyCode,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (receiptWithItems.items.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    receiptWithItems.items
                        .sortedBy { it.sortPosition }
                        .take(3)
                        .forEach { item ->
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    item.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    formatMinor(item.amountMinor, item.currencyCode),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    if (receiptWithItems.items.size > 3) {
                        Text(
                            stringResource(
                                R.string.more_receipt_items,
                                receiptWithItems.items.size - 3,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$rounded €", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    formatEuroCents(exactCents),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onDelete(receipt) }) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete_receipt),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CreateTripDialog(
    suggestedName: String,
    exchangeRateLookup: ExchangeRateLookupState,
    onLookupRate: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, Long, String) -> Unit,
) {
    var name by remember { mutableStateOf(suggestedName) }
    var currency by remember { mutableStateOf("EGP") }
    var rate by remember { mutableStateOf("55,5") }
    var tip by remember { mutableStateOf("1,00") }
    var tipCurrency by remember { mutableStateOf("EUR") }
    var rateWasEdited by remember { mutableStateOf(false) }
    var useDailyRate by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(currency) {
        if (currency.length == 3) {
            rateWasEdited = false
            delay(500)
            onLookupRate(currency)
        }
    }
    LaunchedEffect(exchangeRateLookup) {
        val success = exchangeRateLookup as? ExchangeRateLookupState.Success
        if (success?.quote?.targetCurrencyCode == currency && !rateWasEdited) {
            rate = success.quote.foreignPerEuro
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_trip)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.trip_name)) })
                OutlinedTextField(currency, { currency = it.uppercase().take(3) }, label = { Text(stringResource(R.string.foreign_currency)) })
                OutlinedTextField(
                    rate,
                    {
                        rate = it
                        rateWasEdited = true
                    },
                    label = { Text(stringResource(R.string.exchange_rate)) },
                    placeholder = { Text(stringResource(R.string.rate_example)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = {
                        if (exchangeRateLookup is ExchangeRateLookupState.Loading &&
                            exchangeRateLookup.target == currency
                        ) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { onLookupRate(currency) }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.refresh_rate),
                                )
                            }
                        }
                    },
                    supportingText = {
                        when (exchangeRateLookup) {
                            is ExchangeRateLookupState.Success -> if (
                                exchangeRateLookup.quote.targetCurrencyCode == currency
                            ) {
                                Text(
                                    stringResource(
                                        if (exchangeRateLookup.quote.fromCache) {
                                            R.string.exchange_rate_cached
                                        } else {
                                            R.string.exchange_rate_updated
                                        },
                                        formatRateDate(exchangeRateLookup.quote.updatedAt),
                                    ),
                                )
                            }
                            is ExchangeRateLookupState.Error -> if (
                                exchangeRateLookup.target == currency
                            ) {
                                Text(
                                    stringResource(R.string.rate_lookup_failed),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            else -> Unit
                        }
                    },
                )
                TextButton(
                    onClick = { uriHandler.openUri("https://www.exchangerate-api.com") },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        stringResource(R.string.rate_attribution),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.daily_exchange_rate))
                        Text(
                            stringResource(R.string.daily_exchange_rate_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = useDailyRate, onCheckedChange = { useDailyRate = it })
                }
                OutlinedTextField(
                    tip,
                    { tip = it },
                    label = { Text(stringResource(R.string.default_tip)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(tipCurrency, { tipCurrency = it.uppercase().take(3) }, label = { Text(stringResource(R.string.tip_currency)) })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name,
                        currency.ifBlank { "EGP" },
                        rate,
                        useDailyRate,
                        MainViewModel.parseMinor(tip) ?: 100,
                        tipCurrency.ifBlank { "EUR" },
                    )
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ManualReceiptDialog(
    trip: TripEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, List<ReceiptItemDraft>) -> Boolean,
) {
    var location by remember { mutableStateOf("") }
    var check by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var addTip by remember { mutableStateOf(trip.defaultTipSelected) }
    var invalid by remember { mutableStateOf(false) }
    var nextItemId by remember { mutableStateOf(1L) }
    var items by remember { mutableStateOf(listOf(EditableReceiptItem(id = 0))) }
    val itemSumMinor = items.mapNotNull { MainViewModel.parseMinor(it.amountText) }.sum()
    val receiptMinor = MainViewModel.parseMinor(amount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_receipt)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(location, { location = it }, label = { Text(stringResource(R.string.location)) })
                OutlinedTextField(check, { check = it }, label = { Text(stringResource(R.string.check_number)) })
                OutlinedTextField(
                    amount,
                    {
                        amount = it
                        invalid = false
                    },
                    label = { Text(stringResource(R.string.amount_in_currency, trip.foreignCurrencyCode)) },
                    placeholder = { Text(stringResource(R.string.amount_example)) },
                    isError = invalid,
                    supportingText = if (invalid) {
                        { Text(stringResource(R.string.invalid_amount)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text(
                    stringResource(R.string.receipt_items),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                items.forEachIndexed { index, item ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.item_number, index + 1),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { items = items.filterNot { it.id == item.id } },
                                ) {
                                    Icon(
                                        Icons.Default.RemoveCircleOutline,
                                        contentDescription = stringResource(R.string.remove_item),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { value ->
                                    items = items.toMutableList().also {
                                        it[index] = item.copy(name = value)
                                    }
                                },
                                label = { Text(stringResource(R.string.item_name)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = item.amountText,
                                onValueChange = { value ->
                                    items = items.toMutableList().also {
                                        it[index] = item.copy(amountText = value)
                                    }
                                    invalid = false
                                },
                                label = {
                                    Text(
                                        stringResource(
                                            R.string.item_amount,
                                            trip.foreignCurrencyCode,
                                        ),
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        items = items + EditableReceiptItem(id = nextItemId++)
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_item))
                }
                if (itemSumMinor > 0) {
                    val difference = receiptMinor?.minus(itemSumMinor)
                    Text(
                        text = when {
                            receiptMinor == null -> stringResource(
                                R.string.item_sum_used_as_total,
                                formatMinor(itemSumMinor, trip.foreignCurrencyCode),
                            )
                            difference == 0L -> stringResource(
                                R.string.item_sum_matches,
                                formatMinor(itemSumMinor, trip.foreignCurrencyCode),
                            )
                            else -> stringResource(
                                R.string.item_sum_differs,
                                formatMinor(itemSumMinor, trip.foreignCurrencyCode),
                                formatMinor(
                                    kotlin.math.abs(requireNotNull(difference)),
                                    trip.foreignCurrencyCode,
                                ),
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (difference == null || difference == 0L) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = addTip, onCheckedChange = { addTip = it })
                    Column {
                        Text(stringResource(R.string.add_default_tip))
                        Text(
                            formatMinor(trip.defaultTipMinor, trip.defaultTipCurrencyCode),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                invalid = !onSave(
                    location,
                    check,
                    amount,
                    addTip,
                    items.map { ReceiptItemDraft(it.name, it.amountText) },
                )
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private data class EditableReceiptItem(
    val id: Long,
    val name: String = "",
    val amountText: String = "",
)

private fun formatEuroCents(cents: Long): String = formatMinor(cents, "EUR")

private fun formatMinor(minor: Long, currencyCode: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance(currencyCode)
    }.format(BigDecimal.valueOf(minor, 2))
}.getOrElse { "${BigDecimal.valueOf(minor, 2)} $currencyCode" }

private fun formatDate(epochMillis: Long): String = DateTimeFormatter
    .ofPattern("dd.MM., HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(epochMillis))

private fun formatRateDate(epochMillis: Long): String = DateTimeFormatter
    .ofPattern("dd.MM.yyyy, HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(epochMillis))
