package de.shakie.billcheck

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.shakie.billcheck.data.ReceiptEntity
import de.shakie.billcheck.data.ReceiptImageStorage
import de.shakie.billcheck.data.ReceiptWithItems
import de.shakie.billcheck.data.TripEntity
import de.shakie.billcheck.domain.MoneyCalculator
import de.shakie.billcheck.ui.MainUiState
import de.shakie.billcheck.ui.MainViewModel
import de.shakie.billcheck.ui.OpenImageDocumentContract
import de.shakie.billcheck.ui.ReceiptItemDraft
import de.shakie.billcheck.ui.ReceiptImageReview
import de.shakie.billcheck.ui.ReceiptThumbnail
import de.shakie.billcheck.ui.ExchangeRateLookupState
import de.shakie.billcheck.ui.AiExtractionState
import de.shakie.billcheck.ui.LocalOcrState
import de.shakie.billcheck.ui.GeminiModelsState
import de.shakie.billcheck.ui.TransferState
import de.shakie.billcheck.ui.ReconciliationManagerDialog
import de.shakie.billcheck.data.ExportFormat
import de.shakie.billcheck.data.ImportPreview
import de.shakie.billcheck.ui.theme.BillCheckTheme
import androidx.compose.foundation.isSystemInDarkTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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
                    onDarkThemeChange = { useDarkTheme ->
                        darkTheme = useDarkTheme
                        preferences.edit().putBoolean("dark_theme", useDarkTheme).apply()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillCheckApp(
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exchangeRateLookup by viewModel.exchangeRateLookup.collectAsStateWithLifecycle()
    val candidateSelection by viewModel.candidateSelection.collectAsStateWithLifecycle()
    val aiSettings by viewModel.aiSettings.collectAsStateWithLifecycle()
    val aiExtraction by viewModel.aiExtraction.collectAsStateWithLifecycle()
    val localOcr by viewModel.localOcr.collectAsStateWithLifecycle()
    val geminiModels by viewModel.geminiModels.collectAsStateWithLifecycle()
    val transferState by viewModel.transfer.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val imageStorage = remember { ReceiptImageStorage(context) }
    var showCreateTrip by remember { mutableStateOf(false) }
    var editingTrip by remember { mutableStateOf<TripEntity?>(null) }
    var showManualReceipt by remember { mutableStateOf(false) }
    var editingReceipt by remember { mutableStateOf<ReceiptWithItems?>(null) }
    var showAppMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showReconciliations by remember { mutableStateOf(false) }
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var draftReceiptImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
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
                onOpenReceiptImage = { receipt ->
                    imageTargetReceiptId = receipt.id
                    imageTargetHadLinkedImage = true
                    pendingImageUriString = receipt.imageUri
                },
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
            exchangeRateLookup = exchangeRateLookup,
            onLookupRate = viewModel::requestExchangeRate,
            onDismiss = {
                showCreateTrip = false
                viewModel.clearExchangeRateLookup()
            },
            onSave = { name, currency, rate, useDailyRate, tipMinor, tipCurrency ->
                viewModel.createTrip(name, currency, rate, useDailyRate, tipMinor, tipCurrency)
                showCreateTrip = false
                viewModel.clearExchangeRateLookup()
                true
            },
        )
    }

    editingTrip?.let { trip ->
        TripEditorDialog(
            existing = trip,
            suggestedName = stringResource(R.string.trip_default_name),
            exchangeRateLookup = exchangeRateLookup,
            onLookupRate = viewModel::requestExchangeRate,
            onDismiss = {
                editingTrip = null
                viewModel.clearExchangeRateLookup()
            },
            onSave = { name, currency, rate, useDailyRate, tipMinor, tipCurrency ->
                viewModel.updateTrip(
                    existing = trip,
                    name = name,
                    currencyCode = currency,
                    exchangeRate = rate,
                    useDailyRate = useDailyRate,
                    defaultTipMinor = tipMinor,
                    defaultTipCurrencyCode = tipCurrency,
                ).also { saved ->
                    if (saved) {
                        editingTrip = null
                        viewModel.clearExchangeRateLookup()
                    }
                }
            },
        )
    }

    if (showSettings) {
        AppearanceSettingsDialog(
            darkTheme = darkTheme,
            onDarkThemeChange = onDarkThemeChange,
            aiSettings = aiSettings,
            geminiModels = geminiModels,
            onLoadGeminiModels = viewModel::loadGeminiModels,
            onSaveAiSettings = viewModel::saveAiSettings,
            onClearAiApiKey = viewModel::clearAiApiKey,
            onDismiss = { showSettings = false },
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
                    draftReceiptImageUriString?.let(viewModel::analyzeReceipt)
                },
                onAnalyzeLocally = {
                    draftReceiptImageUriString?.let(viewModel::analyzeLocally)
                },
                onClearLocalOcr = viewModel::clearLocalOcr,
                onDismiss = {
                    showManualReceipt = false
                    draftReceiptImageUriString = null
                    viewModel.clearLocalOcr()
                },
                onSave = { location, check, amount, tip, itemDrafts ->
                    viewModel.addReceipt(
                        location,
                        check,
                        amount,
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
                    existing.receipt.imageUri?.let(viewModel::analyzeReceipt)
                },
                onAnalyzeLocally = {
                    existing.receipt.imageUri?.let(viewModel::analyzeLocally)
                },
                onClearLocalOcr = viewModel::clearLocalOcr,
                onDismiss = {
                    editingReceipt = null
                    viewModel.clearLocalOcr()
                },
                onSave = { location, check, amount, tip, itemDrafts ->
                    viewModel.updateReceipt(
                        existing = existing,
                        location = location,
                        checkNumber = check,
                        foreignAmountText = amount,
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
        AiExtractionState.MissingApiKey -> AlertDialog(
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
        is AiExtractionState.ReceiptSuccess -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.image_analysis)) },
            text = { Text(stringResource(R.string.ai_result_ready)) },
            confirmButton = {
                TextButton(onClick = viewModel::clearAiExtraction) { Text(stringResource(R.string.ok)) }
            },
        )
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
                defaultCurrencyCode = trip.foreignCurrencyCode,
                candidateSelection = candidateSelection,
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
            )
        }
    }
}

@Composable
private fun ExportDataDialog(
    trips: List<TripEntity>,
    onDismiss: () -> Unit,
    onExport: (ExportFormat, Set<String>) -> Unit,
) {
    var format by remember { mutableStateOf(ExportFormat.BILL_CHECK) }
    var selectedIds by remember(trips) { mutableStateOf(trips.mapTo(mutableSetOf()) { it.id }) }
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
        mutableStateOf(preview.trips.mapTo(mutableSetOf()) { it.sourceId })
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
    aiSettings: de.shakie.billcheck.data.AiSettings,
    geminiModels: GeminiModelsState,
    onLoadGeminiModels: () -> Unit,
    onSaveAiSettings: (String?, String) -> Unit,
    onClearAiApiKey: () -> Unit,
    onDismiss: () -> Unit,
) {
    var apiKey by remember { mutableStateOf("") }
    var model by remember(aiSettings.model) { mutableStateOf(aiSettings.model) }
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
                    stringResource(R.string.ai_recognition),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = stringResource(R.string.gemini),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.ai_provider)) },
                    modifier = Modifier.fillMaxWidth(),
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
                Spacer(Modifier.height(24.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp, 8.dp, 16.dp, 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = {
                        onSaveAiSettings(apiKey.takeIf(String::isNotBlank), model)
                        onDismiss()
                    }) { Text(stringResource(R.string.save)) }
                }
            }
        }
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
    onOpenReceiptImage: (ReceiptEntity) -> Unit,
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
                ReceiptCard(receipt, onDeleteReceipt, onOpenReceiptImage, onEditReceipt)
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
                "${state.roundedEuro} €",
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
                    value = formatEuroCents(state.exactEuroCents),
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
    onDelete: (ReceiptEntity) -> Unit,
    onOpenImage: (ReceiptEntity) -> Unit,
    onEdit: (ReceiptWithItems) -> Unit,
) {
    val receipt = receiptWithItems.receipt
    val exactCents = MoneyCalculator.exactEuroCents(receipt)
    val rounded = MoneyCalculator.roundedUpEuro(exactCents)
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
                        .clickable { onOpenImage(receipt) },
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
                    formatMinor(receipt.foreignAmountMinor, receipt.foreignCurrencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(
                        R.string.receipt_exchange_rate,
                        receipt.exchangeRate,
                        receipt.foreignCurrencyCode,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                Text("$rounded €", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    formatEuroCents(exactCents),
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
    suggestedName: String,
    exchangeRateLookup: ExchangeRateLookupState,
    onLookupRate: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, Long, String) -> Boolean,
) {
    val stateKey = existing?.id
    var name by remember(stateKey) { mutableStateOf(existing?.name ?: suggestedName) }
    var currency by remember(stateKey) { mutableStateOf(existing?.foreignCurrencyCode ?: "EGP") }
    var rate by remember(stateKey) { mutableStateOf(existing?.defaultExchangeRate ?: "55,5") }
    var tip by remember(stateKey) {
        mutableStateOf(existing?.defaultTipMinor?.let(::formatInputMinor) ?: "1,00")
    }
    var tipCurrency by remember(stateKey) {
        mutableStateOf(existing?.defaultTipCurrencyCode ?: "EUR")
    }
    var rateWasEdited by remember(stateKey) { mutableStateOf(false) }
    var useDailyRate by remember(stateKey) {
        mutableStateOf(existing?.exchangeRateMode == "DAILY")
    }
    var previousCurrency by remember(stateKey) { mutableStateOf(currency) }
    var invalidRate by remember(stateKey) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(currency) {
        if (currency.length == 3 && (existing == null || currency != previousCurrency)) {
            rateWasEdited = false
            delay(500)
            onLookupRate(currency)
        }
        previousCurrency = currency
    }
    LaunchedEffect(exchangeRateLookup) {
        val success = exchangeRateLookup as? ExchangeRateLookupState.Success
        if (success?.quote?.targetCurrencyCode == currency && !rateWasEdited) {
            rate = success.quote.foreignPerEuro
        }
    }

    ScrollableEditorDialog(
        title = stringResource(if (existing == null) R.string.create_trip else R.string.edit_trip),
        onDismiss = onDismiss,
        onSave = {
            val saved = onSave(
                name,
                currency.ifBlank { "EGP" },
                rate,
                useDailyRate,
                MainViewModel.parseMinor(tip) ?: 100,
                tipCurrency.ifBlank { "EUR" },
            )
            invalidRate = !saved
        },
    ) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.trip_name)) })
                OutlinedTextField(currency, { currency = it.uppercase().take(3) }, label = { Text(stringResource(R.string.foreign_currency)) })
                OutlinedTextField(
                    rate,
                    {
                        rate = it
                        rateWasEdited = true
                        invalidRate = false
                    },
                    label = { Text(stringResource(R.string.exchange_rate)) },
                    placeholder = { Text(stringResource(R.string.rate_example)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = invalidRate,
                    trailingIcon = {
                        if (exchangeRateLookup is ExchangeRateLookupState.Loading &&
                            exchangeRateLookup.target == currency
                        ) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { onLookupRate(currency) }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.refresh_rate),
                                )
                            }
                        }
                    },
                    supportingText = {
                        when (exchangeRateLookup) {
                            is ExchangeRateLookupState.Success -> if (
                                exchangeRateLookup.quote.targetCurrencyCode == currency
                            ) {
                                Text(
                                    stringResource(
                                        if (exchangeRateLookup.quote.fromCache) {
                                            R.string.exchange_rate_cached
                                        } else {
                                            R.string.exchange_rate_updated
                                        },
                                        formatRateDate(exchangeRateLookup.quote.updatedAt),
                                    ),
                                )
                            }
                            is ExchangeRateLookupState.Error -> if (
                                exchangeRateLookup.target == currency
                            ) {
                                Text(
                                    stringResource(R.string.rate_lookup_failed),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            else -> Unit
                        }
                    },
                )
                TextButton(
                    onClick = { uriHandler.openUri("https://www.exchangerate-api.com") },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        stringResource(R.string.rate_attribution),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { useDailyRate = !useDailyRate }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.daily_exchange_rate))
                        Text(
                            stringResource(R.string.daily_exchange_rate_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(if (useDailyRate) R.string.switch_on else R.string.switch_off),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Switch(
                            checked = useDailyRate,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
                OutlinedTextField(
                    tip,
                    { tip = it },
                    label = { Text(stringResource(R.string.default_tip)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(tipCurrency, { tipCurrency = it.uppercase().take(3) }, label = { Text(stringResource(R.string.tip_currency)) })
    }
}

@Composable
private fun ReceiptEditorDialog(
    trip: TripEntity,
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
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, List<ReceiptItemDraft>) -> Boolean,
) {
    val stateKey = existing?.receipt?.id
    var location by remember(stateKey) { mutableStateOf(existing?.receipt?.location.orEmpty()) }
    var check by remember(stateKey) { mutableStateOf(existing?.receipt?.checkNumber.orEmpty()) }
    var amount by remember(stateKey) {
        mutableStateOf(existing?.receipt?.foreignAmountMinor?.let(::formatInputMinor).orEmpty())
    }
    var addTip by remember(stateKey) {
        mutableStateOf(existing?.receipt?.tipMinor?.let { it > 0 } ?: trip.defaultTipSelected)
    }
    var invalid by remember(stateKey) { mutableStateOf(false) }
    val initialItems = existing?.items
        ?.sortedBy { it.sortPosition }
        ?.mapIndexed { index, item ->
            EditableReceiptItem(
                id = index.toLong(),
                name = item.name,
                amountText = formatInputMinor(item.amountMinor),
            )
        }
        .orEmpty()
        .ifEmpty { listOf(EditableReceiptItem(id = 0)) }
    var nextItemId by remember(stateKey) { mutableStateOf(initialItems.size.toLong()) }
    var items by remember(stateKey) { mutableStateOf(initialItems) }
    var ocrTarget by remember(stateKey) { mutableStateOf("LOCATION") }
    val itemSumMinor = items.mapNotNull { MainViewModel.parseMinor(it.amountText) }.sum()
    val receiptMinor = MainViewModel.parseMinor(amount)

    LaunchedEffect(aiExtraction) {
        val extracted = (aiExtraction as? AiExtractionState.ReceiptSuccess)
            ?.takeIf { it.imageUri == imageUri }
            ?.receipt
            ?: return@LaunchedEffect
        location = extracted.location
        check = extracted.checkNumber
        amount = extracted.totalAmountText
        if (extracted.items.isNotEmpty()) {
            items = extracted.items.mapIndexed { index, item ->
                EditableReceiptItem(index.toLong(), item.name, item.amountText)
            }
            nextItemId = items.size.toLong()
        }
        invalid = false
    }

    if (!visible) return

    ScrollableEditorDialog(
        title = stringResource(if (existing == null) R.string.add_receipt else R.string.edit_receipt),
        onDismiss = onDismiss,
        onSave = {
            invalid = !onSave(
                location,
                check,
                amount,
                addTip,
                items.map { ReceiptItemDraft(it.name, it.amountText) },
            )
        },
    ) {
                ReceiptEditorImageSection(
                    imageUri = imageUri,
                    onTakePhoto = onTakePhoto,
                    onChooseImage = onChooseImage,
                    onBrowseFolders = onBrowseFolders,
                    onOpenImage = onOpenImage,
                    onAnalyzeImage = onAnalyzeImage,
                    onAnalyzeLocally = onAnalyzeLocally,
                )
                LocalOcrHelper(
                    state = localOcr,
                    imageUri = imageUri,
                    target = ocrTarget,
                    itemCount = items.size,
                    onTargetChange = { ocrTarget = it },
                    onToken = { token ->
                        when {
                            ocrTarget == "LOCATION" -> location = appendToken(location, token)
                            ocrTarget == "CHECK" -> check = appendToken(check, token)
                            ocrTarget == "AMOUNT" -> amount = normalizeOcrAmount(token)
                            ocrTarget.startsWith("ITEM_") -> {
                                val index = ocrTarget.removePrefix("ITEM_").toIntOrNull()
                                if (index != null && index in items.indices) {
                                    items = items.toMutableList().also {
                                        it[index] = it[index].copy(name = appendToken(it[index].name, token))
                                    }
                                }
                            }
                        }
                    },
                    onClose = onClearLocalOcr,
                )
                HistoryTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = stringResource(R.string.location),
                    suggestions = locationSuggestions,
                )
                OutlinedTextField(check, { check = it }, label = { Text(stringResource(R.string.check_number)) })
                OutlinedTextField(
                    amount,
                    {
                        amount = it
                        invalid = false
                    },
                    label = { Text(stringResource(R.string.amount_in_currency, trip.foreignCurrencyCode)) },
                    placeholder = { Text(stringResource(R.string.amount_example)) },
                    isError = invalid,
                    supportingText = if (invalid) {
                        { Text(stringResource(R.string.invalid_amount)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text(
                    stringResource(R.string.receipt_items),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                items.forEachIndexed { index, item ->
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
                                    onClick = { items = items.filterNot { it.id == item.id } },
                                ) {
                                    Icon(
                                        Icons.Default.RemoveCircleOutline,
                                        contentDescription = stringResource(R.string.remove_item),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            HistoryTextField(
                                value = item.name,
                                onValueChange = { value ->
                                    items = items.toMutableList().also {
                                        it[index] = item.copy(name = value)
                                    }
                                },
                                label = stringResource(R.string.item_name),
                                suggestions = itemNameSuggestions,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = item.amountText,
                                onValueChange = { value ->
                                    items = items.toMutableList().also {
                                        it[index] = item.copy(amountText = value)
                                    }
                                    invalid = false
                                },
                                label = {
                                    Text(
                                        stringResource(
                                            R.string.item_amount,
                                            trip.foreignCurrencyCode,
                                        ),
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        items = items + EditableReceiptItem(id = nextItemId++)
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
                                formatMinor(itemSumMinor, trip.foreignCurrencyCode),
                            )
                            difference == 0L -> stringResource(
                                R.string.item_sum_matches,
                                formatMinor(itemSumMinor, trip.foreignCurrencyCode),
                            )
                            else -> stringResource(
                                R.string.item_sum_differs,
                                formatMinor(itemSumMinor, trip.foreignCurrencyCode),
                                formatMinor(
                                    kotlin.math.abs(requireNotNull(difference)),
                                    trip.foreignCurrencyCode,
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
                            formatMinor(trip.defaultTipMinor, trip.defaultTipCurrencyCode),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ReceiptEditorImageSection(
    imageUri: String?,
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
                .clickable(onClick = onOpenImage)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReceiptThumbnail(
                imageUri = imageUri,
                modifier = Modifier.size(84.dp),
            )
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    stringResource(R.string.image_linked),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.tap_to_review_image),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            Text(stringResource(R.string.local_text_help))
        }
    }
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalOcrHelper(
    state: LocalOcrState,
    imageUri: String?,
    target: String,
    itemCount: Int,
    onTargetChange: (String) -> Unit,
    onToken: (String) -> Unit,
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
            Text(
                stringResource(R.string.local_ocr_failed, state.message),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        is LocalOcrState.Success -> if (state.imageUri == imageUri) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.recognized_text_blocks), fontWeight = FontWeight.SemiBold)
                    if (state.tokens.isEmpty()) {
                        Text(stringResource(R.string.local_ocr_empty))
                    } else {
                        Text(stringResource(R.string.ocr_target), style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OcrTargetChip("LOCATION", stringResource(R.string.ocr_target_location), target, onTargetChange)
                            OcrTargetChip("CHECK", stringResource(R.string.ocr_target_check), target, onTargetChange)
                            OcrTargetChip("AMOUNT", stringResource(R.string.ocr_target_amount), target, onTargetChange)
                            repeat(itemCount) { index ->
                                OcrTargetChip(
                                    "ITEM_$index",
                                    stringResource(R.string.ocr_target_item, index + 1),
                                    target,
                                    onTargetChange,
                                )
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.tokens.forEach { token ->
                                AssistChip(
                                    onClick = { onToken(token.text) },
                                    label = { Text(token.text) },
                                )
                            }
                        }
                    }
                    TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.clear_ocr_blocks))
                    }
                }
            }
        }
        LocalOcrState.Idle -> Unit
    }
}

@Composable
private fun OcrTargetChip(
    key: String,
    label: String,
    selected: String,
    onSelect: (String) -> Unit,
) {
    FilterChip(
        selected = selected == key,
        onClick = { onSelect(key) },
        label = { Text(label) },
    )
}

@Composable
private fun HistoryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = if (suggestions.isNotEmpty()) {
                {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = stringResource(R.string.show_previous_values, label),
                        )
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onValueChange(suggestion)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ScrollableEditorDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
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
)

private fun formatEuroCents(cents: Long): String = formatMinor(cents, "EUR")

private fun formatInputMinor(minor: Long): String = BigDecimal.valueOf(minor, 2)
    .stripTrailingZeros()
    .toPlainString()

private fun formatMinor(minor: Long, currencyCode: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance(currencyCode)
    }.format(BigDecimal.valueOf(minor, 2))
}.getOrElse { "${BigDecimal.valueOf(minor, 2)} $currencyCode" }

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

private fun normalizeOcrAmount(token: String): String {
    val candidate = token.filter { it.isDigit() || it == ',' || it == '.' }
    if (candidate.isEmpty()) return token
    val lastComma = candidate.lastIndexOf(',')
    val lastDot = candidate.lastIndexOf('.')
    val decimalIndex = maxOf(lastComma, lastDot)
    if (decimalIndex < 0) return candidate
    val whole = candidate.substring(0, decimalIndex).filter(Char::isDigit).ifBlank { "0" }
    val decimals = candidate.substring(decimalIndex + 1).filter(Char::isDigit)
    return if (decimals.length in 1..2) "$whole.$decimals" else candidate.filter(Char::isDigit)
}

private fun formatTokenLimit(value: Int): String = when {
    value >= 1_000_000 -> "${value / 1_000_000}M"
    value >= 1_000 -> "${value / 1_000}k"
    else -> value.toString()
}
