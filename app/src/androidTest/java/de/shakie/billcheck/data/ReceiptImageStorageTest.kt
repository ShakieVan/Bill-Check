package de.shakie.billcheck.data

import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptImageStorageTest {
    @Test
    fun galleryImportCreatesDurableCopyAndDiscardRemovesOnlyCopy() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val bytes = byteArrayOf(1, 3, 3, 7)
        val source = checkNotNull(
            resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "billcheck-import-source.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Bill Check Test")
                },
            ),
        )
        resolver.openOutputStream(source, "w")!!.use { it.write(bytes) }
        var imported: android.net.Uri? = null
        try {
            val storage = ReceiptImageStorage(context)
            imported = storage.importGalleryImage(source)

            assertArrayEquals(bytes, resolver.openInputStream(imported)!!.use { it.readBytes() })
            assertTrue(storage.hasImageData(imported))
            storage.discardImportedImage(imported)
            assertFalse(storage.hasImageData(imported))
            imported = null
            assertArrayEquals(bytes, resolver.openInputStream(source)!!.use { it.readBytes() })
        } finally {
            imported?.let { resolver.delete(it, null, null) }
            resolver.delete(source, null, null)
        }
    }
}
