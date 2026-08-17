package de.shakie.billcheck.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import de.shakie.billcheck.domain.ReconciliationStatus
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataTransferManager(
    private val context: Context,
    database: BillCheckDatabase,
) {
    private val dao = database.dao()
    private var pendingImport: PendingImport? = null

    suspend fun export(uri: Uri, tripIds: Set<String>, format: ExportFormat) =
        withContext(Dispatchers.IO) {
            require(tripIds.isNotEmpty()) { "Select at least one trip" }
            val transfer = buildPackage(tripIds)
            when (format) {
                ExportFormat.BILL_CHECK -> exportBackup(uri, transfer)
                ExportFormat.CSV -> writeText(uri, CsvTransferCodec.encode(transfer))
                ExportFormat.PDF -> exportPdf(uri, transfer)
            }
        }

    suspend fun previewImport(uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        val pending = runCatching {
            PendingImport(uri, readBackupManifest(uri), TransferFormat.BILL_CHECK)
        }.recoverCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("File cannot be opened")
            PendingImport(uri, CsvTransferCodec.decode(text), TransferFormat.CSV)
        }.getOrThrow()
        pendingImport = pending
        ImportPreview(
            format = pending.format,
            trips = pending.transfer.trips.map {
                ImportTripPreview(it.id, it.name, it.receipts.size, it.reconciliations.size)
            },
        )
    }

    suspend fun importSelected(sourceTripIds: Set<String>): List<String> = withContext(Dispatchers.IO) {
        val pending = pendingImport ?: error("No import has been opened")
        require(sourceTripIds.isNotEmpty()) { "Select at least one trip" }
        val selected = pending.transfer.trips.filter { it.id in sourceTripIds }
        val imageUris = if (pending.format == TransferFormat.BILL_CHECK) {
            restoreSelectedImages(pending.uri, selected)
        } else {
            emptyMap()
        }
        val importedTripIds = selected.map { insertAsNewTrip(it, imageUris) }
        pendingImport = null
        importedTripIds
    }

    fun clearPendingImport() {
        pendingImport = null
    }

    private suspend fun buildPackage(tripIds: Set<String>): TransferPackage {
        val trips = dao.getTrips(tripIds.toList()).map { trip ->
            val receipts = dao.getReceiptsWithItems(trip.id)
            val reconciliations = dao.getReconciliations(trip.id)
            TransferTrip(
                id = trip.id,
                name = trip.name,
                foreignCurrencyCode = trip.foreignCurrencyCode,
                defaultExchangeRate = trip.defaultExchangeRate,
                exchangeRateMode = trip.exchangeRateMode,
                defaultTipMinor = trip.defaultTipMinor,
                defaultTipCurrencyCode = trip.defaultTipCurrencyCode,
                defaultTipSelected = trip.defaultTipSelected,
                imageStorageMode = trip.imageStorageMode,
                createdAt = trip.createdAt,
                receipts = receipts.map { related ->
                    val receipt = related.receipt
                    TransferReceipt(
                        id = receipt.id,
                        occurredAt = receipt.occurredAt,
                        location = receipt.location,
                        checkNumber = receipt.checkNumber,
                        foreignAmountMinor = receipt.foreignAmountMinor,
                        foreignCurrencyCode = receipt.foreignCurrencyCode,
                        exchangeRate = receipt.exchangeRate,
                        exactEuroCents = receipt.exactEuroCents,
                        tipMinor = receipt.tipMinor,
                        tipCurrencyCode = receipt.tipCurrencyCode,
                        imageEntry = receipt.imageUri?.let { "images/receipts/${receipt.id}" },
                        imageMimeType = receipt.imageUri?.let(::mimeType),
                        reviewState = receipt.reviewState,
                        createdAt = receipt.createdAt,
                        items = related.items.sortedBy { it.sortPosition }.map { item ->
                            TransferReceiptItem(
                                item.id,
                                item.sortPosition,
                                item.name,
                                item.amountMinor,
                                item.currencyCode,
                            )
                        },
                        imageSourceUri = receipt.imageUri,
                    )
                },
                reconciliations = reconciliations.map { related ->
                    val reconciliation = related.reconciliation
                    TransferReconciliation(
                        id = reconciliation.id,
                        title = reconciliation.title,
                        statementImageEntry = reconciliation.statementImageUri?.let {
                            "images/statements/${reconciliation.id}"
                        },
                        statementImageMimeType = reconciliation.statementImageUri?.let(::mimeType),
                        createdAt = reconciliation.createdAt,
                        analysisSummary = reconciliation.analysisSummary,
                        analysisUpdatedAt = reconciliation.analysisUpdatedAt,
                        declaredTotalMinor = reconciliation.declaredTotalMinor,
                        declaredTotalCurrencyCode = reconciliation.declaredTotalCurrencyCode,
                        lines = related.lines.map { lineWithMatches ->
                            val line = lineWithMatches.line
                            val match = lineWithMatches.matches.firstOrNull()
                            TransferStatementLine(
                                id = line.id,
                                occurredOn = line.occurredOn,
                                description = line.description,
                                checkNumber = line.checkNumber,
                                amountMinor = line.amountMinor,
                                currencyCode = line.currencyCode,
                                status = line.status,
                                acceptedWithoutReceipt = line.acceptedWithoutReceipt,
                                matchedReceiptId = match?.receiptId,
                                matchedManually = match?.matchedManually ?: false,
                                aiSuggestedReceiptId = line.aiSuggestedReceiptId,
                                aiConfidence = line.aiConfidence,
                                aiReason = line.aiReason,
                                sourceDateText = line.sourceDateText,
                                dateAmbiguous = line.dateAmbiguous,
                            )
                        },
                        statementImageSourceUri = reconciliation.statementImageUri,
                    )
                },
            )
        }
        return TransferPackage(exportedAt = System.currentTimeMillis(), trips = trips)
    }

    private fun exportBackup(uri: Uri, transfer: TransferPackage) {
        val receiptImages = transfer.trips.flatMap { trip ->
            trip.receipts.mapNotNull { receipt ->
                receipt.imageEntry?.let { entry ->
                    receipt.imageSourceUri?.let { ImageSource(entry, Uri.parse(it)) }
                }
            }
        }
        val statementImages = transfer.trips.flatMap { trip ->
            trip.reconciliations.mapNotNull { reconciliation ->
                reconciliation.statementImageEntry?.let { entry ->
                    reconciliation.statementImageSourceUri?.let { ImageSource(entry, Uri.parse(it)) }
                }
            }
        }
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(BackupJsonCodec.encode(transfer).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                (receiptImages + statementImages).forEach { source ->
                    zip.putNextEntry(ZipEntry(source.entry))
                    context.contentResolver.openInputStream(source.uri)?.use { it.copyTo(zip) }
                        ?: error("Image cannot be opened: ${source.uri}")
                    zip.closeEntry()
                }
            }
        } ?: error("Target file cannot be opened")
    }

    private fun readBackupManifest(uri: Uri): TransferPackage {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == MANIFEST_ENTRY) {
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8_192)
                        var total = 0
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            total += read
                            require(total <= MAX_MANIFEST_BYTES) { "Backup manifest is too large" }
                            output.write(buffer, 0, read)
                        }
                        return BackupJsonCodec.decode(output.toString(Charsets.UTF_8.name()))
                    }
                }
            }
        }
        error("Bill Check manifest not found")
    }

    private fun restoreSelectedImages(uri: Uri, trips: List<TransferTrip>): Map<String, String> {
        val needed = buildMap {
            trips.forEach { trip ->
                trip.receipts.forEach { receipt ->
                    receipt.imageEntry?.let { put(it, receipt.imageMimeType ?: "image/jpeg") }
                }
                trip.reconciliations.forEach { reconciliation ->
                    reconciliation.statementImageEntry?.let {
                        put(it, reconciliation.statementImageMimeType ?: "image/jpeg")
                    }
                }
            }
        }
        if (needed.isEmpty()) return emptyMap()
        val restored = mutableMapOf<String, String>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val mime = needed[entry.name] ?: continue
                    restored[entry.name] = restoreImage(zip, mime, entry.name).toString()
                }
            }
        } ?: error("Backup cannot be reopened")
        require(restored.keys.containsAll(needed.keys)) { "Backup is missing image data" }
        return restored
    }

    private fun restoreImage(input: ZipInputStream, mimeType: String, entryName: String): Uri {
        val extension = when (mimeType.lowercase(Locale.ROOT)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "BillCheck_restore_${UUID.randomUUID()}.$extension")
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/Bill Check")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val target = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Restored image cannot be created")
        try {
            resolver.openOutputStream(target, "w")?.use { input.copyTo(it) }
                ?: error("Restored image cannot be written: $entryName")
            resolver.update(target, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            return target
        } catch (error: Throwable) {
            resolver.delete(target, null, null)
            throw error
        }
    }

    private suspend fun insertAsNewTrip(source: TransferTrip, imageUris: Map<String, String>): String {
        TransferValidator.validate(source)
        val tripId = UUID.randomUUID().toString()
        val receiptIds = source.receipts.associate { it.id to UUID.randomUUID().toString() }
        val reconciliationIds = source.reconciliations.associate { it.id to UUID.randomUUID().toString() }
        val lineIds = source.reconciliations.flatMap { it.lines }
            .associate { it.id to UUID.randomUUID().toString() }
        val importedName = uniqueImportedName(source.name, dao.getTripNames())
        val trip = TripEntity(
            id = tripId,
            sortPosition = dao.nextTripPosition(),
            name = importedName,
            foreignCurrencyCode = source.foreignCurrencyCode,
            defaultExchangeRate = source.defaultExchangeRate,
            exchangeRateMode = source.exchangeRateMode,
            defaultTipMinor = source.defaultTipMinor,
            defaultTipCurrencyCode = source.defaultTipCurrencyCode,
            defaultTipSelected = source.defaultTipSelected,
            imageStorageMode = source.imageStorageMode,
            createdAt = source.createdAt,
        )
        val receipts = source.receipts.map { receipt ->
            ReceiptEntity(
                id = receiptIds.getValue(receipt.id),
                tripId = tripId,
                occurredAt = receipt.occurredAt,
                location = receipt.location,
                checkNumber = receipt.checkNumber,
                foreignAmountMinor = receipt.foreignAmountMinor,
                foreignCurrencyCode = receipt.foreignCurrencyCode,
                exchangeRate = receipt.exchangeRate,
                exactEuroCents = receipt.exactEuroCents,
                tipMinor = receipt.tipMinor,
                tipCurrencyCode = receipt.tipCurrencyCode,
                imageUri = receipt.imageEntry?.let(imageUris::get),
                reviewState = receipt.reviewState,
                createdAt = receipt.createdAt,
            )
        }
        val items = source.receipts.flatMap { receipt ->
            receipt.items.map { item ->
                ReceiptItemEntity(
                    id = UUID.randomUUID().toString(),
                    receiptId = receiptIds.getValue(receipt.id),
                    sortPosition = item.sortPosition,
                    name = item.name,
                    amountMinor = item.amountMinor,
                    currencyCode = item.currencyCode,
                )
            }
        }
        val reconciliations = source.reconciliations.map { reconciliation ->
            ReconciliationEntity(
                id = reconciliationIds.getValue(reconciliation.id),
                tripId = tripId,
                title = reconciliation.title,
                statementImageUri = reconciliation.statementImageEntry?.let(imageUris::get),
                createdAt = reconciliation.createdAt,
                analysisSummary = reconciliation.analysisSummary,
                analysisUpdatedAt = reconciliation.analysisUpdatedAt,
                declaredTotalMinor = reconciliation.declaredTotalMinor,
                declaredTotalCurrencyCode = reconciliation.declaredTotalCurrencyCode,
            )
        }
        val lines = source.reconciliations.flatMap { reconciliation ->
            reconciliation.lines.map { line ->
                StatementLineEntity(
                    id = lineIds.getValue(line.id),
                    reconciliationId = reconciliationIds.getValue(reconciliation.id),
                    occurredOn = line.occurredOn,
                    description = line.description,
                    checkNumber = line.checkNumber,
                    amountMinor = line.amountMinor,
                    currencyCode = line.currencyCode,
                    status = sanitizeStatus(line.status),
                    acceptedWithoutReceipt = line.acceptedWithoutReceipt,
                    aiSuggestedReceiptId = line.aiSuggestedReceiptId?.let(receiptIds::get),
                    aiConfidence = line.aiConfidence?.coerceIn(0, 100),
                    aiReason = line.aiReason,
                    sourceDateText = line.sourceDateText,
                    dateAmbiguous = line.dateAmbiguous,
                )
            }
        }
        val matches = source.reconciliations.flatMap { it.lines }.mapNotNull { line ->
            val receiptId = line.matchedReceiptId?.let(receiptIds::get) ?: return@mapNotNull null
            ReceiptMatchEntity(lineIds.getValue(line.id), receiptId, line.matchedManually)
        }
        dao.insertTransferredTrip(trip, receipts, items, reconciliations, lines, matches)
        return tripId
    }

    private fun uniqueImportedName(sourceName: String, existingNames: List<String>): String {
        val normalized = existingNames.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
        if (sourceName.lowercase(Locale.ROOT) !in normalized) return sourceName
        var number = 1
        while (true) {
            val suffix = if (number == 1) " (Import)" else " (Import $number)"
            val candidate = sourceName + suffix
            if (candidate.lowercase(Locale.ROOT) !in normalized) return candidate
            number++
        }
    }

    private fun sanitizeStatus(value: String): String = value.takeIf {
        it in setOf(
            ReconciliationStatus.CORRECT,
            ReconciliationStatus.UNCERTAIN,
            ReconciliationStatus.AMOUNT_MISMATCH,
            ReconciliationStatus.CURRENCY_MISMATCH,
            ReconciliationStatus.DATE_MISMATCH,
            ReconciliationStatus.NOT_FOUND,
            ReconciliationStatus.ACCEPTED,
        )
    } ?: ReconciliationStatus.NOT_FOUND

    private fun writeText(uri: Uri, value: String) {
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
            it.write(value)
        } ?: error("Target file cannot be opened")
    }

    private fun exportPdf(uri: Uri, transfer: TransferPackage) {
        val lines = buildPdfLines(transfer)
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.MONOSPACE
        }
        try {
            lines.chunked(PDF_LINES_PER_PAGE).forEachIndexed { pageIndex, pageLines ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageIndex + 1).create())
                page.canvas.drawColor(Color.WHITE)
                pageLines.forEachIndexed { index, text ->
                    page.canvas.drawText(text.take(96), 32f, 42f + index * 15f, paint)
                }
                document.finishPage(page)
            }
            context.contentResolver.openOutputStream(uri, "wt")?.use(document::writeTo)
                ?: error("Target file cannot be opened")
        } finally {
            document.close()
        }
    }

    private fun buildPdfLines(transfer: TransferPackage): List<String> = buildList {
        add("BILL CHECK - REPORT")
        add("Export: ${formatTimestamp(transfer.exportedAt)}")
        add("")
        transfer.trips.forEach { trip ->
            add("TRIP: ${trip.name}")
            add("Receipts: ${trip.receipts.size}   Statements: ${trip.reconciliations.size}")
            val exact = trip.receipts.sumOf { it.exactEuroCents }
            add("Exact total: ${formatMinor(exact)} EUR   Rounded up: ${(exact + 99) / 100} EUR")
            add("")
            trip.reconciliations.forEach { reconciliation ->
                add("STATEMENT: ${reconciliation.title}")
                reconciliation.analysisSummary?.takeIf(String::isNotBlank)?.let { summary ->
                    add("AI SUMMARY:")
                    summary.lineSequence().forEach { add(it) }
                    add("")
                }
                add("Status       Date       Check        Amount       Description")
                reconciliation.lines.forEach { line ->
                    add(
                        "${line.status.padEnd(12).take(12)} " +
                            "${formatDate(line.occurredOn).padEnd(10)} " +
                            "${line.checkNumber.padEnd(12).take(12)} " +
                            "${(formatMinor(line.amountMinor) + " " + line.currencyCode).padEnd(12)} " +
                            line.description,
                    )
                }
                add("")
            }
            add("-".repeat(72))
        }
    }

    private fun mimeType(uri: String): String =
        context.contentResolver.getType(Uri.parse(uri))?.takeIf { it.startsWith("image/") } ?: "image/jpeg"

    private fun formatMinor(value: Long): String = "%d.%02d".format(Locale.ROOT, value / 100, value % 100)

    private fun formatDate(value: Long?): String = value?.let {
        DateTimeFormatter.ISO_LOCAL_DATE.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
    }.orEmpty()

    private fun formatTimestamp(value: Long): String =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(
            Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()),
        )

    private data class ImageSource(val entry: String, val uri: Uri)
    private data class PendingImport(val uri: Uri, val transfer: TransferPackage, val format: TransferFormat)

    private companion object {
        const val MANIFEST_ENTRY = "manifest.json"
        const val MAX_MANIFEST_BYTES = 10 * 1024 * 1024
        const val PDF_LINES_PER_PAGE = 50
    }
}
