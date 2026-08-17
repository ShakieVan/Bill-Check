package de.shakie.billcheck.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    private companion object {
        val FILE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
    }
}
