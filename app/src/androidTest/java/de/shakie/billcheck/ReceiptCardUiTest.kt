package de.shakie.billcheck

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.platform.app.InstrumentationRegistry
import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.ReceiptItemEntity
import de.shakie.billcheck.data.ReceiptReviewState
import de.shakie.billcheck.data.ReceiptWithItems
import de.shakie.billcheck.ui.theme.BillCheckTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReceiptCardUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun headerPriceAndItemsUseSeparateVerticalSections() {
        compose.setContent {
            BillCheckTheme(darkTheme = false) {
                ReceiptCard(receiptWithItems(), "EUR", {}, {}, {})
            }
        }

        val header = compose.onNodeWithText(
            "Sehr langes Restaurant mit Terrasse",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val roundedPrice = compose.onNodeWithText("3 EUR", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val firstItem = compose.onNodeWithText(
            "1 × Erster langer Posten",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot

        assertTrue(roundedPrice.top >= header.bottom)
        assertTrue(firstItem.top >= roundedPrice.bottom)
    }

    @Test
    fun exactPriceIsHiddenUntilPriceSummaryIsExpanded() {
        var editRequested = false
        compose.setContent {
            BillCheckTheme(darkTheme = false) {
                ReceiptCard(receiptWithItems(), "EUR", {}, {}, { editRequested = true })
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        compose.onNodeWithText(context.getString(R.string.exact_total)).assertDoesNotExist()
        compose.onNodeWithTag("receipt-price-summary-receipt").performClick()
        compose.onNodeWithText(context.getString(R.string.exact_total)).assertIsDisplayed()
        assertTrue(!editRequested)
    }

    @Test
    fun twoItemsStayVisibleAndAdditionalItemsExpandExplicitly() {
        compose.setContent {
            BillCheckTheme(darkTheme = false) {
                ReceiptCard(receiptWithItems(), "EUR", {}, {}, {})
            }
        }

        compose.onNodeWithText("1 × Erster langer Posten").assertIsDisplayed()
        compose.onNodeWithText("2 × Zweiter langer Posten").assertIsDisplayed()
        compose.onNodeWithText("3 × Dritter langer Posten").assertDoesNotExist()
        compose.onNodeWithTag("receipt-items-toggle-receipt").performClick()
        compose.onNodeWithText("3 × Dritter langer Posten").assertIsDisplayed()
    }

    @Test
    fun swipeOnlyRevealsDeleteAndExplicitTapRequestsDeletion() {
        var revealed by mutableStateOf(false)
        var deletionRequested = false
        compose.setContent {
            BillCheckTheme(darkTheme = false) {
                ReceiptCard(
                    receiptWithItems = receiptWithItems(),
                    homeCurrencyCode = "EUR",
                    onDelete = { deletionRequested = true },
                    onOpenImage = {},
                    onEdit = {},
                    deleteRevealed = revealed,
                    onDeleteRevealChange = { revealed = it },
                )
            }
        }

        compose.onNodeWithTag("receipt-card-receipt").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertTrue(revealed)
        assertTrue(!deletionRequested)

        compose.onNodeWithTag("receipt-delete-action-receipt").performClick()
        assertTrue(deletionRequested)
    }

    private fun receiptWithItems() = ReceiptWithItems(
        receipt = ReceiptEntity(
            id = "receipt",
            tripId = "trip",
            occurredAt = 0,
            location = "Sehr langes Restaurant mit Terrasse",
            checkNumber = "426",
            amountMinor = 15_595,
            currencyCode = "EGP",
            exchangeRateSnapshot = "55.5",
            exactHomeMinor = 281,
            tipMinor = 100,
            tipCurrencyCode = "EUR",
            tipExchangeRateSnapshot = "1",
            imageUri = null,
            reviewState = ReceiptReviewState.CONFIRMED,
            createdAt = 0,
        ),
        items = listOf(
            item(0, "1 × Erster langer Posten", 5_000),
            item(1, "2 × Zweiter langer Posten", 5_100),
            item(2, "3 × Dritter langer Posten", 5_495),
        ),
    )

    private fun item(position: Int, name: String, amountMinor: Long) = ReceiptItemEntity(
        id = "item-$position",
        receiptId = "receipt",
        sortPosition = position,
        name = name,
        amountMinor = amountMinor,
        currencyCode = "EGP",
    )
}
