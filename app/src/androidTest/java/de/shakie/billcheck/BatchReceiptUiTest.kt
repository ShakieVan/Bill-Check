package de.shakie.billcheck

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import de.shakie.billcheck.data.BatchReceiptImportEntity
import de.shakie.billcheck.data.BatchReceiptImportStatus
import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.ReceiptReviewState
import de.shakie.billcheck.data.ReceiptWithItems
import de.shakie.billcheck.ui.theme.BillCheckTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BatchReceiptUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun imageActionsKeepSingleAndMultipleImagesTogetherAndManualBelow() {
        compose.setContent {
            BillCheckTheme(darkTheme = false) {
                ReceiptActions({}, {}, {}, {}, {})
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = compose.onNodeWithText(context.getString(R.string.choose_image))
            .fetchSemanticsNode().boundsInRoot
        val multiple = compose.onNodeWithText(context.getString(R.string.choose_multiple_images))
            .fetchSemanticsNode().boundsInRoot
        val manual = compose.onNodeWithText(context.getString(R.string.manual_entry))
            .fetchSemanticsNode().boundsInRoot

        assertEquals(image.top, multiple.top)
        assertTrue(manual.top >= image.bottom)
    }

    @Test
    fun failedBatchImageCanBeRetriedIndividually() {
        var retried: String? = null
        val item = batchItem(BatchReceiptImportStatus.FAILED, "Server nicht erreichbar")
        compose.setContent {
            BillCheckTheme(darkTheme = false) {
                BatchReceiptImportCard(
                    items = listOf(item),
                    onRetry = { retried = it },
                    onCancel = {},
                    onDismiss = {},
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.batch_processing)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.retry)).performClick()

        assertEquals(item.id, retried)
    }

    @Test
    fun automaticallyCreatedSuspiciousReceiptIsVisiblyMarked() {
        val receipt = receipt().copy(
            reviewState = ReceiptReviewState.required(listOf("CURRENCY_NOT_AVAILABLE=USD")),
        )
        compose.setContent {
            BillCheckTheme(darkTheme = false) {
                ReceiptCard(
                    receiptWithItems = ReceiptWithItems(receipt, emptyList()),
                    homeCurrencyCode = "EUR",
                    onDelete = {},
                    onOpenImage = {},
                    onEdit = {},
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(
            context.getString(R.string.receipt_currency_needs_review, "USD"),
        ).assertIsDisplayed()
    }

    private fun batchItem(status: String, message: String?) = BatchReceiptImportEntity(
        id = "item",
        batchId = "batch",
        tripId = "trip",
        sortPosition = 0,
        imageUri = "content://image",
        status = status,
        message = message,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun receipt() = ReceiptEntity(
        id = "receipt",
        tripId = "trip",
        occurredAt = 0,
        location = "Sultana",
        checkNumber = "5595",
        amountMinor = 15_595,
        currencyCode = "EGP",
        exchangeRateSnapshot = "55.5",
        exactHomeMinor = 281,
        tipMinor = 0,
        tipCurrencyCode = "EUR",
        tipExchangeRateSnapshot = "1",
        imageUri = null,
        reviewState = ReceiptReviewState.CONFIRMED,
        createdAt = 0,
    )
}
