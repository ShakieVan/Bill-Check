package de.shakie.billcheck

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.ReceiptWithItems
import de.shakie.billcheck.data.TripCurrencyEntity
import de.shakie.billcheck.data.TripEntity
import de.shakie.billcheck.domain.ExtractedItem
import de.shakie.billcheck.domain.ExtractedReceipt
import de.shakie.billcheck.ui.AiExtractionState
import de.shakie.billcheck.ui.ExchangeRateLookupState
import de.shakie.billcheck.ui.LocalOcrState
import de.shakie.billcheck.ui.theme.BillCheckTheme
import org.junit.Rule
import org.junit.Test

class ReceiptAiReviewUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun existingReceiptShowsAllDetectedValuesAndFixedReviewActions() {
        val imageUri = "android.resource://de.shakie.billcheck.debug/${R.drawable.ic_launcher_foreground}"
        val extracted = ExtractedReceipt(
            location = "Sultana Rest.",
            checkNumber = "5595",
            totalAmountText = "155.95",
            currencyCode = "EGP",
            occurredOn = "2024-12-28",
            items = listOf(
                ExtractedItem(name = "Cola - Can 330", amountText = "125.00", quantityText = "5"),
                ExtractedItem(name = "Service", amountText = "30.95"),
            ),
        )
        compose.setContent {
            BillCheckTheme(darkTheme = false) {
                ReceiptEditorDialog(
                    trip = trip(),
                    tripCurrencies = listOf(tripCurrency()),
                    recentCurrencyCodes = emptyList(),
                    exchangeRateLookup = ExchangeRateLookupState.Idle,
                    onLookupRate = { _, _ -> },
                    existing = ReceiptWithItems(receipt(imageUri), emptyList()),
                    imageUri = imageUri,
                    aiExtraction = AiExtractionState.ReceiptSuccess(imageUri, extracted),
                    localOcr = LocalOcrState.Idle,
                    onTakePhoto = {},
                    onChooseImage = {},
                    onBrowseFolders = {},
                    onOpenImage = {},
                    onAnalyzeImage = {},
                    onAnalyzeLocally = {},
                    onClearLocalOcr = {},
                    onAddTripCurrency = { _, _, _ -> true },
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _ -> true },
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.ai_analysis_complete)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.apply_detected_values)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.keep_my_entries)).assertIsDisplayed()
        compose.onNodeWithText("Sultana Rest.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("5595").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("28.12.2024").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("155.95").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("5 × Cola - Can 330").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Service").performScrollTo().assertIsDisplayed()
    }

    private fun trip() = TripEntity(
        id = "trip",
        sortPosition = 0,
        name = "Trip",
        homeCurrencyCode = "EUR",
        defaultTipMinor = 0,
        defaultTipCurrencyCode = "EUR",
        defaultTipSelected = false,
        imageStorageMode = "ORIGINAL",
        createdAt = 0,
    )

    private fun tripCurrency() = TripCurrencyEntity(
        tripId = "trip",
        currencyCode = "EGP",
        homeToCurrencyRate = "55.5",
        exchangeRateMode = "FIXED",
        isDefault = true,
    )

    private fun receipt(imageUri: String) = ReceiptEntity(
        id = "receipt",
        tripId = "trip",
        occurredAt = 0,
        location = "Beach Club",
        checkNumber = "15595",
        amountMinor = 10_000,
        currencyCode = "EGP",
        exchangeRateSnapshot = "55.5",
        exactHomeMinor = 0,
        tipMinor = 0,
        tipCurrencyCode = "EUR",
        tipExchangeRateSnapshot = "1",
        imageUri = imageUri,
        reviewState = "CONFIRMED",
        createdAt = 0,
    )
}
