package de.shakie.billcheck.data

import de.shakie.billcheck.domain.MoneyCalculator
import de.shakie.billcheck.domain.RankedReceiptCandidate
import de.shakie.billcheck.domain.ReconciliationMatcher
import de.shakie.billcheck.domain.ReconciliationStatus
import de.shakie.billcheck.domain.ExtractedStatement
import de.shakie.billcheck.domain.ReceiptSnapshotRules
import de.shakie.billcheck.domain.StatementExtractionValidator
import de.shakie.billcheck.domain.VerifiedReconciliationEntry
import de.shakie.billcheck.domain.VerifiedReconciliationReport
import java.util.UUID
import java.util.Locale

data class NewReceiptItem(
    val name: String,
    val amountMinor: Long,
)

data class NewStatementLine(
    val description: String,
    val checkNumber: String,
    val amountMinor: Long,
    val currencyCode: String,
    val occurredOn: Long? = null,
)

class BillCheckRepository(database: BillCheckDatabase) {
    private val dao = database.dao()

    val trips = dao.observeTrips()

    fun receipts(tripId: String) = dao.observeReceipts(tripId)

    fun locationSuggestions(tripId: String) = dao.observeLocationSuggestions(tripId)

    fun itemNameSuggestions(tripId: String) = dao.observeItemNameSuggestions(tripId)

    fun reconciliations(tripId: String) = dao.observeReconciliations(tripId)

    suspend fun createTrip(
        name: String,
        currencyCode: String,
        exchangeRate: String,
        exchangeRateMode: String,
        defaultTipMinor: Long = 100,
        defaultTipCurrencyCode: String = "EUR",
        defaultTipSelected: Boolean = false,
    ): TripEntity {
        val now = System.currentTimeMillis()
        return TripEntity(
            id = UUID.randomUUID().toString(),
            sortPosition = dao.nextTripPosition(),
            name = name.trim().ifBlank { "Reise 1" },
            foreignCurrencyCode = currencyCode,
            defaultExchangeRate = exchangeRate,
            exchangeRateMode = exchangeRateMode,
            defaultTipMinor = defaultTipMinor,
            defaultTipCurrencyCode = defaultTipCurrencyCode,
            defaultTipSelected = defaultTipSelected,
            imageStorageMode = "ORIGINAL",
            createdAt = now,
        ).also { dao.insertTrip(it) }
    }

    suspend fun updateTrip(
        existing: TripEntity,
        name: String,
        currencyCode: String,
        exchangeRate: String,
        exchangeRateMode: String,
        defaultTipMinor: Long,
        defaultTipCurrencyCode: String,
        defaultTipSelected: Boolean,
    ) {
        dao.updateTrip(
            existing.copy(
                name = name.trim().ifBlank { existing.name },
                foreignCurrencyCode = currencyCode,
                defaultExchangeRate = exchangeRate,
                exchangeRateMode = exchangeRateMode,
                defaultTipMinor = defaultTipMinor,
                defaultTipCurrencyCode = defaultTipCurrencyCode,
                defaultTipSelected = defaultTipSelected,
            ),
        )
    }

    suspend fun reorderTrips(orderedTripIds: List<String>) =
        dao.replaceTripOrder(orderedTripIds)

    suspend fun addReceipt(
        trip: TripEntity,
        location: String,
        checkNumber: String,
        foreignAmountMinor: Long,
        addDefaultTip: Boolean,
        items: List<NewReceiptItem> = emptyList(),
        exchangeRate: String = trip.defaultExchangeRate,
        imageUri: String? = null,
        occurredAt: Long = System.currentTimeMillis(),
    ) {
        val now = System.currentTimeMillis()
        val tipMinor = if (addDefaultTip) trip.defaultTipMinor else 0
        val receiptId = UUID.randomUUID().toString()
        val receipt = ReceiptEntity(
                id = receiptId,
                tripId = trip.id,
                occurredAt = occurredAt,
                location = location.trim(),
                checkNumber = checkNumber.trim(),
                foreignAmountMinor = foreignAmountMinor,
                foreignCurrencyCode = trip.foreignCurrencyCode,
                exchangeRate = exchangeRate,
                exactEuroCents = MoneyCalculator.calculateExactEuroCents(
                    foreignAmountMinor = foreignAmountMinor,
                    exchangeRate = exchangeRate,
                    tipMinor = tipMinor,
                    tipCurrencyCode = trip.defaultTipCurrencyCode,
                ),
                tipMinor = tipMinor,
                tipCurrencyCode = trip.defaultTipCurrencyCode,
                imageUri = imageUri,
                reviewState = "CONFIRMED",
                createdAt = now,
            )
        val receiptItems = items.mapIndexed { index, item ->
            ReceiptItemEntity(
                id = UUID.randomUUID().toString(),
                receiptId = receiptId,
                sortPosition = index,
                name = item.name.trim(),
                amountMinor = item.amountMinor,
                currencyCode = trip.foreignCurrencyCode,
            )
        }
        dao.insertReceiptWithItems(
            receipt = receipt,
            items = receiptItems,
        )
        dao.clearTripAnalyses(trip.id)
    }

    suspend fun deleteReceipt(receipt: ReceiptEntity) {
        val affectedLines = dao.getStatementLinesForReceipt(receipt.id)
        dao.deleteReceipt(receipt)
        affectedLines.forEach { line ->
            dao.updateStatementLine(
                line.copy(status = ReconciliationStatus.NOT_FOUND, acceptedWithoutReceipt = false),
            )
        }
        dao.clearTripAnalyses(receipt.tripId)
    }

    suspend fun updateReceipt(
        trip: TripEntity,
        existing: ReceiptEntity,
        location: String,
        checkNumber: String,
        foreignAmountMinor: Long,
        occurredAt: Long,
        addDefaultTip: Boolean,
        items: List<NewReceiptItem>,
    ) {
        val tip = ReceiptSnapshotRules.tipForEdit(
            existingTipMinor = existing.tipMinor,
            existingTipCurrencyCode = existing.tipCurrencyCode,
            currentDefaultTipMinor = trip.defaultTipMinor,
            currentDefaultTipCurrencyCode = trip.defaultTipCurrencyCode,
            selected = addDefaultTip,
        )
        val receipt = existing.copy(
            occurredAt = occurredAt,
            location = location.trim(),
            checkNumber = checkNumber.trim(),
            foreignAmountMinor = foreignAmountMinor,
            exactEuroCents = MoneyCalculator.calculateExactEuroCents(
                foreignAmountMinor = foreignAmountMinor,
                exchangeRate = existing.exchangeRate,
                tipMinor = tip.minor,
                tipCurrencyCode = tip.currencyCode,
            ),
            tipMinor = tip.minor,
            tipCurrencyCode = tip.currencyCode,
        )
        val receiptItems = items.mapIndexed { index, item ->
            ReceiptItemEntity(
                id = UUID.randomUUID().toString(),
                receiptId = existing.id,
                sortPosition = index,
                name = item.name.trim(),
                amountMinor = item.amountMinor,
                currencyCode = existing.foreignCurrencyCode,
            )
        }
        dao.updateReceiptWithItems(receipt, receiptItems)
        dao.getStatementLinesForReceipt(receipt.id).forEach { line ->
            dao.updateStatementLine(line.copy(status = ReconciliationMatcher.suggestedStatus(line, receipt)))
        }
        dao.clearTripAnalyses(trip.id)
    }

    suspend fun updateReceiptImage(receiptId: String, imageUri: String?) =
        dao.updateReceiptImage(receiptId, imageUri)

    suspend fun createReconciliation(
        trip: TripEntity,
        title: String,
        imageUri: String? = null,
    ): ReconciliationEntity {
        val now = System.currentTimeMillis()
        return ReconciliationEntity(
            id = UUID.randomUUID().toString(),
            tripId = trip.id,
            title = title.trim().ifBlank { "Rechnung" },
            statementImageUri = imageUri,
            createdAt = now,
        ).also { dao.insertReconciliation(it) }
    }

    suspend fun updateReconciliation(
        reconciliationId: String,
        title: String,
        imageUri: String?,
    ) {
        val existing = checkNotNull(dao.getReconciliation(reconciliationId)).reconciliation
        val imageChanged = existing.statementImageUri != imageUri
        dao.updateReconciliationEntity(
            existing.copy(
                title = title.trim().ifBlank { "Rechnung" },
                statementImageUri = imageUri,
                declaredTotalMinor = if (imageChanged) null else existing.declaredTotalMinor,
                declaredTotalCurrencyCode = if (imageChanged) null else existing.declaredTotalCurrencyCode,
            ),
        )
        dao.clearReconciliationAnalysis(reconciliationId)
    }

    suspend fun addStatementLine(
        reconciliationId: String,
        input: NewStatementLine,
    ) {
        dao.insertStatementLine(
            StatementLineEntity(
                id = UUID.randomUUID().toString(),
                reconciliationId = reconciliationId,
                occurredOn = input.occurredOn,
                description = input.description.trim(),
                checkNumber = input.checkNumber.trim(),
                amountMinor = input.amountMinor,
                currencyCode = input.currencyCode,
                status = ReconciliationStatus.NOT_FOUND,
                acceptedWithoutReceipt = false,
            ),
        )
        dao.clearReconciliationAnalysis(reconciliationId)
    }

    suspend fun updateStatementLine(
        existing: StatementLineEntity,
        input: NewStatementLine,
    ) {
        dao.deleteStatementLineMatch(existing.id)
        dao.updateStatementLine(
            existing.copy(
                occurredOn = input.occurredOn,
                description = input.description.trim(),
                checkNumber = input.checkNumber.trim(),
                amountMinor = input.amountMinor,
                currencyCode = input.currencyCode,
                status = ReconciliationStatus.NOT_FOUND,
                acceptedWithoutReceipt = false,
                aiSuggestedReceiptId = null,
                aiConfidence = null,
                aiReason = null,
                sourceDateText = null,
                dateAmbiguous = false,
            ),
        )
        dao.clearReconciliationAnalysis(existing.reconciliationId)
    }

    suspend fun deleteStatementLine(line: StatementLineEntity) {
        dao.deleteStatementLine(line)
        dao.clearReconciliationAnalysis(line.reconciliationId)
    }

    suspend fun setAccepted(line: StatementLineEntity, accepted: Boolean) {
        dao.deleteStatementLineMatch(line.id)
        dao.updateStatementLine(
            line.copy(
                acceptedWithoutReceipt = accepted,
                status = if (accepted) ReconciliationStatus.ACCEPTED else ReconciliationStatus.NOT_FOUND,
            ),
        )
        dao.clearReconciliationAnalysis(line.reconciliationId)
    }

    suspend fun rankCandidates(
        tripId: String,
        line: StatementLineEntity,
    ): List<RankedReceiptCandidate> {
        val usedReceiptIds = dao.getReconciliationMatches(line.reconciliationId)
            .mapTo(mutableSetOf()) { it.receiptId }
        return ReconciliationMatcher.rank(
            line,
            dao.getReceipts(tripId).filterNot { it.id in usedReceiptIds },
        )
    }

    suspend fun assignReceipt(line: StatementLineEntity, receipt: ReceiptEntity, manually: Boolean) {
        dao.replaceReceiptMatch(
            ReceiptMatchEntity(line.id, receipt.id, manually),
            reconciliationId = line.reconciliationId,
        )
        dao.updateStatementLine(
            line.copy(
                status = ReconciliationMatcher.suggestedStatus(line, receipt),
                acceptedWithoutReceipt = false,
            ),
        )
        dao.clearReconciliationAnalysis(line.reconciliationId)
    }

    suspend fun clearLineMatch(line: StatementLineEntity) {
        dao.deleteStatementLineMatch(line.id)
        dao.updateStatementLine(
            line.copy(status = ReconciliationStatus.NOT_FOUND, acceptedWithoutReceipt = false),
        )
        dao.clearReconciliationAnalysis(line.reconciliationId)
    }

    suspend fun runAutomaticReconciliation(
        tripId: String,
        reconciliation: ReconciliationWithLines,
    ): VerifiedReconciliationReport {
        dao.clearReconciliationAnalysis(reconciliation.reconciliation.id)
        dao.resetMatches(reconciliation.reconciliation.id)
        val available = dao.getReceipts(tripId).toMutableList()
        val remainingLines = reconciliation.lines.map { it.line }
            .filterNot { it.acceptedWithoutReceipt }
            .toMutableList()
        while (remainingLines.isNotEmpty() && available.isNotEmpty()) {
            val proposals = remainingLines.mapNotNull { line ->
                val ranked = ReconciliationMatcher.rank(line, available)
                val receipt = ReconciliationMatcher.selectAutomaticMatch(line, ranked) ?: return@mapNotNull null
                AutomaticProposal(
                    line = line,
                    receipt = receipt,
                    exactCheck = ReconciliationMatcher.normalizeCheckNumber(line.checkNumber) ==
                        ReconciliationMatcher.normalizeCheckNumber(receipt.checkNumber),
                    score = ranked.first().score,
                    margin = ranked.first().score - (ranked.getOrNull(1)?.score ?: 0),
                )
            }
            val selected = proposals.sortedWith(
                compareByDescending<AutomaticProposal> { it.exactCheck }
                    .thenByDescending { it.margin }
                    .thenByDescending { it.score },
            ).firstOrNull() ?: break
            assignReceipt(selected.line, selected.receipt, manually = false)
            remainingLines.removeAll { it.id == selected.line.id }
            available.removeAll { it.id == selected.receipt.id }
        }
        remainingLines.forEach { line ->
            val status = ReconciliationMatcher.rank(line, available).firstOrNull()?.receipt?.let {
                ReconciliationMatcher.suggestedStatus(line, it)
            }?.takeIf {
                it in setOf(
                    ReconciliationStatus.AMOUNT_MISMATCH,
                    ReconciliationStatus.CURRENCY_MISMATCH,
                    ReconciliationStatus.DATE_MISMATCH,
                )
            } ?: ReconciliationStatus.NOT_FOUND
            dao.updateStatementLine(line.copy(status = status, acceptedWithoutReceipt = false))
        }
        return buildVerifiedReport(tripId, reconciliation.reconciliation.id)
    }

    suspend fun resetReconciliation(reconciliation: ReconciliationWithLines) {
        dao.clearReconciliationAnalysis(reconciliation.reconciliation.id)
        dao.resetMatches(reconciliation.reconciliation.id)
        reconciliation.lines.forEach { related ->
            dao.updateStatementLine(
                related.line.copy(
                    status = if (related.line.acceptedWithoutReceipt) {
                        ReconciliationStatus.ACCEPTED
                    } else {
                        ReconciliationStatus.NOT_FOUND
                    },
                ),
            )
        }
    }

    suspend fun deleteReconciliation(reconciliationId: String) =
        dao.deleteReconciliation(reconciliationId)

    suspend fun applyExtractedStatement(
        reconciliation: ReconciliationEntity,
        extracted: ExtractedStatement,
        fallbackCurrencyCode: String,
    ) {
        val validated = StatementExtractionValidator.validate(extracted, fallbackCurrencyCode)
        val lines = validated.lines.map { line ->
            StatementLineEntity(
                id = UUID.randomUUID().toString(),
                reconciliationId = reconciliation.id,
                occurredOn = line.occurredOn,
                description = line.description,
                checkNumber = line.checkNumber,
                amountMinor = line.amountMinor,
                currencyCode = line.currencyCode,
                status = ReconciliationStatus.NOT_FOUND,
                acceptedWithoutReceipt = false,
                sourceDateText = line.sourceDateText,
                dateAmbiguous = line.dateAmbiguous,
            )
        }
        dao.applyExtractedStatement(
            reconciliation = reconciliation.copy(
                title = validated.title.ifBlank { reconciliation.title },
                analysisSummary = null,
                analysisUpdatedAt = null,
                declaredTotalMinor = validated.declaredTotalMinor,
                declaredTotalCurrencyCode = validated.declaredTotalCurrencyCode,
            ),
            lines = lines,
        )
    }

    suspend fun storeReconciliationSummary(reconciliationId: String, summary: String) {
        dao.updateReconciliationAnalysis(
            reconciliationId,
            summary.trim().take(4_000).takeIf(String::isNotEmpty),
            System.currentTimeMillis(),
        )
    }

    private suspend fun buildVerifiedReport(
        tripId: String,
        reconciliationId: String,
    ): VerifiedReconciliationReport {
        val reconciliation = checkNotNull(dao.getReconciliation(reconciliationId))
        val receipts = dao.getReceipts(tripId)
        val receiptById = receipts.associateBy { it.id }
        val audit = de.shakie.billcheck.domain.ReconciliationAuditor.audit(reconciliation, receipts)
        val entries = reconciliation.lines.map { related ->
            val line = related.line
            val receipt = related.matches.singleOrNull()?.receiptId?.let(receiptById::get)
            VerifiedReconciliationEntry(
                kind = when {
                    line.acceptedWithoutReceipt -> VerifiedReconciliationReport.KIND_ACCEPTED
                    receipt != null -> VerifiedReconciliationReport.KIND_MATCHED
                    else -> VerifiedReconciliationReport.KIND_STATEMENT_ONLY
                },
                occurredAt = line.occurredOn ?: receipt?.occurredAt,
                description = line.description,
                statementCheckNumber = line.checkNumber.takeIf(String::isNotBlank),
                receiptCheckNumber = receipt?.checkNumber,
                statementAmountMinor = line.amountMinor,
                receiptAmountMinor = receipt?.foreignAmountMinor,
                currencyCode = line.currencyCode,
                status = when {
                    line.acceptedWithoutReceipt -> ReconciliationStatus.ACCEPTED
                    receipt != null -> ReconciliationMatcher.suggestedStatus(line, receipt)
                    else -> line.status
                },
            )
        }.toMutableList()
        val currentMatchedReceiptIds = reconciliation.lines.flatMap { it.matches }
            .mapTo(mutableSetOf()) { it.receiptId }
        receipts.filterNot { it.id in currentMatchedReceiptIds }.forEach { receipt ->
            entries += VerifiedReconciliationEntry(
                kind = VerifiedReconciliationReport.KIND_RECEIPT_ONLY,
                occurredAt = receipt.occurredAt,
                description = receipt.location,
                statementCheckNumber = null,
                receiptCheckNumber = receipt.checkNumber,
                statementAmountMinor = null,
                receiptAmountMinor = receipt.foreignAmountMinor,
                currencyCode = receipt.foreignCurrencyCode,
                status = ReconciliationStatus.NOT_FOUND,
            )
        }
        return VerifiedReconciliationReport(
            reconciliationId = reconciliationId,
            title = reconciliation.reconciliation.title,
            languageCode = Locale.getDefault().language,
            entries = entries.sortedWith(
                compareBy<VerifiedReconciliationEntry> { it.occurredAt ?: Long.MAX_VALUE }
                    .thenBy { it.description },
            ),
            recognizedLineCount = audit.recognizedLineCount,
            declaredTotalMinor = audit.declaredTotalMinor?.toString(),
            declaredTotalCurrencyCode = audit.declaredTotalCurrencyCode,
            declaredTotalDifferenceMinor = audit.declaredTotalDifferenceMinor?.toString(),
            totalCheck = audit.totalCheck.name,
            auditWarnings = buildList {
                if (audit.ambiguousDateCount > 0) add("AMBIGUOUS_DATES=${audit.ambiguousDateCount}")
                if (audit.receiptsOutsideRecognizedDateRangeCount > 0) {
                    add("RECEIPTS_OUTSIDE_RECOGNIZED_DATE_RANGE=${audit.receiptsOutsideRecognizedDateRangeCount}")
                }
                if (audit.duplicateStatementLineCount > 0) {
                    add("POSSIBLE_DUPLICATE_STATEMENT_LINES=${audit.duplicateStatementLineCount}")
                }
                if (audit.duplicateReceiptCount > 0) add("POSSIBLE_DUPLICATE_RECEIPTS=${audit.duplicateReceiptCount}")
                if (audit.nonPositiveAmountCount > 0) add("NON_POSITIVE_AMOUNTS=${audit.nonPositiveAmountCount}")
            },
        )
    }

    private data class AutomaticProposal(
        val line: StatementLineEntity,
        val receipt: ReceiptEntity,
        val exactCheck: Boolean,
        val score: Int,
        val margin: Int,
    )
}
