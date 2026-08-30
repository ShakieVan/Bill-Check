package de.shakie.billcheck.ui

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.shakie.billcheck.R
import de.shakie.billcheck.domain.CurrencyCatalog
import de.shakie.billcheck.domain.CurrencyCatalogEntry
import java.util.Locale

/**
 * Reusable offline ISO-currency picker. [preferredCurrencyCodes] is intended for currencies from
 * the current trip; [recentCurrencyCodes] can contain app-wide history in most-recent-first order.
 */
@Composable
fun CurrencyPickerDialog(
    selectedCurrencyCode: String?,
    onCurrencySelected: (CurrencyCatalogEntry) -> Unit,
    onDismiss: () -> Unit,
    preferredCurrencyCodes: List<String> = emptyList(),
    recentCurrencyCodes: List<String> = emptyList(),
    excludedCurrencyCodes: Set<String> = emptySet(),
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val catalog = remember(locale) { CurrencyCatalog.entries(locale) }
    val priorityCodes = remember(selectedCurrencyCode, preferredCurrencyCodes, recentCurrencyCodes) {
        buildList {
            selectedCurrencyCode?.let(::add)
            addAll(preferredCurrencyCodes)
            addAll(recentCurrencyCodes)
        }.map { it.trim().uppercase(Locale.ROOT) }.distinct()
    }
    val prioritySet = remember(priorityCodes) { priorityCodes.toSet() }
    var query by remember { mutableStateOf("") }
    val normalizedExcluded = remember(excludedCurrencyCodes) {
        excludedCurrencyCodes.mapTo(hashSetOf()) { it.trim().uppercase(Locale.ROOT) }
    }
    val results = remember(catalog, query, priorityCodes, normalizedExcluded) {
        CurrencyCatalog.search(catalog, query, priorityCodes).filterNot { it.code in normalizedExcluded }
    }
    val quickResults = remember(results, prioritySet) { results.filter { it.code in prioritySet } }
    val otherResults = remember(results, prioritySet) { results.filterNot { it.code in prioritySet } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .widthIn(max = 760.dp)
                .imePadding(),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val expanded = maxWidth >= 600.dp
                Column(Modifier.fillMaxSize()) {
                    CurrencyPickerHeader(onDismiss)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        singleLine = true,
                        label = { Text(stringResource(R.string.currency_search_label)) },
                        placeholder = { Text(stringResource(R.string.currency_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.currency_search_clear),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )

                    if (results.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                stringResource(R.string.currency_search_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            if (expanded) item { CurrencyTableHeader() }
                            if (quickResults.isNotEmpty()) {
                                item { CurrencySectionHeader(stringResource(R.string.currency_quick_access)) }
                                items(quickResults, key = CurrencyCatalogEntry::code) { entry ->
                                    CurrencyRow(entry, selectedCurrencyCode, expanded, onCurrencySelected)
                                }
                            }
                            if (otherResults.isNotEmpty()) {
                                if (quickResults.isNotEmpty()) {
                                    item { CurrencySectionHeader(stringResource(R.string.currency_all)) }
                                }
                                items(otherResults, key = CurrencyCatalogEntry::code) { entry ->
                                    CurrencyRow(entry, selectedCurrencyCode, expanded, onCurrencySelected)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyPickerHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 12.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.currency_picker_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
        }
    }
}

@Composable
private fun CurrencyTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.currency_code), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(84.dp))
        Text(stringResource(R.string.currency_name), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.8f))
        Text(stringResource(R.string.currency_region), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1.2f))
        androidx.compose.foundation.layout.Spacer(Modifier.size(40.dp))
    }
    HorizontalDivider()
}

@Composable
private fun CurrencySectionHeader(title: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(
            title,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CurrencyRow(
    entry: CurrencyCatalogEntry,
    selectedCurrencyCode: String?,
    expanded: Boolean,
    onCurrencySelected: (CurrencyCatalogEntry) -> Unit,
) {
    val selected = entry.code.equals(selectedCurrencyCode, ignoreCase = true)
    val regions = currencyRegionSummary(entry)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = { onCurrencySelected(entry) },
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (expanded) {
            Text(entry.code, fontWeight = FontWeight.Bold, modifier = Modifier.width(84.dp))
            Text(entry.name, modifier = Modifier.weight(0.8f).padding(end = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                regions,
                modifier = Modifier.weight(1.2f).padding(end = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.code, fontWeight = FontWeight.Bold)
                    Text(entry.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    regions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = stringResource(R.string.currency_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        } else {
            androidx.compose.foundation.layout.Spacer(Modifier.size(24.dp))
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 20.dp))
}

@Composable
private fun currencyRegionSummary(entry: CurrencyCatalogEntry): String = when {
    entry.code == "EUR" && entry.regions.size > 1 -> stringResource(
        R.string.currency_euro_region,
        entry.regions.size,
    )
    entry.regions.size > MANY_REGIONS_THRESHOLD -> stringResource(
        R.string.currency_many_regions,
        entry.regions.size,
    )
    entry.regions.size <= MAX_VISIBLE_REGIONS -> entry.regions.joinToString(", ")
    else -> stringResource(
        R.string.currency_regions_more,
        entry.regions.take(MAX_VISIBLE_REGIONS).joinToString(", "),
        entry.regions.size - MAX_VISIBLE_REGIONS,
    )
}

private const val MAX_VISIBLE_REGIONS = 2
private const val MANY_REGIONS_THRESHOLD = 5
