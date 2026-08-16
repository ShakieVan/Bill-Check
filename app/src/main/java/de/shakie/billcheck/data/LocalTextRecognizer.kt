package de.shakie.billcheck.data

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class OcrToken(
    val text: String,
    val looksNumeric: Boolean,
)

class LocalTextRecognizer(private val context: Context) {
    suspend fun recognize(imageUri: Uri): List<OcrToken> {
        // ML Kit's URI decoder can return a null bitmap for very large images
        // from some Samsung cameras. Android's ImageDecoder handles the same
        // content URI reliably and lets us bound memory without sacrificing
        // the receipt detail needed by OCR.
        val bitmap = withContext(Dispatchers.IO) {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val largest = maxOf(info.size.width, info.size.height)
                if (largest > OCR_MAX_DIMENSION) {
                    val scale = OCR_MAX_DIMENSION.toDouble() / largest
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val input = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            suspendCancellableCoroutine { continuation ->
                recognizer.process(input)
                    .addOnSuccessListener { result ->
                        if (!continuation.isActive) return@addOnSuccessListener
                        val tokens = result.textBlocks
                            .flatMap { it.lines }
                            .flatMap { it.elements }
                            .map { it.text.trim() }
                            .filter(String::isNotBlank)
                            .distinctBy(String::lowercase)
                            .map { text ->
                                OcrToken(
                                    text = text,
                                    looksNumeric = text.any(Char::isDigit) &&
                                        text.none { it.isLetter() },
                                )
                            }
                        continuation.resume(tokens)
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
            }
        } finally {
            recognizer.close()
        }
    }

    private companion object {
        const val OCR_MAX_DIMENSION = 3_072
    }
}
