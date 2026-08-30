package de.shakie.billcheck.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
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
import de.shakie.billcheck.data.StatementLineWithMatches
import de.shakie.billcheck.domain.ReconciliationStatus
import de.shakie.billcheck.domain.CurrencyAmount
import de.shakie.billcheck.domain.ReconciliationAuditor
import de.shakie.billcheck.domain.ReconciliationCoverage
import de.shakie.billcheck.domain.ReconciliationMatcher
import de.shakie.billcheck.domain.ReconciliationNarrativeFacts
import de.shakie.billcheck.domain.ReconciliationNarrativeIssueKind
import de.shakie.billcheck.domain.ReconciliationNarrator
import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Currency
import java.util.Locale

@Composable
fun ReconciliationManagerDialog(
    initialSelectedId: String? = null,
    reconciliations: List<ReconciliationWithLines>,
    receipts: List<ReceiptWithItems>,
    defaultCurrencyCode: String,
    currencyCodes: List<String>,
    candidateSelection: CandidateSelectionState,
    analysisState: ReconciliationAnalysisState,
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
    onAnalyzeImage: (ReconciliationWithLines) -> Unit,
    homeCurrencyCode: String = defaultCurrencyCode,
    recentCurrencyCodes: List<String> = emptyList(),
    exchangeRateLookup: ExchangeRateLookupState = ExchangeRateLookupState.Idle,
    onLookupRate: (String, String) -> Unit = { _, _ -> },
    onAddTripCurrency: (String, String, Boolean) -> Boolean = { _, _, _ -> false },
) {
    var selectedId by remember(initialSelectedId) { mutableStateOf(initialSelectedId) }
    var showCreate by remember { mutableStateOf(false) }
    var editingTitle by remember { mutableStateOf(false) }
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
                    analysisState = analysisState,
                    onBack = { selectedId = null },
                    onEditTitle = { editingTitle = true },
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
                    onAnalyzeImage = { onAnalyzeImage(selected) },
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
            dialogTitle = R.string.new_reconciliation,
            onDismiss = { showCreate = false },
            onSave = {
                onCreate(it)
                showCreate = false
            },
        )
    }
    if (editingTitle && selected != null) {
        TitleEditorDialog(
            initialTitle = selected.reconciliation.title,
            dialogTitle = R.string.edit_reconciliation_title,
            onDismiss = { editingTitle = false },
            onSave = { title ->
                onUpdateHeader(
                    selected.reconciliation.id,
                    title,
                    selected.reconciliation.statementImageUri,
                )
                editingTitle = false
            },
        )
    }
    if (addingLine && selected != null) {
        StatementLineEditorDialog(
            currencyCode = defaultCurrencyCode,
            currencyCodes = currencyCodes,
            homeCurrencyCode = homeCurrencyCode,
            recentCurrencyCodes = recentCurrencyCodes,
            exchangeRateLookup = exchangeRateLookup,
            onLookupRate = onLookupRate,
            onAddTripCurrency = onAddTripCurrency,
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
            currencyCodes = currencyCodes,
            homeCurrencyCode = homeCurrencyCode,
            recentCurrencyCodes = recentCurrencyCodes,
            exchangeRateLookup = exchangeRateLookup,
            onLookupRate = onLookupRate,
            onAddTripCurrency = onAddTripCurrency,
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
                                Text(
                                    stringResource(R.string.reconciliation_title),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
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
    analysisState: ReconciliationAnalysisState,
    onBack: () -> Unit,
    onEditTitle: () -> Unit,
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
    onAnalyzeImage: () -> Unit,
    onRemoveImage: () -> Unit,
) {
    val receiptById = receipts.associateBy { it.receipt.id }
    val currentMatchedReceiptIds = reconciliation.lines
        .flatMap { it.matches }
        .mapTo(mutableSetOf()) { it.receiptId }
    val receiptOnly = receipts.filterNot { it.receipt.id in currentMatchedReceiptIds }
    val receiptsForStatement = receipts.map { it.receipt }
    val timeline = buildList<ReconciliationTimelineEntry> {
        reconciliation.lines.forEach { related ->
            val matchedReceipt = related.matches.singleOrNull()?.receiptId?.let(receiptById::get)?.receipt
            add(ReconciliationTimelineEntry.Statement(related, matchedReceipt))
        }
        receiptOnly.forEach { add(ReconciliationTimelineEntry.ReceiptOnly(it.receipt)) }
    }.sortedWith(
        compareBy<ReconciliationTimelineEntry> { it.occurredAt ?: Long.MAX_VALUE }
            .thenBy { it.key },
    )
    val running = analysisState is ReconciliationAnalysisState.Running &&
        analysisState.reconciliationId == reconciliation.reconciliation.id
    val nextStep = reconciliationNextStep(
        hasImage = reconciliation.reconciliation.statementImageUri != null,
        statementLineCount = reconciliation.lines.size,
        analysisUpdatedAt = reconciliation.reconciliation.analysisUpdatedAt,
    )
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_to_reconciliations))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.reconciliation_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    reconciliation.reconciliation.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            IconButton(onClick = onEditTitle) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_reconciliation_title))
            }
        }
        HorizontalDivider()
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatementImageActions(
                    hasImage = reconciliation.reconciliation.statementImageUri != null,
                    emphasizeAnalyze = nextStep == ReconciliationNextStep.ANALYZE_IMAGE,
                    onOpen = onOpenImage,
                    onChoose = onChooseImage,
                    onRemove = onRemoveImage,
                    onAnalyze = onAnalyzeImage,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRun,
                        enabled = !running,
                        modifier = Modifier
                            .weight(1f)
                            .nextStepPulse(
                                nextStep == ReconciliationNextStep.RUN_RECONCILIATION && !running,
                            ),
                    ) {
                        if (running) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.run_reconciliation))
                    }
                    FilledTonalButton(onClick = onReset) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_reconciliation))
                    }
                }
            }
            item {
                ReconciliationSummaryCard(
                    reconciliation = reconciliation,
                    receiptsForStatement = receiptsForStatement,
                    analysisState = analysisState,
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.chronological_reconciliation),
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
            if (timeline.isEmpty()) {
                item { Text(stringResource(R.string.no_reconciliation_entries), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(timeline, key = { it.key }) { entry ->
                    when (entry) {
                        is ReconciliationTimelineEntry.Statement -> {
                            val line = entry.related.line
                            StatementLineCard(
                                line = line,
                                matchedReceipt = entry.matchedReceipt,
                                suggestedReceipt = line.aiSuggestedReceiptId?.let(receiptById::get)?.receipt,
                                onEdit = { onEditLine(line) },
                                onDelete = { onDeleteLine(line) },
                                onAccept = { onAcceptLine(line, it) },
                                onAssign = { onLoadCandidates(line) },
                                onClearMatch = { onClearLineMatch(line) },
                            )
                        }
                        is ReconciliationTimelineEntry.ReceiptOnly -> ReceiptOnlyCard(entry.receipt)
                    }
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

private sealed interface ReconciliationTimelineEntry {
    val occurredAt: Long?
    val key: String

    data class Statement(
        val related: StatementLineWithMatches,
        val matchedReceipt: ReceiptEntity?,
    ) : ReconciliationTimelineEntry {
        override val occurredAt: Long? = related.line.occurredOn ?: matchedReceipt?.occurredAt
        override val key: String = "line:${related.line.id}"
    }

    data class ReceiptOnly(val receipt: ReceiptEntity) : ReconciliationTimelineEntry {
        override val occurredAt: Long = receipt.occurredAt
        override val key: String = "receipt:${receipt.id}"
    }
}

@Composable
private fun ReconciliationSummaryCard(
    reconciliation: ReconciliationWithLines,
    receiptsForStatement: List<ReceiptEntity>,
    analysisState: ReconciliationAnalysisState,
) {
    val audit = ReconciliationAuditor.audit(reconciliation, receiptsForStatement)
    val narrativeFacts = ReconciliationNarrator.facts(reconciliation, receiptsForStatement)
    val statementTotalText = formatSummaryTotals(audit.statementTotals)
        ?: stringResource(R.string.summary_no_amount)
    val matchedReceiptTotalText = formatSummaryTotals(audit.matchedReceiptTotals)
        ?: stringResource(R.string.summary_no_amount)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.reconciliation_summary),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryMetricTile(
                    title = stringResource(R.string.summary_statement_total),
                    value = statementTotalText,
                    modifier = Modifier.weight(1f),
                )
                SummaryMetricTile(
                    title = stringResource(R.string.summary_matched_receipt_total),
                    value = matchedReceiptTotalText,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryMetricTile(
                    title = stringResource(R.string.summary_unmatched_receipts),
                    value = audit.receiptsWithoutRecognizedLineCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                SummaryMetricTile(
                    title = stringResource(R.string.summary_unmatched_statement_lines),
                    value = audit.recognizedLinesWithoutReceiptCount.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.summary_short_conclusion), fontWeight = FontWeight.SemiBold)
            Text(reconciliationNarrative(narrativeFacts))
            AiSummaryDisclosure(
                reconciliationId = reconciliation.reconciliation.id,
                summary = reconciliation.reconciliation.analysisSummary,
                analysisState = analysisState,
            )
        }
    }
}

@Composable
private fun SummaryMetricTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatSummaryTotals(totals: Map<String, BigInteger>): String? = totals
    .toSortedMap()
    .entries
    .takeIf { it.isNotEmpty() }
    ?.joinToString(" · ") { (currency, amount) -> formatBigMinor(amount, currency) }

@Composable
private fun reconciliationNarrative(facts: ReconciliationNarrativeFacts): String {
    val sentences = mutableListOf(
        when (facts.coverage) {
            ReconciliationCoverage.EMPTY -> stringResource(R.string.narrative_empty)
            ReconciliationCoverage.NONE -> stringResource(R.string.narrative_none)
            ReconciliationCoverage.FEW -> stringResource(R.string.narrative_few)
            ReconciliationCoverage.SOME -> stringResource(
                R.string.narrative_some,
                facts.matchedLineCount,
                facts.recognizedLineCount,
            )
            ReconciliationCoverage.MOST -> stringResource(R.string.narrative_most)
            ReconciliationCoverage.ALL -> stringResource(R.string.narrative_all)
        },
    )
    if (facts.issues.size in 1..3) {
        facts.issues.forEach { issue ->
            val label = when {
                issue.checkNumber.isBlank() -> issue.description
                issue.description.isBlank() -> "#${issue.checkNumber}"
                else -> stringResource(R.string.narrative_entry_label, issue.description, issue.checkNumber)
            }
            val date = issue.occurredAt?.let(::formatDate)
            sentences += when (issue.kind) {
                ReconciliationNarrativeIssueKind.STATEMENT_WITHOUT_RECEIPT -> if (date == null) {
                    stringResource(R.string.narrative_statement_open_without_date, label)
                } else {
                    stringResource(R.string.narrative_statement_open, label, date)
                }
                ReconciliationNarrativeIssueKind.RECEIPT_WITHOUT_STATEMENT -> if (date == null) {
                    stringResource(R.string.narrative_receipt_open_without_date, label)
                } else {
                    stringResource(R.string.narrative_receipt_open, label, date)
                }
                ReconciliationNarrativeIssueKind.RECEIPT_OUTSIDE_DATE_RANGE -> if (date == null) {
                    stringResource(R.string.narrative_receipt_outside_without_date, label)
                } else {
                    stringResource(R.string.narrative_receipt_outside, label, date)
                }
                ReconciliationNarrativeIssueKind.QUESTIONABLE_MATCH -> if (date == null) {
                    stringResource(R.string.narrative_questionable_without_date, label)
                } else {
                    stringResource(R.string.narrative_questionable, label, date)
                }
            }
        }
    } else if (facts.issues.size > 3) {
        val openParts = buildList {
            if (facts.openStatementCount > 0) add(
                pluralStringResource(
                    R.plurals.narrative_open_statement_count,
                    facts.openStatementCount,
                    facts.openStatementCount,
                ),
            )
            if (facts.unmatchedReceiptCount > 0) add(
                pluralStringResource(
                    R.plurals.narrative_open_receipt_count,
                    facts.unmatchedReceiptCount,
                    facts.unmatchedReceiptCount,
                ),
            )
            if (facts.questionableMatchCount > 0) add(
                pluralStringResource(
                    R.plurals.narrative_questionable_match_count,
                    facts.questionableMatchCount,
                    facts.questionableMatchCount,
                ),
            )
        }
        if (openParts.isNotEmpty()) {
            sentences += stringResource(R.string.narrative_open_aggregate, openParts.joinToString(" ${stringResource(R.string.and)} "))
        }
    }
    if (facts.totalMismatch) sentences += stringResource(R.string.narrative_total_mismatch)
    if (facts.dataWarningCount > 0) {
        sentences += pluralStringResource(
            R.plurals.narrative_data_warning_count,
            facts.dataWarningCount,
            facts.dataWarningCount,
        )
    }
    return sentences.joinToString(" ")
}

@Composable
private fun AiSummaryDisclosure(
    reconciliationId: String,
    summary: String?,
    analysisState: ReconciliationAnalysisState,
) {
    var expanded by remember(reconciliationId, summary) { mutableStateOf(false) }
    when {
        analysisState is ReconciliationAnalysisState.Running &&
            analysisState.reconciliationId == reconciliationId -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.ai_summary_running), style = MaterialTheme.typography.bodySmall)
        }
        analysisState is ReconciliationAnalysisState.Error &&
            analysisState.reconciliationId == reconciliationId -> Text(
            stringResource(R.string.ai_summary_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        !summary.isNullOrBlank() -> {
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.ai_summary_hide else R.string.ai_summary_show))
            }
            if (expanded) Text(summary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReceiptOnlyCard(receipt: ReceiptEntity) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(R.string.receipt_only),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Text(receipt.location, style = MaterialTheme.typography.titleMedium)
            Text(
                "#${receipt.checkNumber} · ${formatMinor(receipt.amountMinor, receipt.currencyCode)} · " +
                    formatDate(receipt.occurredAt),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatementLineCard(
    line: StatementLineEntity,
    matchedReceipt: ReceiptEntity?,
    suggestedReceipt: ReceiptEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAccept: (Boolean) -> Unit,
    onAssign: () -> Unit,
    onClearMatch: () -> Unit,
) {
    val status = statusPresentation(line.status)
    val matchScore = matchedReceipt?.let { receipt ->
        ReconciliationMatcher.rank(line, listOf(receipt)).single().score.coerceIn(0, 100)
    }
    val ambiguousDateText = if (line.dateAmbiguous) {
        line.sourceDateText?.let { stringResource(R.string.ambiguous_source_date, it) }
            ?: stringResource(R.string.ambiguous_date)
    } else null
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
                    ambiguousDateText?.let { append(" · $it") }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            matchedReceipt?.let {
                Spacer(Modifier.height(8.dp))
                Text("↳ ${it.location} · #${it.checkNumber} · ${formatMinor(it.amountMinor, it.currencyCode)}")
            }
            if (matchedReceipt == null && suggestedReceipt != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(
                        R.string.ai_match_suggestion,
                        line.aiConfidence ?: 0,
                        suggestedReceipt.location,
                        suggestedReceipt.checkNumber,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                line.aiReason?.takeIf(String::isNotBlank)?.let { reason ->
                    Text(reason, style = MaterialTheme.typography.bodySmall)
                }
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
            matchScore?.let { score ->
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.match_score, score),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(score / 100f)
                            .fillMaxHeight()
                            .background(matchScoreColor(score)),
                    )
                }
            }
        }
    }
}

private fun matchScoreColor(score: Int): Color = Color.hsv(
    hue = score.coerceIn(0, 100) * 1.2f,
    saturation = 0.86f,
    value = 0.88f,
)

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
                                    Text("#${candidate.receipt.checkNumber} · ${formatMinor(candidate.receipt.amountMinor, candidate.receipt.currencyCode)}")
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
private fun TitleEditorDialog(
    initialTitle: String = "",
    dialogTitle: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(dialogTitle)) },
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
        confirmButton = {
            TextButton(
                onClick = { onSave(title.trim()) },
                enabled = title.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
    )
}

@Composable
private fun StatementLineEditorDialog(
    existing: StatementLineEntity? = null,
    currencyCode: String,
    currencyCodes: List<String>,
    homeCurrencyCode: String,
    recentCurrencyCodes: List<String>,
    exchangeRateLookup: ExchangeRateLookupState,
    onLookupRate: (String, String) -> Unit,
    onAddTripCurrency: (String, String, Boolean) -> Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Long?) -> Boolean,
) {
    var description by remember(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var check by remember(existing?.id) { mutableStateOf(existing?.checkNumber.orEmpty()) }
    var amount by remember(existing?.id) {
        mutableStateOf(existing?.amountMinor?.let { formatInputMinor(it, existing.currencyCode) }.orEmpty())
    }
    var currency by remember(existing?.id) { mutableStateOf(existing?.currencyCode ?: currencyCode) }
    var date by remember(existing?.id) { mutableStateOf(existing?.occurredOn?.let(::formatDate).orEmpty()) }
    var invalid by remember(existing?.id) { mutableStateOf(false) }
    var showCurrencyPicker by remember(existing?.id) { mutableStateOf(false) }
    var pendingNewCurrencyCode by remember(existing?.id) { mutableStateOf<String?>(null) }
    var pendingCurrencyRate by remember(existing?.id) { mutableStateOf("") }
    var pendingCurrencyDaily by remember(existing?.id) { mutableStateOf(false) }
    var pendingCurrencyRateInvalid by remember(existing?.id) { mutableStateOf(false) }

    LaunchedEffect(exchangeRateLookup, pendingNewCurrencyCode) {
        val success = exchangeRateLookup as? ExchangeRateLookupState.Success ?: return@LaunchedEffect
        if (success.quote.baseCurrencyCode == homeCurrencyCode &&
            success.quote.targetCurrencyCode == pendingNewCurrencyCode
        ) {
            pendingCurrencyRate = success.quote.targetUnitsPerBase
            pendingCurrencyRateInvalid = false
        }
    }
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            amount,
                            { amount = it; invalid = false },
                            label = { Text(stringResource(R.string.amount_in_currency, currency)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = invalid,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TripCurrencySelector(
                            selectedCurrencyCode = currency,
                            currencyCodes = currencyCodes,
                            onCurrencySelected = {
                                currency = it
                                invalid = false
                            },
                            onAddCurrencyRequested = { showCurrencyPicker = true },
                        )
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
    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            selectedCurrencyCode = currency,
            preferredCurrencyCodes = currencyCodes,
            recentCurrencyCodes = recentCurrencyCodes,
            excludedCurrencyCodes = currencyCodes.toSet(),
            onCurrencySelected = { entry ->
                showCurrencyPicker = false
                pendingNewCurrencyCode = entry.code
                pendingCurrencyRate = ""
                pendingCurrencyDaily = false
                pendingCurrencyRateInvalid = false
                onLookupRate(homeCurrencyCode, entry.code)
            },
            onDismiss = { showCurrencyPicker = false },
        )
    }
    pendingNewCurrencyCode?.let { newCode ->
        AlertDialog(
            onDismissRequest = { pendingNewCurrencyCode = null },
            title = { Text(stringResource(R.string.trip_currency_add_other)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.trip_currency_rate_formula, homeCurrencyCode, newCode))
                    OutlinedTextField(
                        value = pendingCurrencyRate,
                        onValueChange = {
                            pendingCurrencyRate = it
                            pendingCurrencyRateInvalid = false
                        },
                        isError = pendingCurrencyRateInvalid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            pendingCurrencyDaily = !pendingCurrencyDaily
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.daily_exchange_rate), modifier = Modifier.weight(1f))
                        Switch(checked = pendingCurrencyDaily, onCheckedChange = null)
                    }
                    if (exchangeRateLookup is ExchangeRateLookupState.Error &&
                        exchangeRateLookup.target == newCode
                    ) {
                        Text(
                            stringResource(R.string.rate_lookup_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingNewCurrencyCode = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val validRate = pendingCurrencyRate.trim().replace(',', '.')
                        .toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }
                        ?.stripTrailingZeros()?.toPlainString()
                    if (validRate == null) {
                        pendingCurrencyRateInvalid = true
                    } else if (onAddTripCurrency(newCode, validRate, pendingCurrencyDaily)) {
                        currency = newCode
                        invalid = false
                        pendingNewCurrencyCode = null
                    }
                }) { Text(stringResource(R.string.trip_currency_add)) }
            },
        )
    }
}

@Composable
private fun StatementImageActions(
    hasImage: Boolean,
    emphasizeAnalyze: Boolean,
    onOpen: () -> Unit,
    onChoose: () -> Unit,
    onRemove: () -> Unit,
    onAnalyze: () -> Unit,
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
                Button(
                    onClick = onAnalyze,
                    modifier = Modifier.fillMaxWidth().nextStepPulse(emphasizeAnalyze),
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.analyze_image))
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

internal enum class ReconciliationNextStep {
    ANALYZE_IMAGE,
    RUN_RECONCILIATION,
    NONE,
}

internal fun reconciliationNextStep(
    hasImage: Boolean,
    statementLineCount: Int,
    analysisUpdatedAt: Long?,
): ReconciliationNextStep = when {
    statementLineCount > 0 && analysisUpdatedAt == null ->
        ReconciliationNextStep.RUN_RECONCILIATION
    hasImage && statementLineCount == 0 -> ReconciliationNextStep.ANALYZE_IMAGE
    else -> ReconciliationNextStep.NONE
}

@Composable
private fun Modifier.nextStepPulse(enabled: Boolean): Modifier {
    if (!enabled) return this
    val transition = rememberInfiniteTransition(label = "next step pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "next step scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "next step alpha",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
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
    ReconciliationStatus.CURRENCY_MISMATCH -> StatusPresentation(R.string.status_currency_mismatch, Color(0xFFC62828))
    ReconciliationStatus.DATE_MISMATCH -> StatusPresentation(R.string.status_date_mismatch, Color(0xFFEF6C00))
    ReconciliationStatus.ACCEPTED -> StatusPresentation(R.string.status_accepted, Color(0xFF2E7D32))
    else -> StatusPresentation(R.string.status_not_found, MaterialTheme.colorScheme.error)
}

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")
    .withResolverStyle(ResolverStyle.STRICT)

private fun parseDate(value: String): Long? = try {
    value.trim().takeIf { it.isNotEmpty() }?.let {
        LocalDate.parse(it, dateFormatter).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
} catch (_: DateTimeParseException) {
    null
}

private fun formatDate(value: Long): String =
    Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

private fun formatInputMinor(value: Long, currencyCode: String): String =
    NumberFormat.getNumberInstance(Locale.GERMANY).apply {
        minimumFractionDigits = CurrencyAmount.fractionDigits(currencyCode)
        maximumFractionDigits = CurrencyAmount.fractionDigits(currencyCode)
        isGroupingUsed = false
    }.format(BigDecimal.valueOf(value).movePointLeft(CurrencyAmount.fractionDigits(currencyCode)))

private fun formatMinor(value: Long, currencyCode: String): String =
    CurrencyAmount.formatMinor(value, currencyCode)

private fun formatBigMinor(value: BigInteger, currencyCode: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(currencyCode) }
        .format(BigDecimal(value).movePointLeft(CurrencyAmount.fractionDigits(currencyCode)))
}.getOrElse {
    "${BigDecimal(value).movePointLeft(CurrencyAmount.fractionDigits(currencyCode)).toPlainString()} $currencyCode"
}
