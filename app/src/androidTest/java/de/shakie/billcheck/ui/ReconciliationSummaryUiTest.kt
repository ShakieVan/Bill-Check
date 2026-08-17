package de.shakie.billcheck.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import de.shakie.billcheck.R
import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.ReceiptMatchEntity
import de.shakie.billcheck.data.ReceiptWithItems
import de.shakie.billcheck.data.ReconciliationEntity
import de.shakie.billcheck.data.ReconciliationWithLines
import de.shakie.billcheck.data.StatementLineEntity
import de.shakie.billcheck.data.StatementLineWithMatches
import de.shakie.billcheck.domain.ReconciliationStatus
import de.shakie.billcheck.domain.ReconciliationMatcher
import org.junit.Rule
import org.junit.Test

class ReconciliationSummaryUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun summaryUsesMetricTilesWithoutContradictingRecognizedStatementTotal() {
        val receipt = receipt()
        val line = StatementLineEntity(
            id = "line",
            reconciliationId = "reconciliation",
            occurredOn = 0,
            description = "Sultana Restaurant Food",
            checkNumber = "0015512",
            amountMinor = 31_332,
            currencyCode = "EGP",
            status = ReconciliationStatus.CORRECT,
            acceptedWithoutReceipt = false,
        )
        val reconciliation = ReconciliationWithLines(
            ReconciliationEntity(
                id = "reconciliation",
                tripId = "trip",
                title = "Endrechnung",
                statementImageUri = null,
                createdAt = 0,
            ),
            listOf(StatementLineWithMatches(line, listOf(ReceiptMatchEntity(line.id, receipt.id, false)))),
        )

        compose.setContent {
            ReconciliationManagerDialog(
                initialSelectedId = reconciliation.reconciliation.id,
                reconciliations = listOf(reconciliation),
                receipts = listOf(ReceiptWithItems(receipt, emptyList())),
                defaultCurrencyCode = "EGP",
                candidateSelection = CandidateSelectionState(),
                analysisState = ReconciliationAnalysisState.Idle,
                onDismiss = {}, onCreate = {}, onUpdateHeader = { _, _, _ -> },
                onAddLine = { _, _, _, _, _, _ -> true },
                onUpdateLine = { _, _, _, _, _, _ -> true },
                onDeleteLine = {}, onAcceptLine = { _, _ -> }, onLoadCandidates = {},
                onClearCandidates = {}, onAssignReceipt = { _, _ -> }, onClearLineMatch = {},
                onRun = {}, onReset = {}, onDelete = {}, onOpenImage = {}, onChooseImage = {},
                onAnalyzeImage = {},
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.summary_statement_total)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.summary_matched_receipt_total)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.summary_unmatched_receipts)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.summary_unmatched_statement_lines)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.narrative_all)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.summary_control_total_unavailable)).assertDoesNotExist()
        val score = ReconciliationMatcher.rank(line, listOf(receipt)).single().score
        compose.onNodeWithText(context.getString(R.string.match_score, score))
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun receipt() = ReceiptEntity(
        id = "receipt",
        tripId = "trip",
        occurredAt = 0,
        location = "Sultana Restaurant",
        checkNumber = "5512",
        foreignAmountMinor = 31_332,
        foreignCurrencyCode = "EGP",
        exchangeRate = "55.5",
        exactEuroCents = 0,
        tipMinor = 0,
        tipCurrencyCode = "EUR",
        imageUri = null,
        reviewState = "CONFIRMED",
        createdAt = 0,
    )
}
