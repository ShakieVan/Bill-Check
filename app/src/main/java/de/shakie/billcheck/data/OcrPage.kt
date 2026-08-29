package de.shakie.billcheck.data

/**
 * Provider-independent geometry returned by text recognition.
 *
 * Pixel coordinates refer to the bitmap that was actually analyzed. Use the
 * normalized counterparts when projecting OCR geometry onto an image shown at
 * another size.
 */
data class OcrPage(
    val imageWidth: Int,
    val imageHeight: Int,
    val text: String,
    val blocks: List<OcrBlock>,
) {
    init {
        require(imageWidth > 0) { "OCR image width must be positive" }
        require(imageHeight > 0) { "OCR image height must be positive" }
    }

    /** Lines in the recognizer's page reading order. */
    val lines: List<OcrLine>
        get() = blocks.flatMap(OcrBlock::lines)
}

data class OcrBlock(
    val text: String,
    val bounds: OcrBounds?,
    val normalizedBounds: OcrNormalizedBounds?,
    val cornerPoints: List<OcrPoint>,
    val normalizedCornerPoints: List<OcrNormalizedPoint>,
    val recognizedLanguage: String?,
    val readingOrder: Int,
    val lines: List<OcrLine>,
)

data class OcrLine(
    val text: String,
    val bounds: OcrBounds?,
    val normalizedBounds: OcrNormalizedBounds?,
    val cornerPoints: List<OcrPoint>,
    val normalizedCornerPoints: List<OcrNormalizedPoint>,
    val recognizedLanguage: String?,
    val readingOrder: Int,
    val angleDegrees: Float?,
    val confidence: Float?,
    val elements: List<OcrElement>,
) {
    /** Alias used by selection UI, where ML Kit elements represent words. */
    val words: List<OcrElement>
        get() = elements
}

data class OcrElement(
    val text: String,
    val bounds: OcrBounds?,
    val normalizedBounds: OcrNormalizedBounds?,
    val cornerPoints: List<OcrPoint>,
    val normalizedCornerPoints: List<OcrNormalizedPoint>,
    val recognizedLanguage: String?,
    val readingOrder: Int,
    val angleDegrees: Float?,
    val confidence: Float?,
    val symbols: List<OcrSymbol>,
)

data class OcrSymbol(
    val text: String,
    val bounds: OcrBounds?,
    val normalizedBounds: OcrNormalizedBounds?,
    val cornerPoints: List<OcrPoint>,
    val normalizedCornerPoints: List<OcrNormalizedPoint>,
    val recognizedLanguage: String?,
    val readingOrder: Int,
    val angleDegrees: Float?,
    val confidence: Float?,
)

data class OcrBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    init {
        require(left <= right) { "OCR bounds left must not exceed right" }
        require(top <= bottom) { "OCR bounds top must not exceed bottom" }
    }

    val width: Int get() = right - left
    val height: Int get() = bottom - top

    fun normalize(imageWidth: Int, imageHeight: Int): OcrNormalizedBounds {
        require(imageWidth > 0 && imageHeight > 0) { "Image dimensions must be positive" }
        return OcrNormalizedBounds(
            left = left.toFloat().div(imageWidth).coerceIn(0f, 1f),
            top = top.toFloat().div(imageHeight).coerceIn(0f, 1f),
            right = right.toFloat().div(imageWidth).coerceIn(0f, 1f),
            bottom = bottom.toFloat().div(imageHeight).coerceIn(0f, 1f),
        )
    }
}

data class OcrNormalizedBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(listOf(left, top, right, bottom).all { it in 0f..1f }) {
            "Normalized OCR bounds must be between zero and one"
        }
        require(left <= right) { "Normalized OCR bounds left must not exceed right" }
        require(top <= bottom) { "Normalized OCR bounds top must not exceed bottom" }
    }

    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

data class OcrPoint(val x: Int, val y: Int) {
    fun normalize(imageWidth: Int, imageHeight: Int): OcrNormalizedPoint {
        require(imageWidth > 0 && imageHeight > 0) { "Image dimensions must be positive" }
        return OcrNormalizedPoint(
            x = x.toFloat().div(imageWidth).coerceIn(0f, 1f),
            y = y.toFloat().div(imageHeight).coerceIn(0f, 1f),
        )
    }
}

data class OcrNormalizedPoint(val x: Float, val y: Float) {
    init {
        require(x in 0f..1f && y in 0f..1f) {
            "Normalized OCR points must be between zero and one"
        }
    }
}
