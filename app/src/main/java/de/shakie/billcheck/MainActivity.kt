package de.shakie.billcheck

import android.os.Bundle
import android.net.Uri
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.ReceiptImageStorage
import de.shakie.billcheck.data.ReceiptWithItems
import de.shakie.billcheck.data.TripEntity
import de.shakie.billcheck.data.TripCurrencyEntity
import de.shakie.billcheck.data.TripCurrencyInput
import de.shakie.billcheck.data.HybridOcrPageBuilder
import de.shakie.billcheck.domain.MoneyCalculator
import de.shakie.billcheck.domain.AiSuggestionCertainty
import de.shakie.billcheck.domain.ExtractedItem
import de.shakie.billcheck.domain.ExtractedFieldSuggestions
import de.shakie.billcheck.domain.ExtractedReceipt
import de.shakie.billcheck.domain.SuggestionSource
import de.shakie.billcheck.ui.MainUiState
import de.shakie.billcheck.ui.MainViewModel
import de.shakie.billcheck.ui.OpenImageDocumentContract
import de.shakie.billcheck.ui.ReceiptItemDraft
import de.shakie.billcheck.ui.ReceiptImageReview
import de.shakie.billcheck.ui.ReceiptThumbnail
import de.shakie.billcheck.ui.FullscreenReceiptImage
import de.shakie.billcheck.ui.SpatialTextSelectionDialog
import de.shakie.billcheck.ui.ExchangeRateLookupState
import de.shakie.billcheck.ui.AiExtractionState
import de.shakie.billcheck.ui.LocalOcrState
import de.shakie.billcheck.ui.GeminiModelsState
import de.shakie.billcheck.ui.LocalAiConnectionState
import de.shakie.billcheck.data.LocalAiAuthType
import de.shakie.billcheck.data.AI_PROVIDER_GEMINI
import de.shakie.billcheck.data.AI_PROVIDER_LOCAL
import de.shakie.billcheck.ui.TransferState
import de.shakie.billcheck.ui.AppUpdateStatus
import de.shakie.billcheck.ui.UpdateManagerDialog
import de.shakie.billcheck.ui.ReconciliationManagerDialog
import de.shakie.billcheck.ui.ReconciliationAnalysisState
import de.shakie.billcheck.ui.CurrencyPickerDialog
import de.shakie.billcheck.ui.EditableExchangeRateMode
import de.shakie.billcheck.ui.EditableTripCurrency
import de.shakie.billcheck.ui.TripCurrencyEditorSection
import de.shakie.billcheck.ui.TripCurrencySelector
import de.shakie.billcheck.domain.CurrencyAmount
import de.shakie.billcheck.domain.CurrencyCatalog
import de.shakie.billcheck.data.ExportFormat
import de.shakie.billcheck.data.ImportPreview
import de.shakie.billcheck.ui.theme.BillCheckTheme
import androidx.compose.foundation.isSystemInDarkTheme
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val widgetAction = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetAction.value = intent.action.takeIf(::isWidgetAction)
        enableEdgeToEdge()
        setContent {
            val requestedWidgetAction by widgetAction.collectAsStateWithLifecycle()
            val preferences = remember {
                getSharedPreferences("bill_check_preferences", MODE_PRIVATE)
            }
            val initialSystemDarkTheme = isSystemInDarkTheme()
            var darkTheme by rememberSaveable {
                val savedDarkTheme = preferences.getBoolean("dark_theme", initialSystemDarkTheme)
                if (!preferences.contains("dark_theme")) {
                    preferences.edit().putBoolean("dark_theme", savedDarkTheme).apply()
                }
                mutableStateOf(savedDarkTheme)
            }
            BillCheckTheme(darkTheme = darkTheme) {
                BillCheckApp(
                    darkTheme = darkTheme,
                    widgetAction = requestedWidgetAction,
                    onWidgetActionConsumed = { widgetAction.value = null },
                    onDarkThemeChange = { useDarkTheme ->
                        darkTheme = useDarkTheme
                        preferences.edit().putBoolean("dark_theme", useDarkTheme).apply()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetAction.value = intent.action.takeIf(::isWidgetAction)
    }

    private fun isWidgetAction(action: String?): Boolean = action in setOf(
        BillCheckWidget.ACTION_OPEN,
        BillCheckWidget.ACTION_PHOTO,
        BillCheckWidget.ACTION_IMAGE,
        BillCheckWidget.ACTION_MANUAL,
        BillCheckWidget.ACTION_STATEMENT,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillCheckApp(
    darkTheme: Boolean,
    widgetAction: String?,
    onWidgetActionConsumed: () -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exchangeRateLookup by viewModel.exchangeRateLookup.collectAsStateWithLifecycle()
    val candidateSelection by viewModel.candidateSelection.collectAsStateWithLifecycle()
    val aiSettings by viewModel.aiSettings.collectAsStateWithLifecycle()
    val aiExtraction by viewModel.aiExtraction.collectAsStateWithLifecycle()
    val reconciliationAnalysis by viewModel.reconciliationAnalysis.collectAsStateWithLifecycle()
    val localOcr by viewModel.localOcr.collectAsStateWithLifecycle()
    val geminiModels by viewModel.geminiModels.collectAsStateWithLifecycle()
    val localAiSettings by viewModel.localAiSettings.collectAsStateWithLifecycle()
    val localAiConnection by viewModel.localAiConnection.collectAsStateWithLifecycle()
    val transferState by viewModel.transfer.collectAsStateWithLifecycle()
    val appUpdate by viewModel.appUpdate.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val imageStorage = remember { ReceiptImageStorage(context) }
    var showCreateTrip by remember { mutableStateOf(false) }
    var editingTrip by remember { mutableStateOf<TripEntity?>(null) }
    var deletingTrip by remember { mutableStateOf<TripEntity?>(null) }
    var showManualReceipt by remember { mutableStateOf(false) }
    var editingReceipt by remember { mutableStateOf<ReceiptWithItems?>(null) }
    var showAppMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showUpdates by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showReconciliations by remember { mutableStateOf(false) }
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var draftReceiptImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var fullscreenImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var imageTargetReceiptId by rememberSaveable { mutableStateOf<String?>(null) }
    var imageTargetHadLinkedImage by rememberSaveable { mutableStateOf(false) }
    var imageTargetReconciliationId by rememberSaveable { mutableStateOf<String?>(null) }
    var reconciliationReturnId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExportTripIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val pendingCameraUri = pendingCameraUriString?.let(Uri::parse)
    val pendingImageUri = pendingImageUriString?.let(Uri::parse)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val cameraError = stringResource(R.string.camera_start_failed)
    val updateAvailableMessage = stringResource(R.string.update_available)
    val updatesLabel = stringResource(R.string.updates)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.checkForAppUpdate(force = false)
    }

    LaunchedEffect(appUpdate.status, appUpdate.release?.tagName) {
        if (appUpdate.status == AppUpdateStatus.AVAILABLE) {
            if (snackbar.showSnackbar(updateAvailableMessage, updatesLabel) == SnackbarResult.ActionPerformed) {
                showUpdates = true
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        pendingCameraUri?.let { uri ->
            if (saved || imageStorage.hasImageData(uri)) {
                runCatching { imageStorage.publishCameraImage(uri) }
                pendingImageUriString = uri.toString()
            } else {
                runCatching { imageStorage.discardFailedCameraImage(uri) }
            }
        }
        pendingCameraUriString = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            imageStorage.persistPickedImageAccess(uri)
            pendingImageUriString = uri.toString()
        } else if (imageTargetReconciliationId != null) {
            showReconciliations = true
        }
    }
    val documentLauncher = rememberLauncherForActivityResult(OpenImageDocumentContract()) { uri ->
        if (uri != null) {
            imageStorage.persistPickedImageAccess(uri)
            pendingImageUriString = uri.toString()
        } else if (imageTargetReconciliationId != null) {
            showReconciliations = true
        }
    }
    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) viewModel.exportData(uri, pendingExportTripIds, ExportFormat.BILL_CHECK)
    }
    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) viewModel.exportData(uri, pendingExportTripIds, ExportFormat.CSV)
    }
    val pdfExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) viewModel.exportData(uri, pendingExportTripIds, ExportFormat.PDF)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.previewImport(uri)
        }
    }
    val takePhoto = {
        runCatching { imageStorage.createCameraImage() }
            .onSuccess { uri ->
                pendingCameraUriString = uri.toString()
                cameraLauncher.launch(uri)
            }
            .onFailure { scope.launch { snackbar.showSnackbar(cameraError) } }
        Unit
    }
    val chooseImage = {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }
    val browseFolders = {
        documentLauncher.launch(OpenImageDocumentContract.BILL_CHECK_FOLDER)
    }

    LaunchedEffect(widgetAction) {
        when (widgetAction) {
            BillCheckWidget.ACTION_PHOTO -> {
                imageTargetReceiptId = null
                imageTargetHadLinkedImage = false
                takePhoto()
            }
            BillCheckWidget.ACTION_IMAGE -> {
                imageTargetReceiptId = null
                imageTargetHadLinkedImage = false
                chooseImage()
            }
            BillCheckWidget.ACTION_MANUAL -> showManualReceipt = true
            BillCheckWidget.ACTION_STATEMENT -> showReconciliations = true
        }
        if (widgetAction != null) onWidgetActionConsumed()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Bill Check", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                Text(
                    stringResource(R.string.trips),
                    modifier = Modifier.padding(24.dp, 18.dp, 24.dp, 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.trips.forEach { trip ->
                    TripDrawerItem(
                        trip = trip,
                        selected = state.selectedTrip?.id == trip.id,
                        onSelect = {
                            viewModel.selectTrip(trip.id)
                            scope.launch { drawerState.close() }
                        },
                        onEdit = {
                            viewModel.selectTrip(trip.id)
                            editingTrip = trip
                            scope.launch { drawerState.close() }
                        },
                        onMove = { positions -> viewModel.moveTrip(trip.id, positions) },
                    )
                }
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.new_trip)) },
                    selected = false,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        showCreateTrip = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.open_trip_menu))
                        }
                    },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Bill Check", fontWeight = FontWeight.Bold)
                    }
                },
                    actions = {
                        Box {
                            IconButton(onClick = { showAppMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.open_app_menu))
                            }
                            DropdownMenu(
                                expanded = showAppMenu,
                                onDismissRequest = { showAppMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.export_data)) },
                                    onClick = {
                                        showAppMenu = false
                                        showExport = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.import_data)) },
                                    onClick = {
                                        showAppMenu = false
                                        importLauncher.launch(
                                            arrayOf("application/zip", "application/octet-stream", "text/csv", "text/plain"),
                                        )
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings)) },
                                    onClick = {
                                        showAppMenu = false
                                        showSettings = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.updates)) },
                                    onClick = {
                                        showAppMenu = false
                                        showUpdates = true
                                        viewModel.checkForAppUpdate(force = true)
                                    },
                                )
                            }
                        }
                    },
                )
            },
        ) { padding ->
            if (pendingImageUri != null) {
                ReceiptImageReview(
                imageUri = requireNotNull(pendingImageUri),
                modifier = Modifier.padding(padding),
                onTakeAnother = takePhoto,
                onChooseAnother = chooseImage,
                onBrowseFolders = browseFolders,
                onUseImage = {
                    when {
                        imageTargetReceiptId != null -> viewModel.updateReceiptImage(
                            requireNotNull(imageTargetReceiptId),
                            requireNotNull(pendingImageUriString),
                        )
                        imageTargetReconciliationId != null -> {
                            state.reconciliations.firstOrNull {
                                it.reconciliation.id == imageTargetReconciliationId
                            }?.reconciliation?.let {
                                viewModel.updateReconciliation(
                                    it.id,
                                    it.title,
                                    requireNotNull(pendingImageUriString),
                                )
                            }
                            showReconciliations = true
                        }
                        else -> {
                            draftReceiptImageUriString = pendingImageUriString
                            showManualReceipt = true
                        }
                    }
                    pendingImageUriString = null
                    imageTargetReceiptId = null
                    imageTargetReconciliationId = null
                    imageTargetHadLinkedImage = false
                },
                onClose = {
                    pendingImageUriString = null
                    imageTargetReceiptId = null
                    imageTargetReconciliationId = null
                    imageTargetHadLinkedImage = false
                    if (reconciliationReturnId != null) showReconciliations = true
                },
                onUnlink = when {
                    imageTargetReceiptId != null && imageTargetHadLinkedImage -> {
                        {
                            viewModel.updateReceiptImage(requireNotNull(imageTargetReceiptId), null)
                            pendingImageUriString = null
                            imageTargetReceiptId = null
                            imageTargetHadLinkedImage = false
                        }
                    }
                    imageTargetReconciliationId != null && imageTargetHadLinkedImage -> {
                        {
                            state.reconciliations.firstOrNull {
                                it.reconciliation.id == imageTargetReconciliationId
                            }?.reconciliation?.let {
                                viewModel.updateReconciliation(it.id, it.title, null)
                            }
                            pendingImageUriString = null
                            imageTargetReconciliationId = null
                            imageTargetHadLinkedImage = false
                            showReconciliations = true
                        }
                    }
                    pendingImageUriString == draftReceiptImageUriString -> {
                        {
                            draftReceiptImageUriString = null
                            pendingImageUriString = null
                        }
                    }
                    else -> null
                },
                )
            } else if (state.trips.isEmpty()) {
                EmptyTrips(
                modifier = Modifier.padding(padding),
                onCreate = { showCreateTrip = true },
                )
            } else {
                Dashboard(
                state = state,
                modifier = Modifier.padding(padding),
                onManualReceipt = { showManualReceipt = true },
                onCamera = {
                    imageTargetReceiptId = null
                    imageTargetHadLinkedImage = false
                    takePhoto()
                },
                onGallery = {
                    imageTargetReceiptId = null
                    imageTargetHadLinkedImage = false
                    chooseImage()
                },
                onBrowseFolders = {
                    imageTargetReceiptId = null
                    imageTargetHadLinkedImage = false
                    browseFolders()
                },
                onOpenReceiptImage = { fullscreenImageUriString = it },
                onEditReceipt = { editingReceipt = it },
                onDeleteReceipt = viewModel::deleteReceipt,
                onOpenReconciliations = { showReconciliations = true },
                )
            }
        }
    }

    if (showCreateTrip) {
        TripEditorDialog(
            suggestedName = stringResource(R.string.trip_default_name),
            homeCurrencyCode = state.defaultHomeCurrencyCode,
            recentCurrencyCodes = state.recentCurrencyCodes,
            exchangeRateLookup = exchangeRateLookup,
            onLookupRate = viewModel::requestExchangeRate,
            onDismiss = {
                showCreateTrip = false
                viewModel.clearExchangeRateLookup()
            },
            onSave = { name, currencies, tipMinor, tipCurrency, tipSelected ->
                viewModel.createTrip(
                    name = name,
                    homeCurrencyCode = state.defaultHomeCurrencyCode,
                    currencies = currencies,
                    defaultTipMinor = tipMinor,
                    defaultTipCurrencyCode = tipCurrency,
                    defaultTipSelected = tipSelected,
                ).also { saved ->
                    if (saved) {
                        showCreateTrip = false
                        viewModel.clearExchangeRateLookup()
                    }
                }
            },
        )
    }

    editingTrip?.takeIf { trip ->
        state.selectedTrip?.id == trip.id &&
            state.tripCurrencies.isNotEmpty() &&
            state.tripCurrencies.all { it.tripId == trip.id } &&
            state.receipts.all { it.receipt.tripId == trip.id } &&
            state.reconciliations.all { it.reconciliation.tripId == trip.id }
    }?.let { trip ->
        TripEditorDialog(
            existing = trip,
            existingCurrencies = state.tripCurrencies,
            homeCurrencyCode = trip.homeCurrencyCode,
            recentCurrencyCodes = state.recentCurrencyCodes,
            usedCurrencyCodes = state.receipts.flatMap {
                if (it.receipt.tipMinor > 0) {
                    listOf(it.receipt.currencyCode, it.receipt.tipCurrencyCode)
                } else {
                    listOf(it.receipt.currencyCode)
                }
            }.toSet(),
            suggestedName = stringResource(R.string.trip_default_name),
            exchangeRateLookup = exchangeRateLookup,
            onLookupRate = viewModel::requestExchangeRate,
            onDismiss = {
                editingTrip = null
                viewModel.clearExchangeRateLookup()
            },
            onDeleteRequested = { deletingTrip = trip },
            onSave = { name, currencies, tipMinor, tipCurrency, tipSelected ->
                viewModel.updateTrip(
                    existing = trip,
                    name = name,
                    currencies = currencies,
                    defaultTipMinor = tipMinor,
                    defaultTipCurrencyCode = tipCurrency,
                    defaultTipSelected = tipSelected,
                ).also { saved ->
                    if (saved) {
                        editingTrip = null
                        viewModel.clearExchangeRateLookup()
                    }
                }
            },
        )
    }

    deletingTrip?.let { trip ->
        AlertDialog(
            onDismissRequest = { deletingTrip = null },
            title = { Text(stringResource(R.string.delete_trip)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_trip_confirm,
                        trip.name,
                        state.receipts.size,
                        state.reconciliations.size,
                    ),
                )
            },
            dismissButton = {
                TextButton(onClick = { deletingTrip = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrip(trip)
                        deletingTrip = null
                        editingTrip = null
                        viewModel.clearExchangeRateLookup()
                    },
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    if (showSettings) {
        AppearanceSettingsDialog(
            darkTheme = darkTheme,
            onDarkThemeChange = onDarkThemeChange,
            homeCurrencyCode = state.defaultHomeCurrencyCode,
            recentCurrencyCodes = state.recentCurrencyCodes,
            onHomeCurrencyChange = viewModel::saveHomeCurrency,
            aiSettings = aiSettings,
            geminiModels = geminiModels,
            localAiSettings = localAiSettings,
            localAiConnection = localAiConnection,
            onLoadGeminiModels = viewModel::loadGeminiModels,
            onSaveAiSettings = viewModel::saveAiSettings,
            onClearAiApiKey = viewModel::clearAiApiKey,
            onTestLocalAi = viewModel::testLocalAiConnection,
            onSaveLocalAi = viewModel::saveLocalAiSettings,
            onClearLocalAiCredential = viewModel::clearLocalAiCredential,
            onClearLocalAiConnectionResult = viewModel::clearLocalAiConnectionResult,
            onDismiss = { showSettings = false },
        )
    }

    if (showUpdates) {
        UpdateManagerDialog(
            state = appUpdate,
            onCheck = { viewModel.checkForAppUpdate(force = true) },
            onDownload = viewModel::downloadAppUpdate,
            onCancelDownload = viewModel::cancelAppUpdateDownload,
            onDeleteDownload = viewModel::deleteDownloadedAppUpdate,
            onDismiss = { showUpdates = false },
        )
    }

    if (showExport) {
        ExportDataDialog(
            trips = state.trips,
            onDismiss = { showExport = false },
            onExport = { format, tripIds ->
                showExport = false
                pendingExportTripIds = tripIds
                val date = java.time.LocalDate.now().toString()
                when (format) {
                    ExportFormat.BILL_CHECK -> backupExportLauncher.launch("Bill-Check_$date.billcheck")
                    ExportFormat.CSV -> csvExportLauncher.launch("Bill-Check_$date.csv")
                    ExportFormat.PDF -> pdfExportLauncher.launch("Bill-Check_$date.pdf")
                }
            },
        )
    }

    when (val transfer = transferState) {
        TransferState.Idle -> Unit
        TransferState.Working -> TransferWorkingDialog()
        is TransferState.ImportReady -> ImportTripsDialog(
            preview = transfer.preview,
            onDismiss = viewModel::clearTransferState,
            onImport = viewModel::importSelectedTrips,
        )
        is TransferState.ExportSuccess -> TransferMessageDialog(
            title = stringResource(R.string.export_complete),
            message = stringResource(R.string.export_complete_message),
            onDismiss = viewModel::clearTransferState,
        )
        is TransferState.ImportSuccess -> TransferMessageDialog(
            title = stringResource(R.string.import_complete),
            message = pluralStringResource(
                R.plurals.import_complete_message,
                transfer.tripCount,
                transfer.tripCount,
            ),
            onDismiss = viewModel::clearTransferState,
        )
        is TransferState.Error -> TransferMessageDialog(
            title = stringResource(R.string.transfer_error),
            message = transfer.message.ifBlank { stringResource(R.string.transfer_error_message) },
            onDismiss = viewModel::clearTransferState,
        )
    }

    state.selectedTrip?.let { trip ->
        if (showManualReceipt) {
            ReceiptEditorDialog(
                trip = trip,
                tripCurrencies = state.tripCurrencies,
                recentCurrencyCodes = state.recentCurrencyCodes,
                exchangeRateLookup = exchangeRateLookup,
                onLookupRate = viewModel::requestExchangeRate,
                visible = pendingImageUri == null,
                imageUri = draftReceiptImageUriString,
                locationSuggestions = state.locationSuggestions,
                itemNameSuggestions = state.itemNameSuggestions,
                aiExtraction = aiExtraction,
                localOcr = localOcr,
                onTakePhoto = {
                    imageTargetReceiptId = null
                    imageTargetHadLinkedImage = false
                    takePhoto()
                },
                onChooseImage = {
                    imageTargetReceiptId = null
                    imageTargetHadLinkedImage = false
                    chooseImage()
                },
                onBrowseFolders = {
                    imageTargetReceiptId = null
                    imageTargetHadLinkedImage = false
                    browseFolders()
                },
                onOpenImage = {
                    imageTargetReceiptId = null
                    imageTargetHadLinkedImage = false
                    pendingImageUriString = draftReceiptImageUriString
                },
                onAnalyzeImage = {
                    draftReceiptImageUriString?.let { viewModel.analyzeReceipt(it) }
                },
                onAnalyzeLocally = {
                    draftReceiptImageUriString?.let(viewModel::analyzeLocally)
                },
                onClearLocalOcr = viewModel::clearLocalOcr,
                onAddTripCurrency = viewModel::addCurrencyToSelectedTrip,
                onDismiss = {
                    showManualReceipt = false
                    draftReceiptImageUriString = null
                    viewModel.clearLocalOcr()
                },
                onSave = { location, check, amount, currency, occurredOn, tip, itemDrafts ->
                    viewModel.addReceipt(
                        location,
                        check,
                        amount,
                        currency,
                        occurredOn,
                        tip,
                        itemDrafts,
                        draftReceiptImageUriString,
                    ).also { saved ->
                        if (saved) {
                            showManualReceipt = false
                            pendingImageUriString = null
                            draftReceiptImageUriString = null
                            imageTargetReceiptId = null
                        }
                    }
                },
            )
        }
        editingReceipt?.let { selectedReceipt ->
            val existing = state.receipts.firstOrNull {
                it.receipt.id == selectedReceipt.receipt.id
            } ?: selectedReceipt
            ReceiptEditorDialog(
                trip = trip,
                tripCurrencies = state.tripCurrencies,
                recentCurrencyCodes = state.recentCurrencyCodes,
                exchangeRateLookup = exchangeRateLookup,
                onLookupRate = viewModel::requestExchangeRate,
                existing = existing,
                visible = pendingImageUri == null,
                imageUri = existing.receipt.imageUri,
                locationSuggestions = state.locationSuggestions,
                itemNameSuggestions = state.itemNameSuggestions,
                aiExtraction = aiExtraction,
                localOcr = localOcr,
                onTakePhoto = {
                    imageTargetReceiptId = existing.receipt.id
                    imageTargetHadLinkedImage = existing.receipt.imageUri != null
                    takePhoto()
                },
                onChooseImage = {
                    imageTargetReceiptId = existing.receipt.id
                    imageTargetHadLinkedImage = existing.receipt.imageUri != null
                    chooseImage()
                },
                onBrowseFolders = {
                    imageTargetReceiptId = existing.receipt.id
                    imageTargetHadLinkedImage = existing.receipt.imageUri != null
                    browseFolders()
                },
                onOpenImage = {
                    imageTargetReceiptId = existing.receipt.id
                    imageTargetHadLinkedImage = true
                    pendingImageUriString = existing.receipt.imageUri
                },
                onAnalyzeImage = {
                    existing.receipt.imageUri?.let {
                        viewModel.analyzeReceipt(it, existing.receipt.currencyCode)
                    }
                },
                onAnalyzeLocally = {
                    existing.receipt.imageUri?.let(viewModel::analyzeLocally)
                },
                onClearLocalOcr = viewModel::clearLocalOcr,
                onAddTripCurrency = viewModel::addCurrencyToSelectedTrip,
                onDismiss = {
                    editingReceipt = null
                    viewModel.clearLocalOcr()
                },
                onSave = { location, check, amount, currency, occurredOn, tip, itemDrafts ->
                    viewModel.updateReceipt(
                        existing = existing,
                        location = location,
                        checkNumber = check,
                        amountText = amount,
                        currencyCode = currency,
                        occurredOnText = occurredOn,
                        addDefaultTip = tip,
                        itemDrafts = itemDrafts,
                    ).also { saved ->
                        if (saved) editingReceipt = null
                    }
                },
            )
        }
    }

    when (val extraction = aiExtraction) {
        is AiExtractionState.Loading -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.image_analysis)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.ai_analyzing))
                }
            },
            confirmButton = {},
        )
        AiExtractionState.MissingCredential -> AlertDialog(
            onDismissRequest = viewModel::clearAiExtraction,
            title = { Text(stringResource(R.string.ai_recognition)) },
            text = { Text(stringResource(R.string.ai_missing_key)) },
            dismissButton = {
                TextButton(onClick = viewModel::clearAiExtraction) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAiExtraction()
                    showSettings = true
                }) { Text(stringResource(R.string.open_settings)) }
            },
        )
        is AiExtractionState.Error -> AlertDialog(
            onDismissRequest = viewModel::clearAiExtraction,
            title = { Text(stringResource(R.string.ai_failed)) },
            text = { Text(stringResource(R.string.ai_failed_detail, extraction.message)) },
            confirmButton = {
                TextButton(onClick = viewModel::clearAiExtraction) { Text(stringResource(R.string.ok)) }
            },
        )
        is AiExtractionState.ReceiptSuccess -> Unit
        is AiExtractionState.StatementSuccess -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.image_analysis)) },
            text = {
                Text(
                    stringResource(
                        R.string.statement_ai_result,
                        extraction.statement.lines.size,
                    ),
                )
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearAiExtraction) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::applyExtractedStatement) {
                    Text(stringResource(R.string.apply_result))
                }
            },
        )
        AiExtractionState.Idle -> Unit
    }

    state.selectedTrip?.let { trip ->
        if (showReconciliations && pendingImageUri == null) {
            ReconciliationManagerDialog(
                initialSelectedId = reconciliationReturnId,
                reconciliations = state.reconciliations,
                receipts = state.receipts,
                defaultCurrencyCode = state.tripCurrencies.firstOrNull { it.isDefault }?.currencyCode
                    ?: trip.homeCurrencyCode,
                currencyCodes = state.tripCurrencies.map { it.currencyCode },
                candidateSelection = candidateSelection,
                analysisState = reconciliationAnalysis,
                onDismiss = {
                    showReconciliations = false
                    reconciliationReturnId = null
                    viewModel.clearCandidateSelection()
                },
                onCreate = viewModel::createReconciliation,
                onUpdateHeader = viewModel::updateReconciliation,
                onAddLine = viewModel::addStatementLine,
                onUpdateLine = viewModel::updateStatementLine,
                onDeleteLine = viewModel::deleteStatementLine,
                onAcceptLine = viewModel::setStatementLineAccepted,
                onLoadCandidates = viewModel::loadCandidates,
                onClearCandidates = viewModel::clearCandidateSelection,
                onAssignReceipt = viewModel::assignReceipt,
                onClearLineMatch = viewModel::clearLineMatch,
                onRun = viewModel::runReconciliation,
                onReset = viewModel::resetReconciliation,
                onDelete = viewModel::deleteReconciliation,
                onOpenImage = { reconciliation ->
                    reconciliationReturnId = reconciliation.reconciliation.id
                    imageTargetReconciliationId = reconciliation.reconciliation.id
                    imageTargetReceiptId = null
                    imageTargetHadLinkedImage = true
                    pendingImageUriString = reconciliation.reconciliation.statementImageUri
                    showReconciliations = false
                },
                onChooseImage = { reconciliation ->
                    reconciliationReturnId = reconciliation.reconciliation.id
                    imageTargetReconciliationId = reconciliation.reconciliation.id
                    imageTargetReceiptId = null
                    imageTargetHadLinkedImage = reconciliation.reconciliation.statementImageUri != null
                    showReconciliations = false
                    chooseImage()
                },
                onAnalyzeImage = { reconciliation ->
                    reconciliation.reconciliation.statementImageUri?.let { imageUri ->
                        viewModel.analyzeStatement(reconciliation.reconciliation.id, imageUri)
                    }
                },
                homeCurrencyCode = trip.homeCurrencyCode,
                recentCurrencyCodes = state.recentCurrencyCodes,
                exchangeRateLookup = exchangeRateLookup,
                onLookupRate = viewModel::requestExchangeRate,
                onAddTripCurrency = viewModel::addCurrencyToSelectedTrip,
            )
        }
    }

    fullscreenImageUriString?.let { imageUri ->
        FullscreenReceiptImage(
            imageUri = imageUri,
            onDismiss = { fullscreenImageUriString = null },
        )
    }
}

@Composable
private fun ExportDataDialog(
    trips: List<TripEntity>,
    onDismiss: () -> Unit,
    onExport: (ExportFormat, Set<String>) -> Unit,
) {
    var format by remember { mutableStateOf(ExportFormat.BILL_CHECK) }
    var selectedIds by remember(trips) { mutableStateOf(trips.map { it.id }.toSet()) }
    TransferSelectionDialog(
        title = stringResource(R.string.export_data),
        confirmLabel = stringResource(R.string.export_action),
        confirmEnabled = selectedIds.isNotEmpty(),
        onDismiss = onDismiss,
        onConfirm = { onExport(format, selectedIds) },
    ) {
        Text(stringResource(R.string.export_format), style = MaterialTheme.typography.titleMedium)
        ExportFormat.entries.forEach { candidate ->
            val title = when (candidate) {
                ExportFormat.BILL_CHECK -> stringResource(R.string.export_format_backup)
                ExportFormat.CSV -> stringResource(R.string.export_format_csv)
                ExportFormat.PDF -> stringResource(R.string.export_format_pdf)
            }
            val detail = when (candidate) {
                ExportFormat.BILL_CHECK -> stringResource(R.string.export_format_backup_detail)
                ExportFormat.CSV -> stringResource(R.string.export_format_csv_detail)
                ExportFormat.PDF -> stringResource(R.string.export_format_pdf_detail)
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { format = candidate }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = format == candidate, onClick = { format = candidate })
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(detail, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.select_trips), style = MaterialTheme.typography.titleMedium)
        trips.forEach { trip ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    selectedIds = selectedIds.toMutableSet().apply {
                        if (!add(trip.id)) remove(trip.id)
                    }
                }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = trip.id in selectedIds,
                    onCheckedChange = { checked ->
                        selectedIds = selectedIds.toMutableSet().apply {
                            if (checked) add(trip.id) else remove(trip.id)
                        }
                    },
                )
                Text(trip.name)
            }
        }
    }
}

@Composable
private fun ImportTripsDialog(
    preview: ImportPreview,
    onDismiss: () -> Unit,
    onImport: (Set<String>) -> Unit,
) {
    var selectedIds by remember(preview) {
        mutableStateOf(preview.trips.map { it.sourceId }.toSet())
    }
    TransferSelectionDialog(
        title = stringResource(R.string.import_data),
        confirmLabel = stringResource(R.string.import_action),
        confirmEnabled = selectedIds.isNotEmpty(),
        onDismiss = onDismiss,
        onConfirm = { onImport(selectedIds) },
    ) {
        Text(
            when (preview.format) {
                de.shakie.billcheck.data.TransferFormat.BILL_CHECK ->
                    stringResource(R.string.import_backup_description)
                de.shakie.billcheck.data.TransferFormat.CSV ->
                    stringResource(R.string.import_csv_description)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        preview.trips.forEach { trip ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    selectedIds = selectedIds.toMutableSet().apply {
                        if (!add(trip.sourceId)) remove(trip.sourceId)
                    }
                }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = trip.sourceId in selectedIds,
                    onCheckedChange = { checked ->
                        selectedIds = selectedIds.toMutableSet().apply {
                            if (checked) add(trip.sourceId) else remove(trip.sourceId)
                        }
                    },
                )
                Column {
                    Text(trip.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(
                            R.string.import_trip_counts,
                            trip.receiptCount,
                            trip.reconciliationCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferSelectionDialog(
    title: String,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.86f),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Text(
                    title,
                    modifier = Modifier.padding(24.dp, 22.dp, 24.dp, 14.dp),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp, 8.dp, 16.dp, 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) }
                }
            }
        }
    }
}

@Composable
private fun TransferWorkingDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.data_transfer)) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.transfer_working))
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun TransferMessageDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}

@Composable
private fun AppearanceSettingsDialog(
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    homeCurrencyCode: String,
    recentCurrencyCodes: List<String>,
    onHomeCurrencyChange: (String) -> Unit,
    aiSettings: de.shakie.billcheck.data.AiSettings,
    geminiModels: GeminiModelsState,
    localAiSettings: de.shakie.billcheck.data.LocalAiSettings,
    localAiConnection: LocalAiConnectionState,
    onLoadGeminiModels: () -> Unit,
    onSaveAiSettings: (String, String?, String) -> Unit,
    onClearAiApiKey: () -> Unit,
    onTestLocalAi: (String, String, LocalAiAuthType, String, String?) -> Unit,
    onSaveLocalAi: (String, String, LocalAiAuthType, String, String?) -> Unit,
    onClearLocalAiCredential: () -> Unit,
    onClearLocalAiConnectionResult: () -> Unit,
    onDismiss: () -> Unit,
) {
    var apiKey by remember { mutableStateOf("") }
    var aiProvider by remember(aiSettings.providerId) { mutableStateOf(aiSettings.providerId) }
    var model by remember(aiSettings.model) { mutableStateOf(aiSettings.model) }
    var localAiBaseUrl by remember(localAiSettings.baseUrl) { mutableStateOf(localAiSettings.baseUrl) }
    var localAiModel by remember(localAiSettings.model) { mutableStateOf(localAiSettings.model) }
    var localAiAuthType by remember(localAiSettings.authType) {
        mutableStateOf(localAiSettings.authType)
    }
    var localAiUsername by remember(localAiSettings.username) {
        mutableStateOf(localAiSettings.username)
    }
    var localAiCredential by remember { mutableStateOf("") }
    var selectedHomeCurrency by remember(homeCurrencyCode) { mutableStateOf(homeCurrencyCode) }
    var showHomeCurrencyPicker by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f).imePadding(),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                Text(
                    stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(24.dp, 22.dp, 24.dp, 14.dp),
                )
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                Text(
                    stringResource(R.string.appearance),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                ThemeChoiceRow(
                    label = stringResource(R.string.light_mode),
                    selected = !darkTheme,
                    onClick = { onDarkThemeChange(false) },
                )
                ThemeChoiceRow(
                    label = stringResource(R.string.dark_mode),
                    selected = darkTheme,
                    onClick = { onDarkThemeChange(true) },
                )
                Text(
                    stringResource(R.string.initial_theme_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text(
                    stringResource(R.string.home_currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedButton(
                    onClick = { showHomeCurrencyPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(selectedHomeCurrency)
                }
                Text(
                    stringResource(R.string.home_currency_new_trips_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text(
                    stringResource(R.string.ai_recognition),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.ai_provider),
                    style = MaterialTheme.typography.labelLarge,
                )
                LocalAiAuthChoice(
                    label = stringResource(R.string.local_ai_provider),
                    selected = aiProvider == AI_PROVIDER_LOCAL,
                    onClick = { aiProvider = AI_PROVIDER_LOCAL },
                    modifier = Modifier.fillMaxWidth(),
                )
                LocalAiAuthChoice(
                    label = stringResource(R.string.gemini),
                    selected = aiProvider == AI_PROVIDER_GEMINI,
                    onClick = { aiProvider = AI_PROVIDER_GEMINI },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.gemini_configuration),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedButton(
                    onClick = onLoadGeminiModels,
                    enabled = geminiModels !is GeminiModelsState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (geminiModels is GeminiModelsState.Loading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.models_loading))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.load_models))
                    }
                }
                when (geminiModels) {
                    GeminiModelsState.MissingApiKey -> Text(
                        stringResource(R.string.models_need_key),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    is GeminiModelsState.Error -> Text(
                        stringResource(R.string.models_failed, geminiModels.message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    is GeminiModelsState.Success -> geminiModels.models.forEach { available ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .clickable { model = available.id }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = model == available.id, onClick = { model = available.id })
                            Column {
                                Text(available.displayName)
                                Text(
                                    stringResource(
                                        R.string.model_context,
                                        formatTokenLimit(available.inputTokenLimit),
                                        formatTokenLimit(available.outputTokenLimit),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    else -> Unit
                }
                if (aiSettings.hasApiKey) {
                    Text(
                        stringResource(R.string.api_key_saved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.gemini_api_key)) },
                    placeholder = { Text(stringResource(R.string.api_key_placeholder)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(stringResource(R.string.gemini_model)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (aiSettings.hasApiKey) {
                    TextButton(onClick = {
                        onClearAiApiKey()
                        apiKey = ""
                    }) {
                        Text(stringResource(R.string.remove_api_key), color = MaterialTheme.colorScheme.error)
                    }
                }
                Text(
                    stringResource(R.string.ai_settings_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.quota_not_available),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = { uriHandler.openUri("https://aistudio.google.com/rate-limit") },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(stringResource(R.string.open_quota_dashboard))
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text(
                    stringResource(R.string.local_ai_server),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.local_ai_connection_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = localAiBaseUrl,
                    onValueChange = {
                        localAiBaseUrl = it
                        onClearLocalAiConnectionResult()
                    },
                    label = { Text(stringResource(R.string.local_ai_base_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().onFocusChanged {
                        if (it.isFocused) onClearLocalAiConnectionResult()
                    },
                )
                OutlinedTextField(
                    value = localAiModel,
                    onValueChange = {
                        localAiModel = it
                        onClearLocalAiConnectionResult()
                    },
                    label = { Text(stringResource(R.string.local_ai_model)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().onFocusChanged {
                        if (it.isFocused) onClearLocalAiConnectionResult()
                    },
                )
                Text(
                    stringResource(R.string.authentication),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(Modifier.fillMaxWidth()) {
                    LocalAiAuthChoice(
                        label = stringResource(R.string.basic_authentication),
                        selected = localAiAuthType == LocalAiAuthType.BASIC,
                        onClick = {
                            localAiAuthType = LocalAiAuthType.BASIC
                            onClearLocalAiConnectionResult()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    LocalAiAuthChoice(
                        label = stringResource(R.string.bearer_token),
                        selected = localAiAuthType == LocalAiAuthType.BEARER,
                        onClick = {
                            localAiAuthType = LocalAiAuthType.BEARER
                            onClearLocalAiConnectionResult()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (localAiAuthType == LocalAiAuthType.BASIC) {
                    OutlinedTextField(
                        value = localAiUsername,
                        onValueChange = {
                            localAiUsername = it
                            onClearLocalAiConnectionResult()
                        },
                        label = { Text(stringResource(R.string.username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().onFocusChanged {
                            if (it.isFocused) onClearLocalAiConnectionResult()
                        },
                    )
                }
                if (localAiSettings.hasCredential) {
                    Text(
                        stringResource(R.string.local_ai_credential_saved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                OutlinedTextField(
                    value = localAiCredential,
                    onValueChange = {
                        localAiCredential = it
                        onClearLocalAiConnectionResult()
                    },
                    label = {
                        Text(
                            if (localAiAuthType == LocalAiAuthType.BASIC) {
                                stringResource(R.string.password)
                            } else {
                                stringResource(R.string.access_token)
                            },
                        )
                    },
                    placeholder = { Text(stringResource(R.string.credential_placeholder)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().onFocusChanged {
                        if (it.isFocused) onClearLocalAiConnectionResult()
                    },
                )
                if (localAiSettings.hasCredential) {
                    TextButton(onClick = {
                        onClearLocalAiCredential()
                        localAiCredential = ""
                    }) {
                        Text(
                            stringResource(R.string.remove_local_ai_credential),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        onTestLocalAi(
                            localAiBaseUrl,
                            localAiModel,
                            localAiAuthType,
                            localAiUsername,
                            localAiCredential.takeIf(String::isNotBlank),
                        )
                    },
                    enabled = localAiConnection !is LocalAiConnectionState.Testing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (localAiConnection is LocalAiConnectionState.Testing) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.connection_testing))
                    } else {
                        Text(stringResource(R.string.test_connection))
                    }
                }
                when (localAiConnection) {
                    LocalAiConnectionState.MissingCredential -> Text(
                        stringResource(R.string.local_ai_credential_missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    is LocalAiConnectionState.Error -> Text(
                        stringResource(R.string.connection_failed, localAiConnection.message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    is LocalAiConnectionState.Success -> Text(
                        if (localAiConnection.result.configuredModelAvailable) {
                            stringResource(
                                R.string.connection_success,
                                localAiConnection.result.elapsedMilliseconds,
                            )
                        } else {
                            stringResource(
                                R.string.connection_model_missing,
                                localAiConnection.result.elapsedMilliseconds,
                                localAiConnection.result.availableModels.joinToString(),
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (localAiConnection.result.configuredModelAvailable) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                    )
                    else -> Unit
                }
                Spacer(Modifier.height(24.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp, 8.dp, 16.dp, 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = {
                        onSaveAiSettings(
                            aiProvider,
                            apiKey.takeIf(String::isNotBlank),
                            model,
                        )
                        onSaveLocalAi(
                            localAiBaseUrl,
                            localAiModel,
                            localAiAuthType,
                            localAiUsername,
                            localAiCredential.takeIf(String::isNotBlank),
                        )
                        onHomeCurrencyChange(selectedHomeCurrency)
                        onDismiss()
                    }) { Text(stringResource(R.string.save)) }
                }
            }
        }
    }
    if (showHomeCurrencyPicker) {
        CurrencyPickerDialog(
            selectedCurrencyCode = selectedHomeCurrency,
            preferredCurrencyCodes = listOf(homeCurrencyCode),
            recentCurrencyCodes = recentCurrencyCodes,
            onCurrencySelected = {
                selectedHomeCurrency = it.code
                showHomeCurrencyPicker = false
            },
            onDismiss = { showHomeCurrencyPicker = false },
        )
    }

}

@Composable
private fun LocalAiAuthChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ThemeChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun TripDrawerItem(
    trip: TripEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val rowHeightPx = with(density) { 64.dp.toPx() }
    var dragOffsetY by remember(trip.id) { mutableStateOf(0f) }
    var dragging by remember(trip.id) { mutableStateOf(false) }

    NavigationDrawerItem(
        label = { Text(trip.name) },
        selected = selected,
        icon = {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = stringResource(R.string.reorder_trip_named, trip.name),
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp)
                    .pointerInput(trip.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { dragging = true },
                            onDragCancel = {
                                dragOffsetY = 0f
                                dragging = false
                            },
                            onDragEnd = {
                                val positions = (dragOffsetY / rowHeightPx).roundToInt()
                                dragOffsetY = 0f
                                dragging = false
                                onMove(positions)
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffsetY += amount.y
                            },
                        )
                    },
            )
        },
        badge = {
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_trip_named, trip.name),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        onClick = onSelect,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .zIndex(if (dragging) 1f else 0f),
    )
}

@Composable
private fun EmptyTrips(modifier: Modifier, onCreate: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.first_trip_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.first_trip_text),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreate) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.create_trip))
        }
    }
}

@Composable
private fun Dashboard(
    state: MainUiState,
    modifier: Modifier,
    onManualReceipt: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onBrowseFolders: () -> Unit,
    onOpenReceiptImage: (String) -> Unit,
    onEditReceipt: (ReceiptWithItems) -> Unit,
    onDeleteReceipt: (ReceiptEntity) -> Unit,
    onOpenReconciliations: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Summary(state)
        }
        item {
            ReceiptActions(onCamera, onGallery, onBrowseFolders, onManualReceipt)
        }
        item {
            OutlinedButton(onClick = onOpenReconciliations, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.check_statement))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.receipt_list),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    state.receipts.size.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.receipts.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(
                        stringResource(R.string.no_receipts),
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(state.receipts, key = { it.receipt.id }) { receipt ->
                ReceiptCard(
                    receipt,
                    state.selectedTrip?.homeCurrencyCode ?: state.defaultHomeCurrencyCode,
                    onDeleteReceipt,
                    onOpenReceiptImage,
                    onEditReceipt,
                )
            }
        }
    }
}

@Composable
private fun Summary(state: MainUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            state.selectedTrip?.let { trip ->
                Text(
                    trip.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(10.dp))
            }
            Text(
                stringResource(R.string.rounded_total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "${state.roundedHomeMajor} ${state.selectedTrip?.homeCurrencyCode ?: state.defaultHomeCurrencyCode}",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                stringResource(R.string.rounded_total_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallSummary(
                    label = stringResource(R.string.exact_total),
                    value = formatMinor(
                        state.exactHomeMinor,
                        state.selectedTrip?.homeCurrencyCode ?: state.defaultHomeCurrencyCode,
                    ),
                    modifier = Modifier.weight(1f),
                )
                SmallSummary(
                    label = stringResource(R.string.receipts_count),
                    value = state.receipts.size.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SmallSummary(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReceiptActions(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onBrowseFolders: () -> Unit,
    onManual: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onCamera, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.take_photo))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onGallery, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.choose_image))
            }
            OutlinedButton(onClick = onManual, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.manual_entry))
            }
        }
        TextButton(onClick = onBrowseFolders, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.browse_folders))
        }
    }
}

@Composable
private fun ReceiptCard(
    receiptWithItems: ReceiptWithItems,
    homeCurrencyCode: String,
    onDelete: (ReceiptEntity) -> Unit,
    onOpenImage: (String) -> Unit,
    onEdit: (ReceiptWithItems) -> Unit,
) {
    val receipt = receiptWithItems.receipt
    val exactHomeMinor = MoneyCalculator.exactHomeMinor(receipt)
    val rounded = MoneyCalculator.roundedUpHomeMajor(exactHomeMinor, homeCurrencyCode)
    Card(
        onClick = { onEdit(receiptWithItems) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            receipt.imageUri?.let { imageUri ->
                ReceiptThumbnail(
                    imageUri = imageUri,
                    modifier = Modifier
                        .size(72.dp)
                        .clickable { onOpenImage(imageUri) },
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    receipt.location.ifBlank { stringResource(R.string.add_receipt) },
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    buildString {
                        append(formatDate(receipt.occurredAt))
                        if (receipt.checkNumber.isNotBlank()) append("  ·  #${receipt.checkNumber}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    formatMinor(receipt.amountMinor, receipt.currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(
                        R.string.receipt_exchange_rate,
                        homeCurrencyCode,
                        receipt.exchangeRateSnapshot,
                        receipt.currencyCode,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (receipt.tipMinor > 0) {
                    Text(
                        stringResource(
                            R.string.receipt_tip,
                            formatMinor(receipt.tipMinor, receipt.tipCurrencyCode),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(
                            R.string.receipt_exchange_rate,
                            homeCurrencyCode,
                            receipt.tipExchangeRateSnapshot,
                            receipt.tipCurrencyCode,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (receiptWithItems.items.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    receiptWithItems.items
                        .sortedBy { it.sortPosition }
                        .take(3)
                        .forEach { item ->
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    item.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    formatMinor(item.amountMinor, item.currencyCode),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    if (receiptWithItems.items.size > 3) {
                        Text(
                            stringResource(
                                R.string.more_receipt_items,
                                receiptWithItems.items.size - 3,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$rounded $homeCurrencyCode", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    formatMinor(exactHomeMinor, homeCurrencyCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onDelete(receipt) }) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete_receipt),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun TripEditorDialog(
    existing: TripEntity? = null,
    existingCurrencies: List<TripCurrencyEntity> = emptyList(),
    suggestedName: String,
    homeCurrencyCode: String,
    recentCurrencyCodes: List<String> = emptyList(),
    usedCurrencyCodes: Set<String> = emptySet(),
    exchangeRateLookup: ExchangeRateLookupState,
    onLookupRate: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onDeleteRequested: (() -> Unit)? = null,
    onSave: (String, List<TripCurrencyInput>, Long, String, Boolean) -> Boolean,
) {
    val stateKey = existing?.id
    var name by remember(stateKey) { mutableStateOf(existing?.name ?: suggestedName) }
    var currencies by remember(stateKey, existingCurrencies) {
        mutableStateOf(
            existingCurrencies.map {
                EditableTripCurrency(
                    code = it.currencyCode,
                    rate = it.homeToCurrencyRate,
                    mode = if (it.exchangeRateMode == "DAILY") {
                        EditableExchangeRateMode.DAILY
                    } else {
                        EditableExchangeRateMode.FIXED
                    },
                    isDefault = it.isDefault,
                )
            }.ifEmpty {
                listOf(
                    EditableTripCurrency(
                        code = homeCurrencyCode,
                        rate = "1",
                        mode = EditableExchangeRateMode.FIXED,
                        isDefault = true,
                    ),
                )
            },
        )
    }
    var tipCurrency by remember(stateKey) {
        mutableStateOf(existing?.defaultTipCurrencyCode ?: homeCurrencyCode)
    }
    var tip by remember(stateKey) {
        mutableStateOf(
            existing?.defaultTipMinor?.let { formatInputMinor(it, tipCurrency) }
                ?: formatInputMinor(CurrencyAmount.minorFactor(tipCurrency).longValueExact(), tipCurrency),
        )
    }
    var tipSelected by remember(stateKey) {
        mutableStateOf(existing?.defaultTipSelected ?: false)
    }
    var invalidTip by remember(stateKey) { mutableStateOf(false) }
    var invalidRate by remember(stateKey) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(exchangeRateLookup) {
        val success = exchangeRateLookup as? ExchangeRateLookupState.Success
        if (success?.quote?.baseCurrencyCode == homeCurrencyCode) {
            currencies = currencies.map { currency ->
                if (currency.code == success.quote.targetCurrencyCode) {
                    currency.copy(rate = success.quote.targetUnitsPerBase)
                } else {
                    currency
                }
            }
        }
    }

    ScrollableEditorDialog(
        title = stringResource(if (existing == null) R.string.create_trip else R.string.edit_trip),
        onDismiss = onDismiss,
        destructiveAction = onDeleteRequested?.let { requestDelete ->
            {
                OutlinedButton(
                    onClick = requestDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.delete_trip),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        onSave = {
            val parsedTip = MainViewModel.parseMinor(tip, tipCurrency)
            if (parsedTip == null) {
                invalidTip = true
                return@ScrollableEditorDialog
            }
            val saved = onSave(
                name,
                currencies.map {
                    TripCurrencyInput(
                        currencyCode = it.code,
                        homeToCurrencyRate = it.rate,
                        exchangeRateMode = it.mode.name,
                        isDefault = it.isDefault,
                    )
                },
                parsedTip,
                tipCurrency,
                tipSelected,
            )
            invalidRate = !saved
        },
    ) {
        OutlinedTextField(
            name,
            { name = it },
            label = { Text(stringResource(R.string.trip_name)) },
            modifier = Modifier.fillMaxWidth(),
        )
        TripCurrencyEditorSection(
            homeCurrencyCode = homeCurrencyCode,
            currencies = currencies,
            tipCurrencyCode = tipCurrency,
            usedCurrencyCodes = usedCurrencyCodes,
            recentCurrencyCodes = recentCurrencyCodes,
            onRateChange = { code, value ->
                currencies = currencies.map { if (it.code == code) it.copy(rate = value) else it }
                invalidRate = false
            },
            onModeChange = { code, mode ->
                currencies = currencies.map { if (it.code == code) it.copy(mode = mode) else it }
            },
            onSetDefault = { code ->
                currencies = currencies.map { it.copy(isDefault = it.code == code) }
            },
            onDelete = { code -> currencies = currencies.filterNot { it.code == code } },
            onAdd = { entry ->
                currencies = currencies + EditableTripCurrency(
                    code = entry.code,
                    rate = "",
                    mode = EditableExchangeRateMode.FIXED,
                    isDefault = false,
                )
                onLookupRate(homeCurrencyCode, entry.code)
            },
            onRefreshRate = { code -> onLookupRate(homeCurrencyCode, code) },
        )
        TextButton(
            onClick = { uriHandler.openUri("https://www.exchangerate-api.com") },
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(stringResource(R.string.rate_attribution), style = MaterialTheme.typography.labelSmall)
        }
        if (invalidRate) {
            Text(stringResource(R.string.invalid_amount), color = MaterialTheme.colorScheme.error)
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        Text(stringResource(R.string.default_tip), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            tip,
            {
                tip = it
                invalidTip = false
            },
            label = { Text(stringResource(R.string.default_tip)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = invalidTip,
            supportingText = if (invalidTip) {
                { Text(stringResource(R.string.invalid_amount)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
        )
        TripCurrencySelector(
            selectedCurrencyCode = tipCurrency,
            currencyCodes = currencies.map { it.code },
            onCurrencySelected = {
                tip = tip.trim().replace(',', '.').toBigDecimalOrNull()
                    ?.stripTrailingZeros()?.toPlainString() ?: tip
                tipCurrency = it
                invalidTip = MainViewModel.parseMinor(tip, it) == null
            },
            onAddCurrencyRequested = {},
            label = stringResource(R.string.tip_currency),
            showAddCurrencyOption = false,
        )
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .clickable { tipSelected = !tipSelected }.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.default_tip_preselected))
                Text(
                    stringResource(R.string.default_tip_preselected_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = tipSelected, onCheckedChange = null)
        }
    }
}

@Composable
internal fun ReceiptEditorDialog(
    trip: TripEntity,
    tripCurrencies: List<TripCurrencyEntity>,
    recentCurrencyCodes: List<String>,
    exchangeRateLookup: ExchangeRateLookupState,
    onLookupRate: (String, String) -> Unit,
    existing: ReceiptWithItems? = null,
    visible: Boolean = true,
    imageUri: String? = null,
    locationSuggestions: List<String> = emptyList(),
    itemNameSuggestions: List<String> = emptyList(),
    aiExtraction: AiExtractionState = AiExtractionState.Idle,
    localOcr: LocalOcrState = LocalOcrState.Idle,
    onTakePhoto: () -> Unit,
    onChooseImage: () -> Unit,
    onBrowseFolders: () -> Unit,
    onOpenImage: () -> Unit,
    onAnalyzeImage: () -> Unit,
    onAnalyzeLocally: () -> Unit,
    onClearLocalOcr: () -> Unit,
    onAddTripCurrency: (String, String, Boolean) -> Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Boolean, List<ReceiptItemDraft>) -> Boolean,
) {
    val stateKey = existing?.receipt?.id
    val defaultCurrencyCode = tripCurrencies.firstOrNull { it.isDefault }?.currencyCode
        ?: trip.homeCurrencyCode
    var receiptCurrencyCode by remember(stateKey) {
        mutableStateOf(existing?.receipt?.currencyCode ?: defaultCurrencyCode)
    }
    val existingTip = existing?.receipt?.takeIf { it.tipMinor > 0 }
    val tipPreviewMinor = existingTip?.tipMinor ?: trip.defaultTipMinor
    val tipPreviewCurrencyCode = existingTip?.tipCurrencyCode ?: trip.defaultTipCurrencyCode
    var location by remember(stateKey) { mutableStateOf(existing?.receipt?.location.orEmpty()) }
    var check by remember(stateKey) { mutableStateOf(existing?.receipt?.checkNumber.orEmpty()) }
    var amount by remember(stateKey) {
        mutableStateOf(
            existing?.receipt?.amountMinor?.let { formatInputMinor(it, receiptCurrencyCode) }.orEmpty(),
        )
    }
    var occurredOn by remember(stateKey) {
        mutableStateOf(
            existing?.receipt?.occurredAt?.let(::formatEditorDate)
                ?: LocalDate.now().format(editorDateFormatter),
        )
    }
    var addTip by remember(stateKey) {
        mutableStateOf(existing?.receipt?.tipMinor?.let { it > 0 } ?: trip.defaultTipSelected)
    }
    var invalidAmount by remember(stateKey) { mutableStateOf(false) }
    var invalidDate by remember(stateKey) { mutableStateOf(false) }
    var showFullscreenImage by remember(stateKey, imageUri) { mutableStateOf(false) }
    var openTextSelectionWhenReady by remember(stateKey, imageUri) { mutableStateOf(false) }
    var showTextSelection by remember(stateKey, imageUri) { mutableStateOf(false) }
    var pendingImageText by remember(stateKey, imageUri) { mutableStateOf<String?>(null) }
    var analysisRequest by remember(stateKey, imageUri) { mutableStateOf<ReceiptAnalysisRequest?>(null) }
    var pendingAiReview by remember(stateKey, imageUri) { mutableStateOf<PendingReceiptAiReview?>(null) }
    val initialItems = existing?.items
        ?.sortedBy { it.sortPosition }
        ?.mapIndexed { index, item ->
            EditableReceiptItem(
                id = index.toLong(),
                name = item.name,
                amountText = formatInputMinor(item.amountMinor, receiptCurrencyCode),
            )
        }
        .orEmpty()
        .ifEmpty { listOf(EditableReceiptItem(id = 0)) }
    var nextItemId by remember(stateKey) { mutableStateOf(initialItems.size.toLong()) }
    var items by remember(stateKey) { mutableStateOf(initialItems) }
    val itemSumMinor = items.mapNotNull {
        MainViewModel.parseMinor(it.amountText, receiptCurrencyCode)
    }.sum()
    val receiptMinor = MainViewModel.parseMinor(amount, receiptCurrencyCode)
    var showCurrencyPicker by remember(stateKey) { mutableStateOf(false) }
    var pendingNewCurrencyCode by remember(stateKey) { mutableStateOf<String?>(null) }
    var pendingCurrencyRate by remember(stateKey) { mutableStateOf("") }
    var pendingCurrencyDaily by remember(stateKey) { mutableStateOf(false) }
    var pendingCurrencyRateInvalid by remember(stateKey) { mutableStateOf(false) }
    var newlyConfiguredCurrencyCodes by remember(stateKey) { mutableStateOf(emptySet<String>()) }
    var retainedExtractedReceipt by remember(stateKey, imageUri) {
        mutableStateOf<ExtractedReceipt?>(null)
    }
    var pendingCurrencyExtraction by remember(stateKey, imageUri) {
        mutableStateOf<ExtractedReceipt?>(null)
    }
    var pendingCurrencyAnalysisRequest by remember(stateKey, imageUri) {
        mutableStateOf<ReceiptAnalysisRequest?>(null)
    }
    val incomingExtractedReceipt = (aiExtraction as? AiExtractionState.ReceiptSuccess)
        ?.takeIf { it.imageUri == imageUri }
        ?.receipt
    val extractedReceipt = incomingExtractedReceipt ?: retainedExtractedReceipt
    val recognizedPage = (localOcr as? LocalOcrState.Success)
        ?.takeIf { it.imageUri == imageUri }
        ?.page
    val selectablePage = remember(recognizedPage, extractedReceipt) {
        recognizedPage?.let { localPage ->
            HybridOcrPageBuilder.merge(
                local = localPage,
                transcript = extractedReceipt?.transcriptLines.orEmpty(),
                extraCandidates = extractedReceipt
                    ?.takeIf { it.transcriptLines.isEmpty() }
                    ?.imageTextCandidates()
                    .orEmpty(),
            )
        }
    }
    fun currentBaseline() = ReceiptEditorBaseline(
        location = location,
        checkNumber = check,
        amountText = amount,
        currencyCode = receiptCurrencyCode,
        occurredOn = occurredOn,
        items = items.map { it.name to it.amountText },
    )
    val initialEditorBaseline = remember(stateKey) { currentBaseline() }
    fun protectAiField(field: ReceiptAiField, resolved: Boolean = false) {
        pendingAiReview = pendingAiReview?.let { review ->
            review.copy(
                protectedFields = review.protectedFields + field,
                resolvedFields = if (resolved) review.resolvedFields + field else review.resolvedFields,
            )
        }
    }
    fun protectAiItemField(index: Int, kind: ReceiptAiItemFieldKind, resolved: Boolean = false) {
        val key = ReceiptAiItemField(index, kind)
        pendingAiReview = pendingAiReview?.let { review ->
            review.copy(
                itemsProtected = true,
                protectedItemFields = review.protectedItemFields + key,
                resolvedItemFields = if (resolved) review.resolvedItemFields + key else review.resolvedItemFields,
            )
        }
    }
    fun replaceItemsFromExtraction(extracted: ExtractedReceipt) {
        if (extracted.items.isEmpty()) return
        items = extracted.items.mapIndexed { index, item ->
            EditableReceiptItem(
                id = index.toLong(),
                name = formatExtractedItemName(item.quantityText, item.name),
                amountText = item.amountText,
                aiSourceIndex = index,
                nameAiSuggestions = itemNameAiSuggestions(item),
                amountAiSuggestions = item.amountSuggestions.toEditorSuggestions(),
            )
        }
        nextItemId = items.size.toLong()
    }
    fun applyExtractedReceipt(extracted: ExtractedReceipt) {
        location = extracted.location
        check = extracted.checkNumber
        amount = extracted.totalAmountText
        extracted.occurredOn.takeIf(String::isNotBlank)?.let { extractedDate ->
            occurredOn = normalizeEditorDate(extractedDate) ?: occurredOn
        }
        replaceItemsFromExtraction(extracted)
        invalidAmount = false
        invalidDate = false
        pendingAiReview = null
    }
    fun queueAiReview(
        extracted: ExtractedReceipt,
        request: ReceiptAnalysisRequest,
        suppressCurrency: Boolean = false,
    ) {
        val current = currentBaseline()
        val protectedFields = changedReceiptAiFields(
            requestBaseline = request.baseline,
            currentBaseline = current,
            suppressCurrency = suppressCurrency,
        )
        pendingAiReview = PendingReceiptAiReview(
            extracted = extracted,
            protectedFields = protectedFields,
            resolvedFields = if (suppressCurrency) setOf(ReceiptAiField.CURRENCY) else emptySet(),
            itemsProtected = existing != null ||
                request.baseline.items.any { (name, value) -> name.isNotBlank() || value.isNotBlank() } ||
                request.baseline.items != current.items,
        )
    }
    fun applyOpenAiValues() {
        val review = pendingAiReview ?: return
        val extracted = review.extracted
        fun open(field: ReceiptAiField) =
            field !in review.protectedFields && field !in review.resolvedFields
        if (open(ReceiptAiField.LOCATION) && extracted.location.isNotBlank()) location = extracted.location
        if (open(ReceiptAiField.CHECK_NUMBER) && extracted.checkNumber.isNotBlank()) check = extracted.checkNumber
        if (open(ReceiptAiField.AMOUNT) && extracted.totalAmountText.isNotBlank()) {
            amount = extracted.totalAmountText
            invalidAmount = false
        }
        if (open(ReceiptAiField.DATE) && extracted.occurredOn.isNotBlank()) {
            occurredOn = normalizeEditorDate(extracted.occurredOn) ?: extracted.occurredOn
            invalidDate = false
        }
        val detectedCode = extracted.currencyCode.trim().uppercase(Locale.ROOT)
        if (open(ReceiptAiField.CURRENCY) && (
                tripCurrencies.any { it.currencyCode == detectedCode } ||
                    detectedCode in newlyConfiguredCurrencyCodes
                )
        ) {
            receiptCurrencyCode = detectedCode
            invalidAmount = false
        }
        if (!review.itemsProtected && !review.itemsDismissed) {
            replaceItemsFromExtraction(extracted)
            pendingAiReview = null
        } else if (review.itemsDismissed || extracted.items.isEmpty()) {
            pendingAiReview = null
        } else {
            pendingAiReview = review.copy(resolvedFields = ReceiptAiField.entries.toSet())
        }
    }
    fun applyReviewedItems() {
        val review = pendingAiReview ?: return
        val previousItems = items
        items = review.extracted.items.mapIndexed { index, extractedItem ->
            val previous = previousItems.getOrNull(index)
            val preserveName = ReceiptAiItemField(index, ReceiptAiItemFieldKind.NAME) in
                review.protectedItemFields
            val preserveAmount = ReceiptAiItemField(index, ReceiptAiItemFieldKind.AMOUNT) in
                review.protectedItemFields
            EditableReceiptItem(
                id = previous?.id ?: nextItemId++,
                name = if (preserveName && previous != null) {
                    previous.name
                } else {
                    formatExtractedItemName(extractedItem.quantityText, extractedItem.name)
                },
                amountText = if (preserveAmount && previous != null) {
                    previous.amountText
                } else {
                    extractedItem.amountText
                },
                aiSourceIndex = index,
                nameAiSuggestions = itemNameAiSuggestions(extractedItem),
                amountAiSuggestions = extractedItem.amountSuggestions.toEditorSuggestions(),
            )
        }
        nextItemId = maxOf(nextItemId, items.size.toLong())
        pendingAiReview = null
    }

    LaunchedEffect(localOcr, openTextSelectionWhenReady) {
        when {
            openTextSelectionWhenReady && recognizedPage != null -> {
                openTextSelectionWhenReady = false
                showTextSelection = true
            }
            openTextSelectionWhenReady && localOcr is LocalOcrState.Error -> {
                openTextSelectionWhenReady = false
            }
        }
    }

    LaunchedEffect(aiExtraction) {
        if (aiExtraction is AiExtractionState.Error) {
            analysisRequest = null
            return@LaunchedEffect
        }
        val extracted = incomingExtractedReceipt ?: return@LaunchedEffect
        retainedExtractedReceipt = extracted
        val request = analysisRequest ?: ReceiptAnalysisRequest(currentBaseline(), wasVirgin = false)
        analysisRequest = null
        val mayApplyAutomatically = shouldAutoApplyReceiptAnalysis(request, currentBaseline())
        val supportedCurrencyCodes = CurrencyCatalog.entries(Locale.ROOT).mapTo(hashSetOf()) { it.code }
        val detectedCode = extracted.currencyCode.trim().uppercase(Locale.ROOT)
        when {
            detectedCode.isBlank() -> {
                if (mayApplyAutomatically) applyExtractedReceipt(extracted) else queueAiReview(extracted, request)
            }
            detectedCode !in supportedCurrencyCodes -> queueAiReview(
                extracted,
                request,
                suppressCurrency = true,
            )
            tripCurrencies.any { it.currencyCode == detectedCode } -> {
                if (mayApplyAutomatically) {
                    receiptCurrencyCode = detectedCode
                    applyExtractedReceipt(extracted)
                } else {
                    queueAiReview(extracted, request)
                }
            }
            else -> {
                pendingCurrencyExtraction = extracted
                pendingCurrencyAnalysisRequest = request
                pendingNewCurrencyCode = detectedCode
                pendingCurrencyRate = ""
                pendingCurrencyDaily = false
                onLookupRate(trip.homeCurrencyCode, detectedCode)
            }
        }
    }

    LaunchedEffect(exchangeRateLookup, pendingNewCurrencyCode) {
        val success = exchangeRateLookup as? ExchangeRateLookupState.Success ?: return@LaunchedEffect
        if (success.quote.baseCurrencyCode == trip.homeCurrencyCode &&
            success.quote.targetCurrencyCode == pendingNewCurrencyCode
        ) {
            pendingCurrencyRate = success.quote.targetUnitsPerBase
            pendingCurrencyRateInvalid = false
        }
    }

    if (!visible) return

    ScrollableEditorDialog(
        title = stringResource(if (existing == null) R.string.add_receipt else R.string.edit_receipt),
        onDismiss = onDismiss,
        onSave = {
            invalidDate = MainViewModel.parseReceiptDate(occurredOn) == null
            if (invalidDate) return@ScrollableEditorDialog
            invalidAmount = !onSave(
                location,
                check,
                amount,
                receiptCurrencyCode,
                occurredOn,
                addTip,
                items.map { ReceiptItemDraft(it.name, it.amountText) },
            )
        },
        contextualActions = pendingAiReview?.let { review ->
            {
                val awaitsItemDecision = review.resolvedFields.containsAll(ReceiptAiField.entries) &&
                    review.itemsProtected &&
                    !review.itemsDismissed &&
                    review.extracted.items.isNotEmpty()
                AiReviewActions(
                    itemDecision = awaitsItemDecision,
                    onApply = {
                        if (awaitsItemDecision) applyReviewedItems() else applyOpenAiValues()
                    },
                    onKeep = { pendingAiReview = null },
                )
            }
        },
    ) {
                ReceiptEditorImageSection(
                    imageUri = imageUri,
                    onPreviewImage = { showFullscreenImage = true },
                    onTakePhoto = onTakePhoto,
                    onChooseImage = onChooseImage,
                    onBrowseFolders = onBrowseFolders,
                    onOpenImage = onOpenImage,
                    onAnalyzeImage = {
                        val baseline = currentBaseline()
                        analysisRequest = ReceiptAnalysisRequest(
                            baseline = baseline,
                            wasVirgin = existing == null && baseline == initialEditorBaseline,
                        )
                        onAnalyzeImage()
                    },
                    onAnalyzeLocally = {
                        if (recognizedPage != null) {
                            showTextSelection = true
                        } else {
                            openTextSelectionWhenReady = true
                            onAnalyzeLocally()
                        }
                    },
                )
                LocalOcrStatus(
                    state = localOcr,
                    imageUri = imageUri,
                    onClose = onClearLocalOcr,
                )
                pendingImageText?.let { selectedText ->
                    PendingImageTextBanner(
                        selectedText = selectedText,
                        onCancel = { pendingImageText = null },
                    )
                }
                ImageTextTarget(
                    pendingText = pendingImageText,
                    fieldLabel = stringResource(R.string.location),
                    onApply = {
                        location = it
                        protectAiField(ReceiptAiField.LOCATION, resolved = true)
                        pendingImageText = null
                    },
                ) {
                    HistoryTextField(
                        value = location,
                        onValueChange = {
                            location = it
                            protectAiField(ReceiptAiField.LOCATION)
                        },
                        label = stringResource(R.string.location),
                        suggestions = locationSuggestions,
                        aiSuggestions = extractedReceipt
                            ?.locationSuggestions
                            ?.toEditorSuggestions()
                            .orEmpty(),
                        onSuggestionSelected = {
                            protectAiField(ReceiptAiField.LOCATION, resolved = true)
                        },
                        belowField = {
                            pendingAiReview?.let { review ->
                                val detected = review.extracted.locationSuggestions.preferred
                                    .ifBlank { review.extracted.location }
                                PendingAiValue(
                                    value = detected,
                                    currentValue = location,
                                    visible = ReceiptAiField.LOCATION !in review.resolvedFields,
                                    onApply = {
                                        location = detected
                                        protectAiField(
                                            ReceiptAiField.LOCATION,
                                            resolved = true,
                                        )
                                    },
                                )
                            }
                        },
                    )
                }
                ImageTextTarget(
                    pendingText = pendingImageText,
                    fieldLabel = stringResource(R.string.check_number),
                    onApply = {
                        check = it
                        protectAiField(ReceiptAiField.CHECK_NUMBER, resolved = true)
                        pendingImageText = null
                    },
                ) {
                    OutlinedTextField(
                        check,
                        {
                            check = it
                            protectAiField(ReceiptAiField.CHECK_NUMBER)
                        },
                        label = { Text(stringResource(R.string.check_number)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                pendingAiReview?.let { review ->
                    val detected = review.extracted.checkNumberSuggestions.preferred
                        .ifBlank { review.extracted.checkNumber }
                    PendingAiValue(
                        value = detected,
                        currentValue = check,
                        visible = ReceiptAiField.CHECK_NUMBER !in review.resolvedFields,
                        onApply = {
                            check = detected
                            protectAiField(ReceiptAiField.CHECK_NUMBER, resolved = true)
                        },
                    )
                }
                if (pendingImageText == null) AiSuggestionMenu(
                    currentValue = check,
                    suggestions = extractedReceipt
                        ?.checkNumberSuggestions
                        ?.toEditorSuggestions()
                        .orEmpty(),
                    onSelected = {
                        check = it
                        protectAiField(ReceiptAiField.CHECK_NUMBER, resolved = true)
                    },
                )
                ImageTextTarget(
                    pendingText = pendingImageText,
                    fieldLabel = stringResource(R.string.receipt_date),
                    onApply = {
                        occurredOn = normalizeEditorDate(it) ?: it
                        invalidDate = false
                        protectAiField(ReceiptAiField.DATE, resolved = true)
                        pendingImageText = null
                    },
                ) {
                    OutlinedTextField(
                        value = occurredOn,
                        onValueChange = {
                            occurredOn = it
                            invalidDate = false
                            protectAiField(ReceiptAiField.DATE)
                        },
                        label = { Text(stringResource(R.string.receipt_date)) },
                        placeholder = { Text(stringResource(R.string.date_example)) },
                        isError = invalidDate,
                        supportingText = if (invalidDate) {
                            { Text(stringResource(R.string.invalid_date)) }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                pendingAiReview?.let { review ->
                    val rawDetected = review.extracted.occurredOnSuggestions.preferred
                        .ifBlank { review.extracted.occurredOn }
                    val detected = normalizeEditorDate(rawDetected) ?: rawDetected
                    PendingAiValue(
                        value = detected,
                        currentValue = occurredOn,
                        visible = ReceiptAiField.DATE !in review.resolvedFields,
                        onApply = {
                            occurredOn = detected
                            invalidDate = false
                            protectAiField(ReceiptAiField.DATE, resolved = true)
                        },
                    )
                }
                if (pendingImageText == null) AiSuggestionMenu(
                    currentValue = occurredOn,
                    suggestions = extractedReceipt
                        ?.occurredOnSuggestions
                        ?.toEditorSuggestions { candidate ->
                            normalizeEditorDate(candidate) ?: candidate
                        }
                        .orEmpty(),
                    onSelected = { selected ->
                        occurredOn = normalizeEditorDate(selected) ?: selected
                        invalidDate = false
                        protectAiField(ReceiptAiField.DATE, resolved = true)
                    },
                )
                TripCurrencySelector(
                    selectedCurrencyCode = receiptCurrencyCode,
                    currencyCodes = tripCurrencies.map { it.currencyCode },
                    onCurrencySelected = { selected ->
                        receiptCurrencyCode = selected
                        invalidAmount = false
                        protectAiField(ReceiptAiField.CURRENCY, resolved = true)
                    },
                    onAddCurrencyRequested = {
                        pendingCurrencyExtraction = null
                        showCurrencyPicker = true
                    },
                )
                pendingAiReview?.let { review ->
                    val detected = review.extracted.currencyCode.trim().uppercase(Locale.ROOT)
                    PendingAiValue(
                        value = detected,
                        currentValue = receiptCurrencyCode,
                        visible = ReceiptAiField.CURRENCY !in review.resolvedFields &&
                            (tripCurrencies.any { it.currencyCode == detected } ||
                                detected in newlyConfiguredCurrencyCodes),
                        onApply = {
                            receiptCurrencyCode = detected
                            invalidAmount = false
                            protectAiField(ReceiptAiField.CURRENCY, resolved = true)
                        },
                    )
                }
                ImageTextTarget(
                    pendingText = pendingImageText,
                    fieldLabel = stringResource(R.string.amount_in_currency, receiptCurrencyCode),
                    onApply = {
                        amount = CurrencyAmount.normalizeOcrMajorText(it, receiptCurrencyCode)
                        invalidAmount = false
                        protectAiField(ReceiptAiField.AMOUNT, resolved = true)
                        pendingImageText = null
                    },
                ) {
                    OutlinedTextField(
                        amount,
                        {
                            amount = it
                            invalidAmount = false
                            protectAiField(ReceiptAiField.AMOUNT)
                        },
                        label = { Text(stringResource(R.string.amount_in_currency, receiptCurrencyCode)) },
                        placeholder = { Text(amountInputPlaceholder(receiptCurrencyCode)) },
                        isError = invalidAmount,
                        supportingText = if (invalidAmount) {
                            { Text(stringResource(R.string.invalid_amount)) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                pendingAiReview?.let { review ->
                    val detected = review.extracted.totalAmountSuggestions.preferred
                        .ifBlank { review.extracted.totalAmountText }
                    PendingAiValue(
                        value = detected,
                        currentValue = amount,
                        visible = ReceiptAiField.AMOUNT !in review.resolvedFields,
                        onApply = {
                            amount = detected
                            invalidAmount = false
                            protectAiField(ReceiptAiField.AMOUNT, resolved = true)
                        },
                    )
                }
                if (pendingImageText == null) AiSuggestionMenu(
                    currentValue = amount,
                    suggestions = extractedReceipt
                        ?.totalAmountSuggestions
                        ?.toEditorSuggestions()
                        .orEmpty(),
                    onSelected = {
                        amount = it
                        invalidAmount = false
                        protectAiField(ReceiptAiField.AMOUNT, resolved = true)
                    },
                )
                Text(
                    stringResource(R.string.receipt_items),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                items.forEachIndexed { index, item ->
                    val aiItemSuggestionSource = item.aiSourceIndex
                        ?.let { extractedReceipt?.items?.getOrNull(it) }
                        ?: pendingAiReview?.takeUnless { it.itemsDismissed }
                            ?.extracted?.items?.getOrNull(index)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.item_number, index + 1),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = {
                                        items = items.filterNot { it.id == item.id }
                                        pendingAiReview = pendingAiReview?.copy(
                                            itemsProtected = true,
                                            itemsDismissed = true,
                                        )
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.RemoveCircleOutline,
                                        contentDescription = stringResource(R.string.remove_item),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            ImageTextTarget(
                                pendingText = pendingImageText,
                                fieldLabel = stringResource(R.string.item_name_with_number, index + 1),
                                onApply = { selected ->
                                    items = items.toMutableList().also {
                                        it[index] = item.copy(name = selected)
                                    }
                                    protectAiItemField(
                                        index,
                                        ReceiptAiItemFieldKind.NAME,
                                        resolved = true,
                                    )
                                    pendingImageText = null
                                },
                            ) {
                                HistoryTextField(
                                    value = item.name,
                                    onValueChange = { value ->
                                        items = items.toMutableList().also {
                                            it[index] = item.copy(name = value)
                                        }
                                        protectAiItemField(index, ReceiptAiItemFieldKind.NAME)
                                    },
                                    label = stringResource(R.string.item_name),
                                    suggestions = itemNameSuggestions,
                                    aiSuggestions = aiItemSuggestionSource
                                        ?.let(::itemNameAiSuggestions)
                                        ?: item.nameAiSuggestions,
                                    onSuggestionSelected = {
                                        protectAiItemField(
                                            index,
                                            ReceiptAiItemFieldKind.NAME,
                                            resolved = true,
                                        )
                                    },
                                    belowField = {
                                        pendingAiReview?.takeUnless { it.itemsDismissed }
                                            ?.let { review ->
                                                val detected = review.extracted.items
                                                    .getOrNull(index)
                                                    ?.let { detectedItem ->
                                                        formatExtractedItemName(
                                                            detectedItem.quantityText,
                                                            detectedItem.name,
                                                        )
                                                    }
                                                    .orEmpty()
                                                PendingAiValue(
                                                    value = detected,
                                                    currentValue = item.name,
                                                    visible = ReceiptAiItemField(
                                                        index,
                                                        ReceiptAiItemFieldKind.NAME,
                                                    ) !in review.resolvedItemFields,
                                                    onApply = {
                                                        items = items.toMutableList().also {
                                                            it[index] = item.copy(name = detected)
                                                        }
                                                        protectAiItemField(
                                                            index,
                                                            ReceiptAiItemFieldKind.NAME,
                                                            resolved = true,
                                                        )
                                                    },
                                                )
                                            }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            ImageTextTarget(
                                pendingText = pendingImageText,
                                fieldLabel = stringResource(R.string.item_amount_with_number, index + 1),
                                onApply = { selected ->
                                    items = items.toMutableList().also {
                                        it[index] = item.copy(
                                            amountText = CurrencyAmount.normalizeOcrMajorText(
                                                selected,
                                                receiptCurrencyCode,
                                            ),
                                        )
                                    }
                                    invalidAmount = false
                                    protectAiItemField(
                                        index,
                                        ReceiptAiItemFieldKind.AMOUNT,
                                        resolved = true,
                                    )
                                    pendingImageText = null
                                },
                            ) {
                                OutlinedTextField(
                                    value = item.amountText,
                                    onValueChange = { value ->
                                        items = items.toMutableList().also {
                                            it[index] = item.copy(amountText = value)
                                        }
                                        invalidAmount = false
                                        protectAiItemField(index, ReceiptAiItemFieldKind.AMOUNT)
                                    },
                                    label = {
                                        Text(
                                            stringResource(
                                                R.string.item_amount,
                                                receiptCurrencyCode,
                                            ),
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            pendingAiReview?.takeUnless { it.itemsDismissed }?.let { review ->
                                val detected = review.extracted.items.getOrNull(index)?.amountText.orEmpty()
                                PendingAiValue(
                                    value = detected,
                                    currentValue = item.amountText,
                                    visible = ReceiptAiItemField(
                                        index,
                                        ReceiptAiItemFieldKind.AMOUNT,
                                    ) !in review.resolvedItemFields,
                                    onApply = {
                                        items = items.toMutableList().also {
                                            it[index] = item.copy(amountText = detected)
                                        }
                                        invalidAmount = false
                                        protectAiItemField(
                                            index,
                                            ReceiptAiItemFieldKind.AMOUNT,
                                            resolved = true,
                                        )
                                    },
                                )
                            }
                            if (pendingImageText == null) AiSuggestionMenu(
                                currentValue = item.amountText,
                                suggestions = aiItemSuggestionSource
                                    ?.amountSuggestions
                                    ?.toEditorSuggestions()
                                    ?: item.amountAiSuggestions,
                                onSelected = { value ->
                                    items = items.toMutableList().also {
                                        it[index] = item.copy(amountText = value)
                                    }
                                    invalidAmount = false
                                    protectAiItemField(
                                        index,
                                        ReceiptAiItemFieldKind.AMOUNT,
                                        resolved = true,
                                    )
                                },
                            )
                        }
                    }
                }
                pendingAiReview?.takeUnless { it.itemsDismissed }?.let { review ->
                    val additionalItems = review.extracted.items.drop(items.size)
                    if (additionalItems.isNotEmpty()) {
                        Text(
                            stringResource(R.string.additional_detected_items),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        additionalItems.forEachIndexed { offset, detectedItem ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        stringResource(R.string.item_number, items.size + offset + 1),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    Text(
                                        formatExtractedItemName(
                                            detectedItem.quantityText,
                                            detectedItem.name,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    detectedItem.amountText.takeIf(String::isNotBlank)?.let { value ->
                                        Text(value, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        items = items + EditableReceiptItem(id = nextItemId++)
                        pendingAiReview = pendingAiReview?.copy(
                            itemsProtected = true,
                            itemsDismissed = true,
                        )
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_item))
                }
                if (itemSumMinor > 0) {
                    val difference = receiptMinor?.minus(itemSumMinor)
                    Text(
                        text = when {
                            receiptMinor == null -> stringResource(
                                R.string.item_sum_used_as_total,
                                formatMinor(itemSumMinor, receiptCurrencyCode),
                            )
                            difference == 0L -> stringResource(
                                R.string.item_sum_matches,
                                formatMinor(itemSumMinor, receiptCurrencyCode),
                            )
                            else -> stringResource(
                                R.string.item_sum_differs,
                                formatMinor(itemSumMinor, receiptCurrencyCode),
                                formatMinor(
                                    kotlin.math.abs(requireNotNull(difference)),
                                    receiptCurrencyCode,
                                ),
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (difference == null || difference == 0L) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = addTip, onCheckedChange = { addTip = it })
                    Column {
                        Text(stringResource(R.string.add_default_tip))
                        Text(
                            formatMinor(tipPreviewMinor, tipPreviewCurrencyCode),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
    }
    if (showFullscreenImage && imageUri != null) {
        FullscreenReceiptImage(
            imageUri = imageUri,
            onDismiss = { showFullscreenImage = false },
        )
    }
    if (showTextSelection && imageUri != null && selectablePage != null) {
        SpatialTextSelectionDialog(
            imageUri = Uri.parse(imageUri),
            ocrPage = selectablePage,
            onConfirm = { selectedText ->
                pendingImageText = selectedText.trim().takeIf(String::isNotEmpty)
                showTextSelection = false
            },
            onDismiss = { showTextSelection = false },
        )
    }
    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            selectedCurrencyCode = receiptCurrencyCode,
            preferredCurrencyCodes = tripCurrencies.map { it.currencyCode },
            recentCurrencyCodes = recentCurrencyCodes,
            excludedCurrencyCodes = tripCurrencies.mapTo(hashSetOf()) { it.currencyCode },
            onCurrencySelected = { entry ->
                showCurrencyPicker = false
                pendingNewCurrencyCode = entry.code
                pendingCurrencyRate = ""
                pendingCurrencyDaily = false
                pendingCurrencyRateInvalid = false
                onLookupRate(trip.homeCurrencyCode, entry.code)
            },
            onDismiss = { showCurrencyPicker = false },
        )
    }
    pendingNewCurrencyCode?.let { newCode ->
        AlertDialog(
            onDismissRequest = {
                val extracted = pendingCurrencyExtraction
                val request = pendingCurrencyAnalysisRequest
                if (extracted != null && request != null) {
                    queueAiReview(extracted, request, suppressCurrency = true)
                }
                pendingCurrencyExtraction = null
                pendingCurrencyAnalysisRequest = null
                pendingNewCurrencyCode = null
            },
            title = { Text(stringResource(R.string.trip_currency_add_other)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.trip_currency_rate_formula, trip.homeCurrencyCode, newCode))
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
                TextButton(onClick = {
                    val extracted = pendingCurrencyExtraction
                    val request = pendingCurrencyAnalysisRequest
                    if (extracted != null && request != null) {
                        queueAiReview(extracted, request, suppressCurrency = true)
                    }
                    pendingCurrencyExtraction = null
                    pendingCurrencyAnalysisRequest = null
                    pendingNewCurrencyCode = null
                }) {
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
                        newlyConfiguredCurrencyCodes = newlyConfiguredCurrencyCodes + newCode
                        val extracted = pendingCurrencyExtraction
                        val request = pendingCurrencyAnalysisRequest
                        val mayApplyAutomatically = extracted != null && request != null &&
                            shouldAutoApplyReceiptAnalysis(request, currentBaseline())
                        if (extracted != null && request != null) {
                            if (mayApplyAutomatically) {
                                receiptCurrencyCode = newCode
                                applyExtractedReceipt(extracted)
                            } else {
                                queueAiReview(extracted, request)
                            }
                        } else {
                            receiptCurrencyCode = newCode
                        }
                        pendingCurrencyExtraction = null
                        pendingCurrencyAnalysisRequest = null
                        pendingNewCurrencyCode = null
                    }
                }) { Text(stringResource(R.string.trip_currency_add)) }
            },
        )
    }
}

@Composable
private fun ReceiptEditorImageSection(
    imageUri: String?,
    onPreviewImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseImage: () -> Unit,
    onBrowseFolders: () -> Unit,
    onOpenImage: () -> Unit,
    onAnalyzeImage: () -> Unit,
    onAnalyzeLocally: () -> Unit,
) {
    Text(
        stringResource(R.string.receipt_image),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    if (imageUri != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReceiptThumbnail(
                imageUri = imageUri,
                modifier = Modifier
                    .size(84.dp)
                    .clickable(onClick = onPreviewImage),
            )
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    stringResource(R.string.image_linked),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.tap_image_to_enlarge),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onOpenImage,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.edit))
                }
            }
        }
    } else {
        Text(
            stringResource(R.string.no_image_linked),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onTakePhoto, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.take_photo))
        }
        OutlinedButton(onClick = onChooseImage, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.choose_image))
        }
    }
    TextButton(onClick = onBrowseFolders, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.FolderOpen, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.browse_folders))
    }
    if (imageUri != null) {
        FilledTonalButton(
            onClick = onAnalyzeImage,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.analyze_image))
        }
        OutlinedButton(
            onClick = onAnalyzeLocally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.select_text_in_image))
        }
    }
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
}

@Composable
private fun LocalOcrStatus(
    state: LocalOcrState,
    imageUri: String?,
    onClose: () -> Unit,
) {
    when (state) {
        is LocalOcrState.Loading -> if (state.imageUri == imageUri) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.local_ocr_running))
            }
        }
        is LocalOcrState.Error -> if (state.imageUri == imageUri) {
            Column {
                Text(
                    stringResource(R.string.local_ocr_failed, state.message),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.close))
                }
            }
        }
        is LocalOcrState.Success -> if (state.imageUri == imageUri) {
            if (state.page.text.isBlank()) {
                Text(
                    stringResource(R.string.local_ocr_empty),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        LocalOcrState.Idle -> Unit
    }
}

@Composable
private fun PendingImageTextBanner(
    selectedText: String,
    onCancel: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.choose_target_field),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                selectedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.cancel_selection))
            }
        }
    }
}

@Composable
private fun AiReviewActions(
    itemDecision: Boolean,
    onApply: () -> Unit,
    onKeep: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.ai_analysis_complete),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (itemDecision) R.string.apply_detected_items
                    else R.string.apply_detected_values,
                ),
            )
        }
        OutlinedButton(onClick = onKeep, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (itemDecision) R.string.keep_my_items
                    else R.string.keep_my_entries,
                ),
            )
        }
    }
}

@Composable
private fun PendingAiValue(
    value: String,
    currentValue: String,
    visible: Boolean,
    onApply: () -> Unit,
) {
    if (!visible || value.isBlank() || value.trim().equals(currentValue.trim(), ignoreCase = true)) return
    val applyDescription = stringResource(R.string.apply_detected_value, value)
    Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = applyDescription, onClick = onApply)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun ImageTextTarget(
    pendingText: String?,
    fieldLabel: String,
    onApply: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val applyLabel = stringResource(R.string.apply_selected_text_to, fieldLabel)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (pendingText != null) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                },
            )
            .then(
                if (pendingText != null) {
                    Modifier.clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = applyLabel
                        onClick(action = {
                            onApply(pendingText)
                            true
                        })
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        content()
        pendingText?.let { selectedText ->
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .clickable(
                        onClickLabel = applyLabel,
                        onClick = { onApply(selectedText) },
                    ),
            )
        }
    }
}

@Composable
private fun HistoryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    aiSuggestions: List<EditorSuggestion> = emptyList(),
    onSuggestionSelected: ((String) -> Unit)? = null,
    belowField: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
    }
    LaunchedEffect(value) {
        if (fieldValue.text != value) {
            fieldValue = TextFieldValue(value, selection = TextRange(value.length))
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { updated ->
                fieldValue = updated
                onValueChange(updated.text)
            },
            label = { Text(label) },
            singleLine = false,
            minLines = 1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        belowField?.invoke()
        FieldSuggestionMenu(
            currentValue = fieldValue.text,
            suggestions = aiSuggestions + suggestions.map {
                EditorSuggestion(value = it, source = SuggestionSource.HISTORY)
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            onSelected = { suggestion ->
                fieldValue = TextFieldValue(
                    suggestion,
                    selection = TextRange(suggestion.length),
                )
                onValueChange(suggestion)
                onSuggestionSelected?.invoke(suggestion)
            },
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun AiSuggestionMenu(
    currentValue: String,
    suggestions: List<EditorSuggestion>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    FieldSuggestionMenu(
        currentValue = currentValue,
        suggestions = suggestions,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        onSelected = onSelected,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FieldSuggestionMenu(
    currentValue: String,
    suggestions: List<EditorSuggestion>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val alternatives = suggestions
        .filter { it.value.isNotBlank() && !it.value.equals(currentValue, ignoreCase = true) }
        .distinctBy { it.value.trim().lowercase(Locale.ROOT) }
        .take(8)
    if (alternatives.isEmpty()) return

    Box(modifier = modifier) {
        TextButton(
            onClick = { onExpandedChange(true) },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(Icons.Default.History, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (alternatives.any { it.source != SuggestionSource.HISTORY }) {
                    stringResource(R.string.field_suggestions, alternatives.size)
                } else {
                    stringResource(R.string.previous_values, alternatives.size)
                },
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            alternatives.forEach { suggestion ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(suggestion.value)
                            Text(
                                when (suggestion.source) {
                                    SuggestionSource.AI -> stringResource(
                                        R.string.ai_suggestion_source,
                                        suggestion.certainty.localizedLabel(),
                                    )
                                    SuggestionSource.HISTORY -> stringResource(R.string.history_suggestion_source)
                                    SuggestionSource.LOCAL_OCR -> stringResource(R.string.local_ocr_suggestion_source)
                                    SuggestionSource.MANUAL_IMAGE -> stringResource(R.string.image_selection_suggestion_source)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            suggestion.evidence
                                .takeIf { it.isNotBlank() && !it.equals(suggestion.value, ignoreCase = true) }
                                ?.let { evidence ->
                                    Text(
                                        stringResource(R.string.ai_suggestion_evidence, evidence),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                        }
                    },
                    onClick = {
                        onSelected(suggestion.value)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

@Composable
private fun AiSuggestionCertainty?.localizedLabel(): String = stringResource(
    when (this) {
        AiSuggestionCertainty.HIGH -> R.string.ai_certainty_high
        AiSuggestionCertainty.MEDIUM -> R.string.ai_certainty_medium
        AiSuggestionCertainty.LOW -> R.string.ai_certainty_low
        null -> R.string.ai_certainty_unknown
    },
)

@Composable
private fun ScrollableEditorDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    destructiveAction: (@Composable () -> Unit)? = null,
    contextualActions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    title,
                    modifier = Modifier.padding(24.dp, 22.dp, 24.dp, 14.dp),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content,
                )
                contextualActions?.let { actions ->
                    HorizontalDivider()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        actions()
                    }
                }
                destructiveAction?.let { action ->
                    HorizontalDivider()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        action()
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp, 8.dp, 16.dp, 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = onSave) { Text(stringResource(R.string.save)) }
                }
            }
        }
    }
}

private data class EditableReceiptItem(
    val id: Long,
    val name: String = "",
    val amountText: String = "",
    val aiSourceIndex: Int? = null,
    val nameAiSuggestions: List<EditorSuggestion> = emptyList(),
    val amountAiSuggestions: List<EditorSuggestion> = emptyList(),
)

internal data class ReceiptEditorBaseline(
    val location: String,
    val checkNumber: String,
    val amountText: String,
    val currencyCode: String,
    val occurredOn: String,
    val items: List<Pair<String, String>>,
)

internal enum class ReceiptAiField {
    LOCATION,
    CHECK_NUMBER,
    DATE,
    CURRENCY,
    AMOUNT,
}

private enum class ReceiptAiItemFieldKind {
    NAME,
    AMOUNT,
}

private data class ReceiptAiItemField(
    val index: Int,
    val kind: ReceiptAiItemFieldKind,
)

internal data class ReceiptAnalysisRequest(
    val baseline: ReceiptEditorBaseline,
    val wasVirgin: Boolean,
)

internal fun shouldAutoApplyReceiptAnalysis(
    request: ReceiptAnalysisRequest,
    currentBaseline: ReceiptEditorBaseline,
): Boolean = request.wasVirgin && request.baseline == currentBaseline

internal fun changedReceiptAiFields(
    requestBaseline: ReceiptEditorBaseline,
    currentBaseline: ReceiptEditorBaseline,
    suppressCurrency: Boolean = false,
): Set<ReceiptAiField> = buildSet {
    if (requestBaseline.location != currentBaseline.location) add(ReceiptAiField.LOCATION)
    if (requestBaseline.checkNumber != currentBaseline.checkNumber) add(ReceiptAiField.CHECK_NUMBER)
    if (requestBaseline.amountText != currentBaseline.amountText) add(ReceiptAiField.AMOUNT)
    if (requestBaseline.currencyCode != currentBaseline.currencyCode || suppressCurrency) {
        add(ReceiptAiField.CURRENCY)
    }
    if (requestBaseline.occurredOn != currentBaseline.occurredOn) add(ReceiptAiField.DATE)
}

private data class PendingReceiptAiReview(
    val extracted: ExtractedReceipt,
    val protectedFields: Set<ReceiptAiField> = emptySet(),
    val resolvedFields: Set<ReceiptAiField> = emptySet(),
    val itemsProtected: Boolean = false,
    val protectedItemFields: Set<ReceiptAiItemField> = emptySet(),
    val resolvedItemFields: Set<ReceiptAiItemField> = emptySet(),
    val itemsDismissed: Boolean = false,
)

private data class EditorSuggestion(
    val value: String,
    val source: SuggestionSource,
    val evidence: String = "",
    val certainty: AiSuggestionCertainty? = null,
)

private fun ExtractedFieldSuggestions.toEditorSuggestions(
    transformValue: (String) -> String = { it },
): List<EditorSuggestion> = candidates.map { candidate ->
    EditorSuggestion(
        value = transformValue(candidate.value),
        source = candidate.source,
        evidence = candidate.evidenceText,
        certainty = candidate.certainty,
    )
}

private fun itemNameAiSuggestions(item: ExtractedItem): List<EditorSuggestion> {
    val preferredQuantity = item.quantitySuggestions.preferred.ifBlank { item.quantityText }
    val nameVariants = item.nameSuggestions.toEditorSuggestions { candidateName ->
        formatExtractedItemName(preferredQuantity, candidateName)
    }
    val quantityVariants = item.quantitySuggestions.toEditorSuggestions { candidateQuantity ->
        formatExtractedItemName(candidateQuantity, item.nameSuggestions.preferred.ifBlank { item.name })
    }
    return (nameVariants + quantityVariants)
        .distinctBy { it.value.trim().lowercase(Locale.ROOT) }
}

private fun ExtractedReceipt.imageTextCandidates() = buildList {
    addAll(locationSuggestions.candidates)
    addAll(checkNumberSuggestions.candidates)
    addAll(totalAmountSuggestions.candidates)
    addAll(occurredOnSuggestions.candidates)
    items.forEach { item ->
        addAll(item.quantitySuggestions.candidates)
        addAll(item.nameSuggestions.candidates)
        addAll(item.amountSuggestions.candidates)
    }
}

internal fun formatExtractedItemName(quantityText: String, name: String): String {
    val quantity = quantityText.trim()
    val cleanName = name.trim()
    val normalizedQuantity = runCatching {
        BigDecimal(quantity.replace(',', '.')).stripTrailingZeros().toPlainString()
    }.getOrDefault(quantity)
    val quantityPattern = listOf(quantity, normalizedQuantity)
        .filter(String::isNotBlank)
        .distinct()
        .joinToString("|") { Regex.escape(it) }
    val quantityPrefix = if (quantityPattern.isBlank()) null else Regex(
        pattern = "^(?:$quantityPattern)(?:\\s*[x×*]\\s*|\\s+)",
        option = RegexOption.IGNORE_CASE,
    )
    val nameWithoutRepeatedQuantity = quantityPrefix
        ?.find(cleanName)
        ?.takeIf { it.range.first == 0 }
        ?.let { cleanName.removeRange(it.range).trim() }
        ?.takeIf(String::isNotBlank)
    return when {
        quantity.isBlank() -> cleanName
        cleanName.isBlank() -> quantity
        nameWithoutRepeatedQuantity != null -> "$quantity × $nameWithoutRepeatedQuantity"
        else -> "$quantity × $cleanName"
    }
}

private fun formatInputMinor(minor: Long, currencyCode: String): String = BigDecimal.valueOf(minor)
    .movePointLeft(CurrencyAmount.fractionDigits(currencyCode))
    .stripTrailingZeros()
    .toPlainString()

private fun amountInputPlaceholder(currencyCode: String): String {
    val fractionDigits = CurrencyAmount.fractionDigits(currencyCode)
    if (fractionDigits == 0) return "0"
    val separator = DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator
    return "0$separator${"0".repeat(fractionDigits)}"
}

private val editorDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")

private fun formatEditorDate(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()
    .format(editorDateFormatter)

private fun normalizeEditorDate(value: String): String? = runCatching {
    LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE).format(editorDateFormatter)
}.getOrNull()

private fun formatMinor(minor: Long, currencyCode: String): String =
    CurrencyAmount.formatMinor(minor, currencyCode)

private fun formatDate(epochMillis: Long): String = DateTimeFormatter
    .ofPattern("dd.MM., HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(epochMillis))

private fun formatRateDate(epochMillis: Long): String = DateTimeFormatter
    .ofPattern("dd.MM.yyyy, HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(epochMillis))

private fun appendToken(existing: String, token: String): String =
    listOf(existing.trim(), token.trim()).filter(String::isNotEmpty).joinToString(" ")

private fun formatTokenLimit(value: Int): String = when {
    value >= 1_000_000 -> "${value / 1_000_000}M"
    value >= 1_000 -> "${value / 1_000}k"
    else -> value.toString()
}
