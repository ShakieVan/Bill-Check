package de.shakie.billcheck.ui

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenGalleryImageContractTest {
    @Test
    fun multipleGalleryResultPreservesEverySelectedImageOnce() {
        val first = Uri.parse("content://images/1")
        val second = Uri.parse("content://images/2")
        val intent = Intent().apply {
            clipData = ClipData.newRawUri("receipt", first).apply {
                addItem(ClipData.Item(second))
            }
            data = first
        }

        val parsed = OpenMultipleGalleryImagesContract().parseResult(Activity.RESULT_OK, intent)

        assertEquals(listOf(first, second), parsed)
    }
}
