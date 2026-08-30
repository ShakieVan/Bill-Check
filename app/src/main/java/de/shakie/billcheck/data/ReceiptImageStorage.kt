package de.shakie.billcheck.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReceiptImageStorage(context: Context) {
    private val resolver = context.contentResolver

    fun createCameraImage(): Uri {
        val fileName = "BillCheck_${FILE_TIME.format(LocalDateTime.now())}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Bill Check")
        }
        return checkNotNull(
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values),
        ) { "Could not create gallery image" }
    }

    fun publishCameraImage(uri: Uri) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        resolver.update(uri, values, null, null)
    }

    fun hasImageData(uri: Uri): Boolean = runCatching {
        resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length > 0
        } == true
    }.getOrDefault(false)

    fun discardFailedCameraImage(uri: Uri) {
        resolver.delete(uri, null, null)
    }

    fun persistPickedImageAccess(uri: Uri) {
        runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Copies a temporary gallery URI into Bill Check's own durable MediaStore album. */
    suspend fun importGalleryImage(source: Uri): Uri = withContext(Dispatchers.IO) {
        val sourceMime = resolver.getType(source)?.takeIf { it.startsWith("image/") }
        val sourceName = resolver.query(
            source,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            cursor.takeIf { it.moveToFirst() }?.getString(0)
        }
        val extension = sourceMime?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
            ?: sourceName?.substringAfterLast('.', missingDelimiterValue = "")?.takeIf {
                it.length in 1..8 && it.all(Char::isLetterOrDigit)
            }
            ?: "jpg"
        val mime = sourceMime
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            ?: "image/jpeg"
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "BillCheck_Import_${FILE_TIME.format(LocalDateTime.now())}.$extension",
            )
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Bill Check")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val imported = checkNotNull(
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values),
        ) { "Could not create gallery import" }
        try {
            val copied = resolver.openInputStream(source)?.use { input ->
                resolver.openOutputStream(imported, "w")?.use(input::copyTo)
            } ?: error("Image cannot be opened")
            check(copied > 0) { "Image is empty" }
            check(
                resolver.update(
                    imported,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null,
                ) == 1,
            ) { "Gallery import could not be published" }
            imported
        } catch (error: Throwable) {
            resolver.delete(imported, null, null)
            throw error
        }
    }

    fun discardImportedImage(uri: Uri) {
        resolver.delete(uri, null, null)
    }

    private companion object {
        val FILE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
    }
}
