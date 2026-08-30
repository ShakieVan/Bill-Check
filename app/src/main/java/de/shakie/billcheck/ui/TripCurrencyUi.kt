package de.shakie.billcheck.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.shakie.billcheck.R
import de.shakie.billcheck.domain.CurrencyCatalog
import de.shakie.billcheck.domain.CurrencyCatalogEntry
import java.util.Locale

enum class EditableExchangeRateMode { FIXED, DAILY }

data class EditableTripCurrency(
    val code: String,
    val rate: String,
    val mode: EditableExchangeRateMode,
    val isDefault: Boolean,
)

/** State-hoisted editor for all currencies available during a trip. */
@Composable
fun TripCurrencyEditorSection(
    homeCurrencyCode: String,
    currencies: List<EditableTripCurrency>,
    tipCurrencyCode: String?,
    usedCurrencyCodes: Set<String>,
    onRateChange: (currencyCode: String, rate: String) -> Unit,
    onModeChange: (currencyCode: String, mode: EditableExchangeRateMode) -> Unit,
    onSetDefault: (currencyCode: String) -> Unit,
    onDelete: (currencyCode: String) -> Unit,
    onAdd: (CurrencyCatalogEntry) -> Unit,
    modifier: Modifier = Modifier,
    recentCurrencyCodes: List<String> = emptyList(),
    onRefreshRate: ((currencyCode: String) -> Unit)? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.trip_currencies_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.trip_currencies_hint, homeCurrencyCode),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        currencies.forEach { currency ->
            TripCurrencyEditorRow(
                currency = currency,
                homeCurrencyCode = homeCurrencyCode,
                tipCurrencyCode = tipCurrencyCode,
                usedCurrencyCodes = usedCurrencyCodes,
                onRateChange = { onRateChange(currency.code, it) },
                onModeChange = { onModeChange(currency.code, it) },
                onSetDefault = { onSetDefault(currency.code) },
                onDelete = { onDelete(currency.code) },
                onRefreshRate = onRefreshRate?.let { refresh -> { refresh(currency.code) } },
            )
        }
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(stringResource(R.string.trip_currency_add), modifier = Modifier.padding(start = 8.dp))
        }
    }

    if (showPicker) {
        CurrencyPickerDialog(
            selectedCurrencyCode = null,
            preferredCurrencyCodes = listOf(homeCurrencyCode),
            recentCurrencyCodes = recentCurrencyCodes,
            excludedCurrencyCodes = currencies.mapTo(hashSetOf()) { it.code },
            onCurrencySelected = {
                showPicker = false
                onAdd(it)
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun TripCurrencyEditorRow(
    currency: EditableTripCurrency,
    homeCurrencyCode: String,
    tipCurrencyCode: String?,
    usedCurrencyCodes: Set<String>,
    onRateChange: (String) -> Unit,
    onModeChange: (EditableExchangeRateMode) -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
    onRefreshRate: (() -> Unit)?,
) {
    val protection = tripCurrencyRemovalProtection(
        currency,
        homeCurrencyCode,
        tipCurrencyCode,
        usedCurrencyCodes,
    )
    val isHome = protection == TripCurrencyRemovalProtection.HOME
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currency.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isHome) {
                    Text(
                        stringResource(R.string.trip_currency_home_badge),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                if (!isHome && onRefreshRate != null) {
                    IconButton(onClick = onRefreshRate) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_rate))
                    }
                }
                IconButton(onClick = onDelete, enabled = protection == TripCurrencyRemovalProtection.NONE) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.trip_currency_delete, currency.code))
                }
            }
            OutlinedTextField(
                value = if (isHome) "1" else currency.rate,
                onValueChange = onRateChange,
                readOnly = isHome,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.trip_currency_rate_formula, homeCurrencyCode, currency.code)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            if (!isHome) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currency.mode == EditableExchangeRateMode.FIXED,
                        onClick = { onModeChange(EditableExchangeRateMode.FIXED) },
                        label = { Text(stringResource(R.string.trip_currency_rate_fixed)) },
                    )
                    FilterChip(
                        selected = currency.mode == EditableExchangeRateMode.DAILY,
                        onClick = { onModeChange(EditableExchangeRateMode.DAILY) },
                        label = { Text(stringResource(R.string.trip_currency_rate_daily)) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable(enabled = !currency.isDefault, onClick = onSetDefault),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = currency.isDefault, onClick = if (currency.isDefault) null else onSetDefault)
                Text(stringResource(R.string.trip_currency_make_default))
            }
            removalProtectionMessage(protection)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun removalProtectionMessage(protection: TripCurrencyRemovalProtection): String? = when (protection) {
    TripCurrencyRemovalProtection.HOME -> stringResource(R.string.trip_currency_home_protected)
    TripCurrencyRemovalProtection.DEFAULT -> stringResource(R.string.trip_currency_default_protected)
    TripCurrencyRemovalProtection.TIP -> stringResource(R.string.trip_currency_tip_protected)
    TripCurrencyRemovalProtection.USED -> stringResource(R.string.trip_currency_used_protected)
    TripCurrencyRemovalProtection.NONE -> null
}

internal enum class TripCurrencyRemovalProtection { NONE, HOME, DEFAULT, TIP, USED }

internal fun tripCurrencyRemovalProtection(
    currency: EditableTripCurrency,
    homeCurrencyCode: String,
    tipCurrencyCode: String?,
    usedCurrencyCodes: Set<String>,
): TripCurrencyRemovalProtection = when {
    currency.code.equals(homeCurrencyCode, ignoreCase = true) -> TripCurrencyRemovalProtection.HOME
    currency.isDefault -> TripCurrencyRemovalProtection.DEFAULT
    currency.code.equals(tipCurrencyCode, ignoreCase = true) -> TripCurrencyRemovalProtection.TIP
    usedCurrencyCodes.any { it.equals(currency.code, ignoreCase = true) } ->
        TripCurrencyRemovalProtection.USED
    else -> TripCurrencyRemovalProtection.NONE
}

/** Compact trip-only currency selector for the receipt editor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripCurrencySelector(
    selectedCurrencyCode: String,
    currencyCodes: List<String>,
    onCurrencySelected: (String) -> Unit,
    onAddCurrencyRequested: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.receipt_currency),
    showAddCurrencyOption: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val namesByCode = remember(locale) {
        CurrencyCatalog.entries(locale).associate { it.code to it.name }
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = buildCurrencyLabel(
                selectedCurrencyCode,
                namesByCode[selectedCurrencyCode.uppercase(Locale.ROOT)],
            ),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            currencyCodes.distinctBy { it.uppercase(Locale.ROOT) }.forEach { code ->
                DropdownMenuItem(
                    text = { Text(buildCurrencyLabel(code, namesByCode[code.uppercase(Locale.ROOT)])) },
                    onClick = {
                        expanded = false
                        onCurrencySelected(code.uppercase(Locale.ROOT))
                    },
                )
            }
            if (showAddCurrencyOption) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.trip_currency_add_other)) },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onAddCurrencyRequested()
                    },
                )
            }
        }
    }
}

internal fun buildCurrencyLabel(code: String, localizedName: String?): String {
    val normalized = code.trim().uppercase(Locale.ROOT)
    return localizedName?.takeIf(String::isNotBlank)?.let { "$normalized · $it" } ?: normalized
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun TripCurrencyEditorSectionPreview() {
    MaterialTheme {
        TripCurrencyEditorSection(
            homeCurrencyCode = "EUR",
            currencies = listOf(
                EditableTripCurrency("EUR", "1", EditableExchangeRateMode.FIXED, false),
                EditableTripCurrency("EGP", "55.5", EditableExchangeRateMode.DAILY, true),
            ),
            tipCurrencyCode = "EUR",
            usedCurrencyCodes = setOf("EGP"),
            onRateChange = { _, _ -> },
            onModeChange = { _, _ -> },
            onSetDefault = {},
            onDelete = {},
            onAdd = {},
        )
    }
}
