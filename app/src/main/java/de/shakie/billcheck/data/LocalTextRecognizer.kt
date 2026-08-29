package de.shakie.billcheck.data

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class LocalTextRecognizer(private val context: Context) {
    suspend fun recognize(imageUri: Uri): OcrPage {
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
                        val imageWidth = bitmap.width
                        val imageHeight = bitmap.height
                        var pageLineOrder = 0
                        val blocks = result.textBlocks.mapIndexed { blockIndex, block ->
                            val blockGeometry = geometry(
                                block.boundingBox,
                                block.cornerPoints,
                                imageWidth,
                                imageHeight,
                            )
                            OcrBlock(
                                text = block.text,
                                bounds = blockGeometry.bounds,
                                normalizedBounds = blockGeometry.normalizedBounds,
                                cornerPoints = blockGeometry.cornerPoints,
                                normalizedCornerPoints = blockGeometry.normalizedCornerPoints,
                                recognizedLanguage = block.recognizedLanguage.nonBlankOrNull(),
                                readingOrder = blockIndex,
                                lines = block.lines.map { line ->
                                    val lineGeometry = geometry(
                                        line.boundingBox,
                                        line.cornerPoints,
                                        imageWidth,
                                        imageHeight,
                                    )
                                    OcrLine(
                                        text = line.text,
                                        bounds = lineGeometry.bounds,
                                        normalizedBounds = lineGeometry.normalizedBounds,
                                        cornerPoints = lineGeometry.cornerPoints,
                                        normalizedCornerPoints = lineGeometry.normalizedCornerPoints,
                                        recognizedLanguage = line.recognizedLanguage.nonBlankOrNull(),
                                        readingOrder = pageLineOrder++,
                                        angleDegrees = line.angle.finiteOrNull(),
                                        confidence = line.confidence.confidenceOrNull(),
                                        elements = line.elements.mapIndexed { elementIndex, element ->
                                            val elementGeometry = geometry(
                                                element.boundingBox,
                                                element.cornerPoints,
                                                imageWidth,
                                                imageHeight,
                                            )
                                            OcrElement(
                                                text = element.text,
                                                bounds = elementGeometry.bounds,
                                                normalizedBounds = elementGeometry.normalizedBounds,
                                                cornerPoints = elementGeometry.cornerPoints,
                                                normalizedCornerPoints = elementGeometry.normalizedCornerPoints,
                                                recognizedLanguage = element.recognizedLanguage
                                                    .nonBlankOrNull(),
                                                readingOrder = elementIndex,
                                                angleDegrees = element.angle.finiteOrNull(),
                                                confidence = element.confidence.confidenceOrNull(),
                                                symbols = element.symbols.mapIndexed { symbolIndex, symbol ->
                                                    val symbolGeometry = geometry(
                                                        symbol.boundingBox,
                                                        symbol.cornerPoints,
                                                        imageWidth,
                                                        imageHeight,
                                                    )
                                                    OcrSymbol(
                                                        text = symbol.text,
                                                        bounds = symbolGeometry.bounds,
                                                        normalizedBounds = symbolGeometry.normalizedBounds,
                                                        cornerPoints = symbolGeometry.cornerPoints,
                                                        normalizedCornerPoints = symbolGeometry
                                                            .normalizedCornerPoints,
                                                        recognizedLanguage = symbol.recognizedLanguage
                                                            .nonBlankOrNull(),
                                                        readingOrder = symbolIndex,
                                                        angleDegrees = symbol.angle.finiteOrNull(),
                                                        confidence = symbol.confidence.confidenceOrNull(),
                                                    )
                                                },
                                            )
                                        },
                                    )
                                },
                            )
                        }
                        continuation.resume(
                            OcrPage(
                                imageWidth = imageWidth,
                                imageHeight = imageHeight,
                                text = result.text,
                                blocks = blocks,
                            ),
                        )
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

private data class OcrGeometry(
    val bounds: OcrBounds?,
    val normalizedBounds: OcrNormalizedBounds?,
    val cornerPoints: List<OcrPoint>,
    val normalizedCornerPoints: List<OcrNormalizedPoint>,
)

private fun geometry(
    rectangle: Rect?,
    points: Array<Point>?,
    imageWidth: Int,
    imageHeight: Int,
): OcrGeometry {
    val bounds = rectangle?.let {
        OcrBounds(
            left = minOf(it.left, it.right),
            top = minOf(it.top, it.bottom),
            right = maxOf(it.left, it.right),
            bottom = maxOf(it.top, it.bottom),
        )
    }
    val cornerPoints = points.orEmpty().map { OcrPoint(it.x, it.y) }
    return OcrGeometry(
        bounds = bounds,
        normalizedBounds = bounds?.normalize(imageWidth, imageHeight),
        cornerPoints = cornerPoints,
        normalizedCornerPoints = cornerPoints.map { it.normalize(imageWidth, imageHeight) },
    )
}

private fun String?.nonBlankOrNull(): String? = this?.takeIf(String::isNotBlank)

private fun Float.finiteOrNull(): Float? = takeIf(Float::isFinite)

private fun Float.confidenceOrNull(): Float? = takeIf { it.isFinite() && it in 0f..1f }
