package de.shakie.billcheck.data

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrPageTest {
    @Test
    fun `pixel geometry normalizes and clamps to image`() {
        val normalized = OcrBounds(
            left = -20,
            top = 25,
            right = 220,
            bottom = 75,
        ).normalize(imageWidth = 200, imageHeight = 100)

        assertEquals(0f, normalized.left)
        assertEquals(0.25f, normalized.top)
        assertEquals(1f, normalized.right)
        assertEquals(0.75f, normalized.bottom)
    }

    @Test
    fun `page exposes lines in block reading order without deduplication`() {
        val first = line(text = "TOTAL", readingOrder = 0)
        val duplicate = line(text = "TOTAL", readingOrder = 0)
        val page = OcrPage(
            imageWidth = 100,
            imageHeight = 200,
            text = "TOTAL\nTOTAL",
            blocks = listOf(
                block(readingOrder = 0, lines = listOf(first)),
                block(readingOrder = 1, lines = listOf(duplicate)),
            ),
        )

        assertEquals(listOf(first, duplicate), page.lines)
    }

    private fun block(readingOrder: Int, lines: List<OcrLine>) = OcrBlock(
        text = lines.joinToString("\n", transform = OcrLine::text),
        bounds = null,
        normalizedBounds = null,
        cornerPoints = emptyList(),
        normalizedCornerPoints = emptyList(),
        recognizedLanguage = null,
        readingOrder = readingOrder,
        lines = lines,
    )

    private fun line(text: String, readingOrder: Int) = OcrLine(
        text = text,
        bounds = null,
        normalizedBounds = null,
        cornerPoints = emptyList(),
        normalizedCornerPoints = emptyList(),
        recognizedLanguage = null,
        readingOrder = readingOrder,
        angleDegrees = null,
        confidence = null,
        elements = emptyList(),
    )
}
