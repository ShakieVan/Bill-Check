package de.shakie.billcheck.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class OcrToken(
    val text: String,
    val looksNumeric: Boolean,
)

class LocalTextRecognizer(private val context: Context) {
    suspend fun recognize(imageUri: Uri): List<OcrToken> {
        val input = InputImage.fromFilePath(context, imageUri)
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
}
