package de.shakie.billcheck.data

import de.shakie.billcheck.domain.AiSuggestionCertainty
import de.shakie.billcheck.domain.ExtractedFieldCandidate
import de.shakie.billcheck.domain.ExtractedTranscriptLine
import de.shakie.billcheck.domain.NormalizedBoundingBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridOcrPageBuilderTest {
    @Test
    fun `better Sultana text uses its local geometry even when AI box points near Beach Club`() {
        val beachBounds = OcrBounds(80, 100, 500, 150)
        val sultanaBounds = OcrBounds(90, 250, 560, 305)
        val local = page(
            line("Beach Club", beachBounds, 0),
            line("SuItana ResL", sultanaBounds, 1),
        )

        val result = HybridOcrPageBuilder.merge(
            local = local,
            transcript = listOf(
                transcript("Beach Club***", 80, 100, 500, 150),
                // Intentionally poor AI grounding: text similarity must prevent a wrong Beach match.
                transcript("Sultana Rest.**", 70, 95, 520, 155),
            ),
        )

        assertEquals(listOf("Beach Club***", "Sultana Rest.**"), result.lines.map(OcrLine::text))
        assertEquals(beachBounds, result.lines[0].bounds)
        assertEquals(sultanaBounds, result.lines[1].bounds)
    }

    @Test
    fun `quantity from AI is retained and gets selectable character geometry`() {
        val localBounds = OcrBounds(100, 400, 900, 460)
        val result = HybridOcrPageBuilder.merge(
            local = page(line("Cola - Can 330", localBounds, 0)),
            transcript = listOf(transcript("5 Cola - Can 330", 95, 395, 905, 465)),
        )

        val merged = result.lines.single()
        assertEquals("5 Cola - Can 330", merged.text)
        assertEquals(localBounds, merged.bounds)
        assertEquals(listOf("5", "Cola", "-", "Can", "330"), merged.words.map(OcrElement::text))
        assertEquals("5Cola-Can330", merged.words.flatMap(OcrElement::symbols).joinToString("") { it.text })
        assertTrue(merged.words.flatMap(OcrElement::symbols).all { it.bounds != null })
    }

    @Test
    fun `whitespace case and punctuation duplicates collapse canonically`() {
        val bounds = OcrBounds(100, 100, 700, 150)
        val result = HybridOcrPageBuilder.merge(
            local = page(line("  BEACH   CLUB ", bounds, 0)),
            transcript = listOf(
                transcript("Beach Club", 100, 100, 700, 150),
                transcript("beach club***", 102, 101, 702, 151),
            ),
        )

        assertEquals(1, result.lines.size)
        assertEquals("Beach Club", result.lines.single().text)
    }

    @Test
    fun `overlapping AI boxes do not erase distinct lines`() {
        val result = HybridOcrPageBuilder.merge(
            local = page(),
            transcript = listOf(
                transcript("Beach Club", 100, 100, 700, 180),
                transcript("Sultana Rest.", 120, 120, 720, 200),
            ),
        )

        assertEquals(listOf("Beach Club", "Sultana Rest."), result.lines.map(OcrLine::text))
        assertTrue(result.lines.all { it.bounds != null && it.words.isNotEmpty() })
    }

    @Test
    fun `degenerate transcript and candidate without a box preserve all text`() {
        val result = HybridOcrPageBuilder.merge(
            local = page(line("TOTAL 25.00", OcrBounds(100, 700, 800, 760), 0)),
            transcript = listOf(
                ExtractedTranscriptLine(
                    text = "Sultana Rest.",
                    boundingBox = NormalizedBoundingBox(500, 400, 500, 400),
                ),
            ),
            extraCandidates = listOf(
                ExtractedFieldCandidate(
                    value = "Check 5595",
                    evidenceText = "No. 5595",
                    certainty = AiSuggestionCertainty.MEDIUM,
                    boundingBox = null,
                ),
            ),
        )

        assertEquals(setOf("TOTAL 25.00", "Sultana Rest.", "No. 5595"), result.lines.map(OcrLine::text).toSet())
        assertEquals("TOTAL 25.00\nSultana Rest.\nNo. 5595", result.text)
        assertNotNull(result.lines.single { it.text == "TOTAL 25.00" }.bounds)
        assertEquals(null, result.lines.single { it.text == "Sultana Rest." }.bounds)
        assertEquals(null, result.lines.single { it.text == "No. 5595" }.bounds)
    }

    @Test
    fun `unmatched local lines remain in deterministic visual reading order`() {
        val result = HybridOcrPageBuilder.merge(
            local = page(
                line("BOTTOM", OcrBounds(100, 800, 500, 850), 0),
                line("TOP", OcrBounds(100, 100, 500, 150), 1),
            ),
            transcript = emptyList(),
        )

        assertEquals(listOf("TOP", "BOTTOM"), result.lines.map(OcrLine::text))
        assertEquals(listOf(0, 1), result.lines.map(OcrLine::readingOrder))
    }

    private fun page(vararg lines: OcrLine): OcrPage = OcrPage(
        imageWidth = 1_000,
        imageHeight = 1_000,
        text = lines.joinToString("\n", transform = OcrLine::text),
        blocks = lines.mapIndexed { index, line ->
            OcrBlock(
                text = line.text,
                bounds = line.bounds,
                normalizedBounds = line.normalizedBounds,
                cornerPoints = emptyList(),
                normalizedCornerPoints = emptyList(),
                recognizedLanguage = null,
                readingOrder = index,
                lines = listOf(line),
            )
        },
    )

    private fun line(text: String, bounds: OcrBounds, order: Int): OcrLine = OcrLine(
        text = text,
        bounds = bounds,
        normalizedBounds = bounds.normalize(1_000, 1_000),
        cornerPoints = emptyList(),
        normalizedCornerPoints = emptyList(),
        recognizedLanguage = null,
        readingOrder = order,
        angleDegrees = null,
        confidence = null,
        elements = emptyList(),
    )

    private fun transcript(
        text: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ): ExtractedTranscriptLine = ExtractedTranscriptLine(
        text = text,
        boundingBox = NormalizedBoundingBox(left, top, right, bottom),
    )
}
