package de.shakie.billcheck.data

import de.shakie.billcheck.domain.MoneyCalculator
import de.shakie.billcheck.domain.CurrencyAmount
import de.shakie.billcheck.domain.CurrencyCatalog
import de.shakie.billcheck.domain.RankedReceiptCandidate
import de.shakie.billcheck.domain.ReconciliationMatcher
import de.shakie.billcheck.domain.ReconciliationStatus
import de.shakie.billcheck.domain.ExtractedStatement
import de.shakie.billcheck.domain.StatementExtractionValidator
import de.shakie.billcheck.domain.VerifiedReconciliationEntry
import de.shakie.billcheck.domain.VerifiedReconciliationReport
import java.util.UUID
import java.util.Locale

data class NewReceiptItem(
    val name: String,
    val amountMinor: Long,
)

data class TripCurrencyInput(
    val currencyCode: String,
    val homeToCurrencyRate: String,
    val exchangeRateMode: String,
    val isDefault: Boolean,
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

    fun tripCurrencies(tripId: String) = dao.observeTripCurrencies(tripId)

    fun usedTripCurrencyCodes(tripId: String) = dao.observeUsedReceiptCurrencyCodes(tripId)

    fun receipts(tripId: String) = dao.observeReceipts(tripId)

    fun visibleBatchImports(tripId: String) = dao.observeVisibleBatchImports(tripId)

    fun locationSuggestions(tripId: String) = dao.observeLocationSuggestions(tripId)

    fun itemNameSuggestions(tripId: String) = dao.observeItemNameSuggestions(tripId)

    fun reconciliations(tripId: String) = dao.observeReconciliations(tripId)

    suspend fun createTrip(
        name: String,
        homeCurrencyCode: String,
        currencies: List<TripCurrencyInput>,
        defaultTipMinor: Long = 100,
        defaultTipCurrencyCode: String = homeCurrencyCode,
        defaultTipSelected: Boolean = false,
    ): TripEntity {
        require(defaultTipMinor >= 0) { "Default tip must not be negative" }
        val now = System.currentTimeMillis()
        val tripId = UUID.randomUUID().toString()
        val normalizedHome = requireSupportedCurrency(homeCurrencyCode)
        val trip = TripEntity(
            id = tripId,
            sortPosition = dao.nextTripPosition(),
            name = name.trim().ifBlank { "Reise 1" },
            homeCurrencyCode = normalizedHome,
            defaultTipMinor = defaultTipMinor,
            defaultTipCurrencyCode = requireSupportedCurrency(defaultTipCurrencyCode),
            defaultTipSelected = defaultTipSelected,
            imageStorageMode = "ORIGINAL",
            createdAt = now,
        )
        val entities = normalizedTripCurrencies(tripId, normalizedHome, currencies)
        require(trip.defaultTipCurrencyCode in entities.map { it.currencyCode }) {
            "Default tip currency must be available on the trip"
        }
        dao.insertTripWithCurrencies(trip, entities)
        return trip
    }

    suspend fun updateTrip(
        existing: TripEntity,
        name: String,
        currencies: List<TripCurrencyInput>,
        defaultTipMinor: Long,
        defaultTipCurrencyCode: String,
        defaultTipSelected: Boolean,
    ) {
        require(defaultTipMinor >= 0) { "Default tip must not be negative" }
        val entities = normalizedTripCurrencies(existing.id, existing.homeCurrencyCode, currencies)
        val tipCode = requireSupportedCurrency(defaultTipCurrencyCode)
        require(tipCode in entities.map { it.currencyCode }) {
            "Default tip currency must be available on the trip"
        }
        val configuredCodes = entities.mapTo(hashSetOf()) { it.currencyCode }
        val usedCodes = dao.getReceipts(existing.id).flatMapTo(hashSetOf()) {
            if (it.tipMinor > 0) listOf(it.currencyCode, it.tipCurrencyCode) else listOf(it.currencyCode)
        }
        require(configuredCodes.containsAll(usedCodes)) {
            "Currencies used by existing receipts cannot be removed"
        }
        dao.updateTripWithCurrencies(
            existing.copy(
                name = name.trim().ifBlank { existing.name },
                defaultTipMinor = defaultTipMinor,
                defaultTipCurrencyCode = tipCode,
                defaultTipSelected = defaultTipSelected,
            ),
            entities,
        )
    }

    suspend fun reorderTrips(orderedTripIds: List<String>) =
        dao.replaceTripOrder(orderedTripIds)

    suspend fun deleteTrip(trip: TripEntity) {
        dao.deleteTrip(trip)
    }

    suspend fun addReceipt(
        trip: TripEntity,
        location: String,
        checkNumber: String,
        amountMinor: Long,
        currencyCode: String,
        exchangeRateSnapshot: String,
        tipMinor: Long,
        tipCurrencyCode: String,
        tipExchangeRateSnapshot: String,
        items: List<NewReceiptItem> = emptyList(),
        imageUri: String? = null,
        occurredAt: Long = System.currentTimeMillis(),
        reviewState: String = ReceiptReviewState.CONFIRMED,
        receiptId: String = UUID.randomUUID().toString(),
    ): ReceiptEntity {
        require(amountMinor > 0) { "Receipt amount must be positive" }
        require(tipMinor >= 0) { "Tip must not be negative" }
        require(items.all { it.amountMinor >= 0 }) { "Item amounts must not be negative" }
        val now = System.currentTimeMillis()
        val receiptCurrency = requireTripCurrency(trip, currencyCode)
        val receiptRate = requireRateSnapshot(trip.homeCurrencyCode, receiptCurrency, exchangeRateSnapshot)
        val tipCurrency = if (tipMinor == 0L) {
            trip.homeCurrencyCode
        } else {
            requireTripCurrency(trip, tipCurrencyCode)
        }
        val tipRate = if (tipMinor == 0L) {
            "1"
        } else {
            requireRateSnapshot(trip.homeCurrencyCode, tipCurrency, tipExchangeRateSnapshot)
        }
        val receipt = ReceiptEntity(
                id = receiptId,
                tripId = trip.id,
                occurredAt = occurredAt,
                location = location.trim(),
                checkNumber = checkNumber.trim(),
                amountMinor = amountMinor,
                currencyCode = receiptCurrency,
                exchangeRateSnapshot = receiptRate,
                exactHomeMinor = MoneyCalculator.calculateExactHomeMinor(
                    amountMinor = amountMinor,
                    currencyCode = receiptCurrency,
                    exchangeRateSnapshot = receiptRate,
                    tipMinor = tipMinor,
                    tipCurrencyCode = tipCurrency,
                    tipExchangeRateSnapshot = tipRate,
                    homeCurrencyCode = trip.homeCurrencyCode,
                ),
                tipMinor = tipMinor,
                tipCurrencyCode = tipCurrency,
                tipExchangeRateSnapshot = tipRate,
                imageUri = imageUri,
                reviewState = reviewState,
                createdAt = now,
            )
        val receiptItems = items.mapIndexed { index, item ->
            ReceiptItemEntity(
                id = UUID.randomUUID().toString(),
                receiptId = receiptId,
                sortPosition = index,
                name = item.name.trim(),
                amountMinor = item.amountMinor,
                currencyCode = receiptCurrency,
            )
        }
        dao.insertReceiptWithItems(
            receipt = receipt,
            items = receiptItems,
        )
        dao.clearTripAnalyses(trip.id)
        return receipt
    }

    suspend fun deleteReceipt(receipt: ReceiptEntity) {
        val affectedLines = dao.getStatementLinesForReceipt(receipt.id)
        dao.deleteBatchImportForReceipt(receipt.id)
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
        amountMinor: Long,
        currencyCode: String,
        /** Required only when [currencyCode] differs from the stored currency. */
        exchangeRateSnapshot: String?,
        occurredAt: Long,
        tipMinor: Long,
        tipCurrencyCode: String,
        /** Required only when [tipCurrencyCode] differs from the stored tip currency. */
        tipExchangeRateSnapshot: String?,
        items: List<NewReceiptItem>,
    ) {
        require(amountMinor > 0) { "Receipt amount must be positive" }
        require(tipMinor >= 0) { "Tip must not be negative" }
        require(items.all { it.amountMinor >= 0 }) { "Item amounts must not be negative" }
        val receiptCurrency = requireTripCurrency(trip, currencyCode)
        val receiptRate = if (receiptCurrency == existing.currencyCode) {
            existing.exchangeRateSnapshot
        } else {
            requireNotNull(exchangeRateSnapshot) { "A changed receipt currency needs an exchange rate" }
        }.let { requireRateSnapshot(trip.homeCurrencyCode, receiptCurrency, it) }
        val tipCurrency = if (tipMinor == 0L) {
            trip.homeCurrencyCode
        } else {
            requireTripCurrency(trip, tipCurrencyCode)
        }
        val tipRate = if (tipMinor == 0L) {
            "1"
        } else {
            if (tipCurrency == existing.tipCurrencyCode) {
                existing.tipExchangeRateSnapshot
            } else {
                requireNotNull(tipExchangeRateSnapshot) { "A changed tip currency needs an exchange rate" }
            }.let { requireRateSnapshot(trip.homeCurrencyCode, tipCurrency, it) }
        }
        val receipt = existing.copy(
            occurredAt = occurredAt,
            location = location.trim(),
            checkNumber = checkNumber.trim(),
            amountMinor = amountMinor,
            currencyCode = receiptCurrency,
            exchangeRateSnapshot = receiptRate,
            exactHomeMinor = MoneyCalculator.calculateExactHomeMinor(
                amountMinor = amountMinor,
                currencyCode = receiptCurrency,
                exchangeRateSnapshot = receiptRate,
                tipMinor = tipMinor,
                tipCurrencyCode = tipCurrency,
                tipExchangeRateSnapshot = tipRate,
                homeCurrencyCode = trip.homeCurrencyCode,
            ),
            tipMinor = tipMinor,
            tipCurrencyCode = tipCurrency,
            tipExchangeRateSnapshot = tipRate,
            reviewState = ReceiptReviewState.CONFIRMED,
        )
        val receiptItems = items.mapIndexed { index, item ->
            ReceiptItemEntity(
                id = UUID.randomUUID().toString(),
                receiptId = existing.id,
                sortPosition = index,
                name = item.name.trim(),
                amountMinor = item.amountMinor,
                currencyCode = receiptCurrency,
            )
        }
        dao.updateReceiptWithItems(receipt, receiptItems)
        dao.markBatchImportReviewed(receipt.id)
        dao.getStatementLinesForReceipt(receipt.id).forEach { line ->
            dao.updateStatementLine(line.copy(status = ReconciliationMatcher.suggestedStatus(line, receipt)))
        }
        dao.clearTripAnalyses(trip.id)
    }

    suspend fun enqueueBatchReceiptImports(tripId: String, imageUris: List<String>): String {
        require(imageUris.isNotEmpty())
        requireNotNull(dao.getTripWithCurrencies(tripId)) { "Trip no longer exists" }
        val batchId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.replaceVisibleBatchImports(
            tripId,
            imageUris.distinct().mapIndexed { index, imageUri ->
                BatchReceiptImportEntity(
                    id = UUID.randomUUID().toString(),
                    batchId = batchId,
                    tripId = tripId,
                    sortPosition = index,
                    imageUri = imageUri,
                    status = BatchReceiptImportStatus.QUEUED,
                    createdAt = now,
                    updatedAt = now,
                )
            },
        )
        return batchId
    }

    suspend fun nextPendingBatchReceiptImport(): BatchReceiptImportEntity? =
        dao.nextPendingBatchImport()

    suspend fun updateBatchReceiptImport(item: BatchReceiptImportEntity) =
        dao.updateBatchImport(item)

    suspend fun retryBatchReceiptImport(itemId: String) =
        dao.retryBatchImport(itemId, System.currentTimeMillis())

    suspend fun cancelBatchReceiptImports(batchId: String) =
        dao.cancelPendingBatchImports(batchId, System.currentTimeMillis())

    suspend fun dismissBatchReceiptImports(tripId: String) =
        dao.dismissVisibleBatchImports(tripId)

    suspend fun tripWithCurrencies(tripId: String): TripWithCurrencies? =
        dao.getTripWithCurrencies(tripId)

    suspend fun receiptsForTrip(tripId: String): List<ReceiptWithItems> =
        dao.getReceiptsWithItems(tripId)

    suspend fun receipt(receiptId: String): ReceiptEntity? = dao.getReceipt(receiptId)

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
        val currencyCode = requireSupportedCurrency(input.currencyCode)
        dao.insertStatementLine(
            StatementLineEntity(
                id = UUID.randomUUID().toString(),
                reconciliationId = reconciliationId,
                occurredOn = input.occurredOn,
                description = input.description.trim(),
                checkNumber = input.checkNumber.trim(),
                amountMinor = input.amountMinor,
                currencyCode = currencyCode,
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
        val currencyCode = requireSupportedCurrency(input.currencyCode)
        dao.deleteStatementLineMatch(existing.id)
        dao.updateStatementLine(
            existing.copy(
                occurredOn = input.occurredOn,
                description = input.description.trim(),
                checkNumber = input.checkNumber.trim(),
                amountMinor = input.amountMinor,
                currencyCode = currencyCode,
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
            availableReceipts(tripId, line.reconciliationId)
                .filterNot { it.id in usedReceiptIds },
        )
    }

    suspend fun assignReceipt(line: StatementLineEntity, receipt: ReceiptEntity, manually: Boolean) {
        val reconciliation = checkNotNull(dao.getReconciliation(line.reconciliationId))
        require(receipt.tripId == reconciliation.reconciliation.tripId) {
            "Receipt and reconciliation belong to different trips"
        }
        require(
            receipt.id !in dao.getReceiptIdsMatchedInOtherReconciliations(
                reconciliation.reconciliation.tripId,
                line.reconciliationId,
            ),
        ) { "Receipt is already assigned to another reconciliation" }
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
        val available = availableReceipts(tripId, reconciliation.reconciliation.id).toMutableList()
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
        val report = buildVerifiedReport(tripId, reconciliation.reconciliation.id)
        // analysisUpdatedAt is the durable marker that the local reconciliation has run.
        // The optional AI wording may be stored afterwards, but it must not determine whether
        // the UI considers the deterministic reconciliation complete.
        dao.updateReconciliationAnalysis(
            reconciliation.reconciliation.id,
            summary = null,
            updatedAt = System.currentTimeMillis(),
        )
        return report
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
        val fallbackCurrency = requireSupportedCurrency(fallbackCurrencyCode)
        val validated = StatementExtractionValidator.validate(extracted, fallbackCurrency)
        val lines = validated.lines.map { line ->
            StatementLineEntity(
                id = UUID.randomUUID().toString(),
                reconciliationId = reconciliation.id,
                occurredOn = line.occurredOn,
                description = line.description,
                checkNumber = line.checkNumber,
                amountMinor = line.amountMinor,
                currencyCode = requireSupportedCurrency(line.currencyCode),
                status = ReconciliationStatus.NOT_FOUND,
                acceptedWithoutReceipt = false,
                sourceDateText = line.sourceDateText,
                dateAmbiguous = line.dateAmbiguous,
            )
        }
        dao.applyExtractedStatement(
            reconciliation = reconciliation.copy(
                title = reconciliation.title,
                analysisSummary = null,
                analysisUpdatedAt = null,
                declaredTotalMinor = validated.declaredTotalMinor,
                declaredTotalCurrencyCode = validated.declaredTotalCurrencyCode
                    ?.let(::requireSupportedCurrency),
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
        val receipts = availableReceipts(tripId, reconciliationId)
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
                receiptAmountMinor = receipt?.amountMinor,
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
                receiptAmountMinor = receipt.amountMinor,
                currencyCode = receipt.currencyCode,
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

    private suspend fun availableReceipts(
        tripId: String,
        reconciliationId: String,
    ): List<ReceiptEntity> {
        val unavailableReceiptIds = dao.getReceiptIdsMatchedInOtherReconciliations(
            tripId,
            reconciliationId,
        ).toHashSet()
        return dao.getReceipts(tripId).filterNot { it.id in unavailableReceiptIds }
    }

    private data class AutomaticProposal(
        val line: StatementLineEntity,
        val receipt: ReceiptEntity,
        val exactCheck: Boolean,
        val score: Int,
        val margin: Int,
    )

    private suspend fun requireTripCurrency(trip: TripEntity, currencyCode: String): String {
        val normalized = requireSupportedCurrency(currencyCode)
        require(dao.getTripCurrencies(trip.id).any { it.currencyCode == normalized }) {
            "Currency $normalized is not configured for this trip"
        }
        return normalized
    }

    private fun requireRateSnapshot(
        homeCurrencyCode: String,
        currencyCode: String,
        rate: String,
    ): String {
        val decimal = rate.trim().toBigDecimalOrNull()
        require(decimal?.signum() == 1) { "Exchange rate must be positive" }
        if (currencyCode == homeCurrencyCode) {
            require(decimal.compareTo(java.math.BigDecimal.ONE) == 0) {
                "Home currency exchange rate must be 1"
            }
        }
        return decimal.stripTrailingZeros().toPlainString()
    }

    private fun normalizedTripCurrencies(
        tripId: String,
        homeCurrencyCode: String,
        currencies: List<TripCurrencyInput>,
    ): List<TripCurrencyEntity> = currencies.map { input ->
        val code = requireSupportedCurrency(input.currencyCode)
        TripCurrencyEntity(
            tripId = tripId,
            currencyCode = code,
            homeToCurrencyRate = requireRateSnapshot(
                homeCurrencyCode,
                code,
                input.homeToCurrencyRate,
            ),
            exchangeRateMode = if (code == homeCurrencyCode) {
                "FIXED"
            } else {
                input.exchangeRateMode.trim().uppercase(Locale.ROOT)
            },
            isDefault = input.isDefault,
        )
    }.also { de.shakie.billcheck.domain.TripCurrencyRules.requireValid(homeCurrencyCode, it) }

    private fun requireSupportedCurrency(currencyCode: String): String {
        val normalized = CurrencyAmount.normalizeCode(currencyCode)
        require(normalized in SUPPORTED_CURRENCY_CODES) {
            "Unsupported currency: $normalized"
        }
        return normalized
    }

    private companion object {
        val SUPPORTED_CURRENCY_CODES: Set<String> by lazy {
            CurrencyCatalog.entries(Locale.ROOT).mapTo(hashSetOf()) { it.code }
        }
    }
}
