package de.shakie.billcheck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.shakie.billcheck.R
import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.ReceiptWithItems
import de.shakie.billcheck.data.ReconciliationWithLines
import de.shakie.billcheck.data.StatementLineEntity
import de.shakie.billcheck.domain.ReconciliationStatus
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Currency
import java.util.Locale

@Composable
fun ReconciliationManagerDialog(
    initialSelectedId: String? = null,
    reconciliations: List<ReconciliationWithLines>,
    receipts: List<ReceiptWithItems>,
    defaultCurrencyCode: String,
    candidateSelection: CandidateSelectionState,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onUpdateHeader: (String, String, String?) -> Unit,
    onAddLine: (String, String, String, String, String, Long?) -> Boolean,
    onUpdateLine: (StatementLineEntity, String, String, String, String, Long?) -> Boolean,
    onDeleteLine: (StatementLineEntity) -> Unit,
    onAcceptLine: (StatementLineEntity, Boolean) -> Unit,
    onLoadCandidates: (StatementLineEntity) -> Unit,
    onClearCandidates: () -> Unit,
    onAssignReceipt: (StatementLineEntity, ReceiptEntity) -> Unit,
    onClearLineMatch: (StatementLineEntity) -> Unit,
    onRun: (ReconciliationWithLines) -> Unit,
    onReset: (ReconciliationWithLines) -> Unit,
    onDelete: (String) -> Unit,
    onOpenImage: (ReconciliationWithLines) -> Unit,
    onChooseImage: (ReconciliationWithLines) -> Unit,
) {
    var selectedId by remember(initialSelectedId) { mutableStateOf(initialSelectedId) }
    var showCreate by remember { mutableStateOf(false) }
    var editingLine by remember { mutableStateOf<StatementLineEntity?>(null) }
    var addingLine by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ReconciliationWithLines?>(null) }
    val selected = reconciliations.firstOrNull { it.reconciliation.id == selectedId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().imePadding(),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (selected == null) {
                ReconciliationOverview(
                    reconciliations = reconciliations,
                    onDismiss = onDismiss,
                    onCreate = { showCreate = true },
                    onSelect = { selectedId = it.reconciliation.id },
                )
            } else {
                ReconciliationDetails(
                    reconciliation = selected,
                    receipts = receipts,
                    onBack = { selectedId = null },
                    onAddLine = { addingLine = true },
                    onEditLine = { editingLine = it },
                    onDeleteLine = onDeleteLine,
                    onAcceptLine = onAcceptLine,
                    onLoadCandidates = onLoadCandidates,
                    onClearLineMatch = onClearLineMatch,
                    onRun = { onRun(selected) },
                    onReset = { onReset(selected) },
                    onDelete = { deleteTarget = selected },
                    onOpenImage = { onOpenImage(selected) },
                    onChooseImage = { onChooseImage(selected) },
                    onRemoveImage = {
                        onUpdateHeader(
                            selected.reconciliation.id,
                            selected.reconciliation.title,
                            null,
                        )
                    },
                )
            }
        }
    }

    if (showCreate) {
        TitleEditorDialog(
            onDismiss = { showCreate = false },
            onSave = {
                onCreate(it)
                showCreate = false
            },
        )
    }
    if (addingLine && selected != null) {
        StatementLineEditorDialog(
            currencyCode = defaultCurrencyCode,
            onDismiss = { addingLine = false },
            onSave = { description, check, amount, currency, date ->
                onAddLine(selected.reconciliation.id, description, check, amount, currency, date)
                    .also { if (it) addingLine = false }
            },
        )
    }
    editingLine?.let { line ->
        StatementLineEditorDialog(
            existing = line,
            currencyCode = defaultCurrencyCode,
            onDismiss = { editingLine = null },
            onSave = { description, check, amount, currency, date ->
                onUpdateLine(line, description, check, amount, currency, date)
                    .also { if (it) editingLine = null }
            },
        )
    }
    val candidateLine = selected?.lines?.firstOrNull {
        it.line.id == candidateSelection.lineId
    }?.line
    if (candidateLine != null) {
        CandidateDialog(
            state = candidateSelection,
            onDismiss = onClearCandidates,
            onAssign = { onAssignReceipt(candidateLine, it) },
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_reconciliation)) },
            text = {
                Text(stringResource(R.string.delete_reconciliation_confirm, target.reconciliation.title))
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(target.reconciliation.id)
                        deleteTarget = null
                        selectedId = null
                    },
                ) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
        )
    }
}

@Composable
private fun ReconciliationOverview(
    reconciliations: List<ReconciliationWithLines>,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onSelect: (ReconciliationWithLines) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        DialogHeader(stringResource(R.string.reconciliations), onDismiss)
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.new_reconciliation))
                }
            }
            if (reconciliations.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_reconciliations),
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(reconciliations, key = { it.reconciliation.id }) { reconciliation ->
                    Card(onClick = { onSelect(reconciliation) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(reconciliation.reconciliation.title, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${reconciliation.lines.size} ${stringResource(R.string.statement_lines)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            StatusDots(reconciliation)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconciliationDetails(
    reconciliation: ReconciliationWithLines,
    receipts: List<ReceiptWithItems>,
    onBack: () -> Unit,
    onAddLine: () -> Unit,
    onEditLine: (StatementLineEntity) -> Unit,
    onDeleteLine: (StatementLineEntity) -> Unit,
    onAcceptLine: (StatementLineEntity, Boolean) -> Unit,
    onLoadCandidates: (StatementLineEntity) -> Unit,
    onClearLineMatch: (StatementLineEntity) -> Unit,
    onRun: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onOpenImage: () -> Unit,
    onChooseImage: () -> Unit,
    onRemoveImage: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_to_reconciliations))
            }
            Text(
                reconciliation.reconciliation.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider()
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatementImageActions(
                    hasImage = reconciliation.reconciliation.statementImageUri != null,
                    onOpen = onOpenImage,
                    onChoose = onChooseImage,
                    onRemove = onRemoveImage,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRun, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.run_reconciliation))
                    }
                    FilledTonalButton(onClick = onReset) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_reconciliation))
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.statement_lines),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onAddLine) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(stringResource(R.string.add_statement_line))
                    }
                }
            }
            if (reconciliation.lines.isEmpty()) {
                item { Text(stringResource(R.string.no_statement_lines), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(reconciliation.lines, key = { it.line.id }) { related ->
                    val match = related.matches.singleOrNull()
                    val matchedReceipt = receipts.firstOrNull { it.receipt.id == match?.receiptId }?.receipt
                    StatementLineCard(
                        line = related.line,
                        matchedReceipt = matchedReceipt,
                        onEdit = { onEditLine(related.line) },
                        onDelete = { onDeleteLine(related.line) },
                        onAccept = { onAcceptLine(related.line, it) },
                        onAssign = { onLoadCandidates(related.line) },
                        onClearMatch = { onClearLineMatch(related.line) },
                    )
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_reconciliation), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun StatementLineCard(
    line: StatementLineEntity,
    matchedReceipt: ReceiptEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAccept: (Boolean) -> Unit,
    onAssign: () -> Unit,
    onClearMatch: () -> Unit,
) {
    val status = statusPresentation(line.status)
    Card(
        colors = CardDefaults.cardColors(containerColor = status.color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(status.color, RoundedCornerShape(50)))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(status.label), fontWeight = FontWeight.SemiBold, color = status.color)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_statement_line)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) }
            }
            Text(line.description.ifBlank { "#${line.checkNumber}" }, style = MaterialTheme.typography.titleMedium)
            Text(
                buildString {
                    if (line.checkNumber.isNotBlank()) append("#${line.checkNumber} · ")
                    append(formatMinor(line.amountMinor, line.currencyCode))
                    line.occurredOn?.let { append(" · ${formatDate(it)}") }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            matchedReceipt?.let {
                Spacer(Modifier.height(8.dp))
                Text("↳ ${it.location} · #${it.checkNumber} · ${formatMinor(it.foreignAmountMinor, it.foreignCurrencyCode)}")
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = line.acceptedWithoutReceipt,
                    onCheckedChange = onAccept,
                    enabled = matchedReceipt == null,
                )
                Text(
                    stringResource(R.string.accept_without_receipt),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f).clickable(
                        enabled = matchedReceipt == null,
                        onClick = { onAccept(!line.acceptedWithoutReceipt) },
                    ),
                )
            }
            if (!line.acceptedWithoutReceipt) {
                OutlinedButton(onClick = onAssign, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(if (matchedReceipt == null) R.string.assign_receipt else R.string.change_assignment))
                }
                if (matchedReceipt != null) {
                    TextButton(onClick = onClearMatch, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.LinkOff, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.remove_assignment))
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateDialog(
    state: CandidateSelectionState,
    onDismiss: () -> Unit,
    onAssign: (ReceiptEntity) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f)) {
            Column {
                DialogHeader(stringResource(R.string.candidate_receipts), onDismiss)
                if (state.loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (state.candidates.isEmpty()) {
                    Text(
                        stringResource(R.string.no_candidate_receipts),
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.candidates, key = { it.receipt.id }) { candidate ->
                            Card(onClick = { onAssign(candidate.receipt) }) {
                                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                    Text(candidate.receipt.location, fontWeight = FontWeight.SemiBold)
                                    Text("#${candidate.receipt.checkNumber} · ${formatMinor(candidate.receipt.foreignAmountMinor, candidate.receipt.foreignCurrencyCode)}")
                                    Text(
                                        "${formatDate(candidate.receipt.occurredAt)} · ${stringResource(R.string.candidate_score, candidate.score)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
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
private fun TitleEditorDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_reconciliation)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.reconciliation_title)) },
                placeholder = { Text(stringResource(R.string.reconciliation_title_example)) },
                singleLine = true,
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        confirmButton = { TextButton(onClick = { onSave(title) }) { Text(stringResource(R.string.save)) } },
    )
}

@Composable
private fun StatementLineEditorDialog(
    existing: StatementLineEntity? = null,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Long?) -> Boolean,
) {
    var description by remember(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var check by remember(existing?.id) { mutableStateOf(existing?.checkNumber.orEmpty()) }
    var amount by remember(existing?.id) { mutableStateOf(existing?.amountMinor?.let(::formatInputMinor).orEmpty()) }
    var currency by remember(existing?.id) { mutableStateOf(existing?.currencyCode ?: currencyCode) }
    var date by remember(existing?.id) { mutableStateOf(existing?.occurredOn?.let(::formatDate).orEmpty()) }
    var invalid by remember(existing?.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.add_statement_line else R.string.edit_statement_line)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.statement_description)) }) }
                item { OutlinedTextField(check, { check = it }, label = { Text(stringResource(R.string.check_number)) }) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            amount,
                            { amount = it; invalid = false },
                            label = { Text(stringResource(R.string.amount_in_currency, currency)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = invalid,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(currency, { currency = it.uppercase().take(3) }, label = { Text(stringResource(R.string.foreign_currency)) }, modifier = Modifier.width(92.dp))
                    }
                }
                item {
                    OutlinedTextField(
                        date,
                        { date = it; invalid = false },
                        label = { Text(stringResource(R.string.statement_date)) },
                        isError = invalid,
                        supportingText = if (invalid) ({ Text(stringResource(R.string.invalid_date)) }) else null,
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            TextButton(onClick = {
                val parsedDate = parseDate(date)
                if (date.isNotBlank() && parsedDate == null) {
                    invalid = true
                } else if (!onSave(description, check, amount, currency, parsedDate)) {
                    invalid = true
                }
            }) { Text(stringResource(R.string.save)) }
        },
    )
}

@Composable
private fun StatementImageActions(
    hasImage: Boolean,
    onOpen: () -> Unit,
    onChoose: () -> Unit,
    onRemove: () -> Unit,
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.statement_image), fontWeight = FontWeight.SemiBold)
            if (hasImage) {
                FilledTonalButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.review_image_title))
                }
                TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.remove_statement_image), color = MaterialTheme.colorScheme.error)
                }
            } else {
                OutlinedButton(onClick = onChoose, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_statement_image))
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(title: String, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
    }
    HorizontalDivider()
}

@Composable
private fun StatusDots(reconciliation: ReconciliationWithLines) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        reconciliation.lines.groupBy { it.line.status }.forEach { (status, lines) ->
            Box(
                modifier = Modifier
                    .size((10 + lines.size.coerceAtMost(5) * 2).dp)
                    .background(statusPresentation(status).color, RoundedCornerShape(50)),
            )
        }
    }
}

private data class StatusPresentation(val label: Int, val color: Color)

@Composable
private fun statusPresentation(status: String): StatusPresentation = when (status) {
    ReconciliationStatus.CORRECT -> StatusPresentation(R.string.status_correct, Color(0xFF2E7D32))
    ReconciliationStatus.UNCERTAIN -> StatusPresentation(R.string.status_uncertain, Color(0xFFF9A825))
    ReconciliationStatus.AMOUNT_MISMATCH -> StatusPresentation(R.string.status_amount_mismatch, Color(0xFFEF6C00))
    ReconciliationStatus.ACCEPTED -> StatusPresentation(R.string.status_accepted, Color(0xFF2E7D32))
    else -> StatusPresentation(R.string.status_not_found, MaterialTheme.colorScheme.error)
}

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")

private fun parseDate(value: String): Long? = try {
    value.trim().takeIf { it.isNotEmpty() }?.let {
        LocalDate.parse(it, dateFormatter).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
} catch (_: DateTimeParseException) {
    null
}

private fun formatDate(value: Long): String =
    Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

private fun formatInputMinor(value: Long): String =
    String.format(Locale.GERMANY, "%.2f", value / 100.0)

private fun formatMinor(value: Long, currencyCode: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(currencyCode) }.format(value / 100.0)
}.getOrElse { "${formatInputMinor(value)} $currencyCode" }
