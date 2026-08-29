package de.shakie.billcheck.data

import de.shakie.billcheck.domain.ExtractedFieldCandidate
import de.shakie.billcheck.domain.ExtractedTranscriptLine
import de.shakie.billcheck.domain.NormalizedBoundingBox
import java.text.BreakIterator
import java.text.Normalizer
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Combines the reliable geometry of on-device OCR with the usually better text from an AI model.
 *
 * AI coordinates are deliberately only used as matching evidence. A matched line keeps the local
 * line box; its words and symbols are rebuilt proportionally so that the improved text remains
 * selectable. Unmatched AI text is retained even when its box is missing or unusable.
 */
object HybridOcrPageBuilder {
    fun merge(
        local: OcrPage,
        transcript: List<ExtractedTranscriptLine>,
        extraCandidates: List<ExtractedFieldCandidate> = emptyList(),
    ): OcrPage {
        val localLines = local.lines.mapIndexed { index, line ->
            MergeLine(
                line = line,
                normalizedBounds = effectiveBounds(line, local.imageWidth, local.imageHeight),
                originOrder = index,
                sourcePriority = SourcePriority.LOCAL,
            )
        }
        val aiLines = buildList {
            transcript.forEachIndexed { index, line ->
                add(
                    AiLine(
                        text = line.text.normalizedDisplayText(),
                        normalizedBounds = line.boundingBox.toNormalizedBoundsOrNull(),
                        originOrder = index,
                    ),
                )
            }
            extraCandidates.forEachIndexed { index, candidate ->
                val evidence = candidate.evidenceText.ifBlank { candidate.value }.normalizedDisplayText()
                if (evidence.isNotBlank()) {
                    add(
                        AiLine(
                            text = evidence,
                            normalizedBounds = candidate.boundingBox?.toNormalizedBoundsOrNull(),
                            originOrder = transcript.size + index,
                        ),
                    )
                }
            }
        }.filter { it.text.isNotBlank() }

        val unusedLocal = localLines.indices.toMutableSet()
        val merged = mutableListOf<MergeLine>()

        aiLines.forEach { ai ->
            val match = unusedLocal
                .map { localIndex ->
                    val candidate = localLines[localIndex]
                    Match(
                        localIndex = localIndex,
                        textSimilarity = textSimilarity(ai.text, candidate.line.text),
                        spatialSimilarity = spatialSimilarity(ai.normalizedBounds, candidate.normalizedBounds),
                    )
                }
                .filter(Match::isAcceptable)
                .maxWithOrNull(
                    compareBy<Match> { it.score }
                        .thenBy { it.textSimilarity }
                        .thenByDescending { it.localIndex },
                )

            if (match == null) {
                val pixelBounds = ai.normalizedBounds?.toPixelBounds(local.imageWidth, local.imageHeight)
                merged += MergeLine(
                    line = syntheticLine(
                        text = ai.text,
                        pixelBounds = pixelBounds,
                        normalizedBounds = ai.normalizedBounds,
                        readingOrder = ai.originOrder,
                    ),
                    normalizedBounds = ai.normalizedBounds,
                    originOrder = ai.originOrder,
                    sourcePriority = SourcePriority.AI,
                )
            } else {
                unusedLocal -= match.localIndex
                val localMatch = localLines[match.localIndex]
                val pixelBounds = localMatch.line.bounds
                    ?.takeIf { it.width > 0 && it.height > 0 }
                    ?: localMatch.normalizedBounds?.toPixelBounds(local.imageWidth, local.imageHeight)
                merged += MergeLine(
                    line = syntheticLine(
                        text = ai.text,
                        pixelBounds = pixelBounds,
                        normalizedBounds = localMatch.normalizedBounds,
                        readingOrder = localMatch.line.readingOrder,
                        language = localMatch.line.recognizedLanguage,
                        angleDegrees = localMatch.line.angleDegrees,
                        confidence = localMatch.line.confidence,
                    ),
                    normalizedBounds = localMatch.normalizedBounds,
                    originOrder = localMatch.originOrder,
                    sourcePriority = SourcePriority.MATCHED_AI,
                )
            }
        }

        unusedLocal.sorted().forEach { merged += localLines[it] }

        val ordered = merged
            .sortedWith(readingOrderComparator)
            .fold(mutableListOf<MergeLine>()) { unique, candidate ->
                val duplicateIndex = unique.indexOfFirst { existing ->
                    canonicalText(existing.line.text) == canonicalText(candidate.line.text) &&
                        sameVisualLine(existing.normalizedBounds, candidate.normalizedBounds)
                }
                if (duplicateIndex < 0 || canonicalText(candidate.line.text).isBlank()) {
                    unique += candidate
                } else if (candidate.sourcePriority.rank > unique[duplicateIndex].sourcePriority.rank) {
                    unique[duplicateIndex] = candidate
                }
                unique
            }
            .mapIndexed { index, item -> item.line.withReadingOrder(index) }

        return OcrPage(
            imageWidth = local.imageWidth,
            imageHeight = local.imageHeight,
            text = ordered.joinToString("\n", transform = OcrLine::text),
            blocks = ordered.mapIndexed { index, line -> line.asBlock(index) },
        )
    }

    private data class AiLine(
        val text: String,
        val normalizedBounds: OcrNormalizedBounds?,
        val originOrder: Int,
    )

    private data class MergeLine(
        val line: OcrLine,
        val normalizedBounds: OcrNormalizedBounds?,
        val originOrder: Int,
        val sourcePriority: SourcePriority,
    )

    private enum class SourcePriority(val rank: Int) {
        LOCAL(0),
        AI(1),
        MATCHED_AI(2),
    }

    private data class Match(
        val localIndex: Int,
        val textSimilarity: Double,
        val spatialSimilarity: Double?,
    ) {
        val score: Double = if (spatialSimilarity == null) {
            textSimilarity
        } else {
            textSimilarity * 0.78 + spatialSimilarity * 0.22
        }

        fun isAcceptable(): Boolean = when {
            textSimilarity >= 0.72 -> true
            textSimilarity >= 0.56 && score >= 0.59 -> true
            textSimilarity >= 0.34 && (spatialSimilarity ?: 0.0) >= 0.78 && score >= 0.50 -> true
            else -> false
        }
    }

    private val readingOrderComparator = compareBy<MergeLine>(
        { it.normalizedBounds == null },
        { it.normalizedBounds?.top ?: Float.MAX_VALUE },
        { it.normalizedBounds?.left ?: Float.MAX_VALUE },
        { it.originOrder },
        { canonicalText(it.line.text) },
    )

    private fun effectiveBounds(line: OcrLine, imageWidth: Int, imageHeight: Int): OcrNormalizedBounds? {
        line.normalizedBounds?.takeIf { it.isUsable() }?.let { return it }
        line.bounds
            ?.takeIf { it.width > 0 && it.height > 0 }
            ?.normalize(imageWidth, imageHeight)
            ?.takeIf { it.isUsable() }
            ?.let { return it }

        return line.elements
            .mapNotNull { element ->
                element.normalizedBounds?.takeIf { it.isUsable() }
                    ?: element.bounds
                        ?.takeIf { it.width > 0 && it.height > 0 }
                        ?.normalize(imageWidth, imageHeight)
                        ?.takeIf { it.isUsable() }
            }
            .unionOrNull()
    }

    private fun NormalizedBoundingBox.toNormalizedBoundsOrNull(): OcrNormalizedBounds? {
        val clampedLeft = left.coerceIn(0, AI_COORDINATE_MAX)
        val clampedTop = top.coerceIn(0, AI_COORDINATE_MAX)
        val clampedRight = right.coerceIn(0, AI_COORDINATE_MAX)
        val clampedBottom = bottom.coerceIn(0, AI_COORDINATE_MAX)
        if (clampedRight <= clampedLeft || clampedBottom <= clampedTop) return null
        return OcrNormalizedBounds(
            left = clampedLeft / AI_COORDINATE_MAX.toFloat(),
            top = clampedTop / AI_COORDINATE_MAX.toFloat(),
            right = clampedRight / AI_COORDINATE_MAX.toFloat(),
            bottom = clampedBottom / AI_COORDINATE_MAX.toFloat(),
        )
    }

    private fun OcrNormalizedBounds.toPixelBounds(imageWidth: Int, imageHeight: Int): OcrBounds? {
        if (!isUsable()) return null
        val leftPx = floor(left * imageWidth).toInt().coerceIn(0, imageWidth - 1)
        val topPx = floor(top * imageHeight).toInt().coerceIn(0, imageHeight - 1)
        val rightPx = ceil(right * imageWidth).toInt().coerceIn(leftPx + 1, imageWidth)
        val bottomPx = ceil(bottom * imageHeight).toInt().coerceIn(topPx + 1, imageHeight)
        return OcrBounds(leftPx, topPx, rightPx, bottomPx)
    }

    private fun syntheticLine(
        text: String,
        pixelBounds: OcrBounds?,
        normalizedBounds: OcrNormalizedBounds?,
        readingOrder: Int,
        language: String? = null,
        angleDegrees: Float? = null,
        confidence: Float? = null,
    ): OcrLine {
        val words = nonWhitespaceRanges(text).mapIndexed { wordIndex, range ->
            val wordBounds = proportionalBounds(pixelBounds, range.first, range.last + 1, text.length)
            val wordNormalizedBounds = proportionalBounds(normalizedBounds, range.first, range.last + 1, text.length)
            val symbols = graphemeRanges(text, range).mapIndexed { symbolIndex, symbolRange ->
                OcrSymbol(
                    text = text.substring(symbolRange.first, symbolRange.last + 1),
                    bounds = proportionalBounds(pixelBounds, symbolRange.first, symbolRange.last + 1, text.length),
                    normalizedBounds = proportionalBounds(
                        normalizedBounds,
                        symbolRange.first,
                        symbolRange.last + 1,
                        text.length,
                    ),
                    cornerPoints = emptyList(),
                    normalizedCornerPoints = emptyList(),
                    recognizedLanguage = language,
                    readingOrder = symbolIndex,
                    angleDegrees = angleDegrees,
                    confidence = confidence,
                )
            }
            OcrElement(
                text = text.substring(range.first, range.last + 1),
                bounds = wordBounds,
                normalizedBounds = wordNormalizedBounds,
                cornerPoints = emptyList(),
                normalizedCornerPoints = emptyList(),
                recognizedLanguage = language,
                readingOrder = wordIndex,
                angleDegrees = angleDegrees,
                confidence = confidence,
                symbols = symbols,
            )
        }
        return OcrLine(
            text = text,
            bounds = pixelBounds,
            normalizedBounds = normalizedBounds,
            cornerPoints = emptyList(),
            normalizedCornerPoints = emptyList(),
            recognizedLanguage = language,
            readingOrder = readingOrder,
            angleDegrees = angleDegrees,
            confidence = confidence,
            elements = words,
        )
    }

    private fun nonWhitespaceRanges(text: String): List<IntRange> = Regex("\\S+")
        .findAll(text)
        .map { it.range }
        .toList()

    private fun graphemeRanges(text: String, wordRange: IntRange): List<IntRange> {
        if (wordRange.isEmpty()) return emptyList()
        val word = text.substring(wordRange.first, wordRange.last + 1)
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(word) }
        val result = mutableListOf<IntRange>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            result += (wordRange.first + start)..(wordRange.first + end - 1)
            start = end
            end = iterator.next()
        }
        return result
    }

    private fun proportionalBounds(
        bounds: OcrBounds?,
        start: Int,
        endExclusive: Int,
        totalLength: Int,
    ): OcrBounds? {
        if (bounds == null || bounds.width <= 0 || totalLength <= 0) return bounds
        val left = bounds.left + floor(bounds.width * start.toDouble() / totalLength).toInt()
        val right = bounds.left + ceil(bounds.width * endExclusive.toDouble() / totalLength).toInt()
        return OcrBounds(left.coerceAtMost(bounds.right), bounds.top, right.coerceAtMost(bounds.right), bounds.bottom)
    }

    private fun proportionalBounds(
        bounds: OcrNormalizedBounds?,
        start: Int,
        endExclusive: Int,
        totalLength: Int,
    ): OcrNormalizedBounds? {
        if (bounds == null || bounds.width <= 0f || totalLength <= 0) return bounds
        val left = bounds.left + bounds.width * start / totalLength
        val right = bounds.left + bounds.width * endExclusive / totalLength
        return OcrNormalizedBounds(left, bounds.top, right, bounds.bottom)
    }

    private fun textSimilarity(first: String, second: String): Double {
        val a = canonicalText(first)
        val b = canonicalText(second)
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0

        val edit = 1.0 - levenshteinDistance(a, b).toDouble() / max(a.length, b.length)
        val aTokens = a.split(' ').filter(String::isNotBlank).toSet()
        val bTokens = b.split(' ').filter(String::isNotBlank).toSet()
        val tokenUnion = (aTokens union bTokens).size
        val tokenJaccard = if (tokenUnion == 0) 0.0 else (aTokens intersect bTokens).size.toDouble() / tokenUnion
        val containment = if (a.contains(b) || b.contains(a)) {
            min(a.length, b.length).toDouble() / max(a.length, b.length)
        } else {
            0.0
        }
        return max(edit, max(tokenJaccard, containment))
    }

    private fun spatialSimilarity(first: OcrNormalizedBounds?, second: OcrNormalizedBounds?): Double? {
        if (first == null || second == null) return null
        val intersectionWidth = max(0f, min(first.right, second.right) - max(first.left, second.left))
        val intersectionHeight = max(0f, min(first.bottom, second.bottom) - max(first.top, second.top))
        val intersection = intersectionWidth * intersectionHeight
        val union = first.width * first.height + second.width * second.height - intersection
        val iou = if (union > 0f) intersection / union else 0f
        val verticalOverlap = intersectionHeight / min(first.height, second.height).coerceAtLeast(0.0001f)
        val centerDistance = hypot(
            ((first.left + first.right) - (second.left + second.right)).toDouble() / 2.0,
            ((first.top + first.bottom) - (second.top + second.bottom)).toDouble() / 2.0,
        )
        val proximity = (1.0 - centerDistance / 0.5).coerceIn(0.0, 1.0)
        return max(iou.toDouble(), verticalOverlap * 0.65 + proximity * 0.35)
    }

    private fun sameVisualLine(first: OcrNormalizedBounds?, second: OcrNormalizedBounds?): Boolean {
        if (first == null || second == null) return true
        val spatial = spatialSimilarity(first, second) ?: return false
        val centerYDistance = kotlin.math.abs(
            (first.top + first.bottom) / 2f - (second.top + second.bottom) / 2f,
        )
        return spatial >= 0.58 || centerYDistance <= min(first.height, second.height) * 0.45f
    }

    private fun canonicalText(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun String.normalizedDisplayText(): String = trim().replace(Regex("\\s+"), " ")

    private fun levenshteinDistance(first: String, second: String): Int {
        if (first.isEmpty()) return second.length
        if (second.isEmpty()) return first.length
        var previous = IntArray(second.length + 1) { it }
        first.forEachIndexed { firstIndex, firstChar ->
            val current = IntArray(second.length + 1)
            current[0] = firstIndex + 1
            second.forEachIndexed { secondIndex, secondChar ->
                current[secondIndex + 1] = minOf(
                    current[secondIndex] + 1,
                    previous[secondIndex + 1] + 1,
                    previous[secondIndex] + if (firstChar == secondChar) 0 else 1,
                )
            }
            previous = current
        }
        return previous[second.length]
    }

    private fun List<OcrNormalizedBounds>.unionOrNull(): OcrNormalizedBounds? {
        if (isEmpty()) return null
        return OcrNormalizedBounds(
            left = minOf(OcrNormalizedBounds::left),
            top = minOf(OcrNormalizedBounds::top),
            right = maxOf(OcrNormalizedBounds::right),
            bottom = maxOf(OcrNormalizedBounds::bottom),
        ).takeIf { it.isUsable() }
    }

    private fun OcrNormalizedBounds.isUsable(): Boolean = width > 0f && height > 0f

    private fun OcrLine.withReadingOrder(readingOrder: Int): OcrLine = copy(
        readingOrder = readingOrder,
        elements = elements.mapIndexed { wordIndex, element ->
            element.copy(
                readingOrder = wordIndex,
                symbols = element.symbols.mapIndexed { symbolIndex, symbol ->
                    symbol.copy(readingOrder = symbolIndex)
                },
            )
        },
    )

    private fun OcrLine.asBlock(readingOrder: Int): OcrBlock = OcrBlock(
        text = text,
        bounds = bounds,
        normalizedBounds = normalizedBounds,
        cornerPoints = cornerPoints,
        normalizedCornerPoints = normalizedCornerPoints,
        recognizedLanguage = recognizedLanguage,
        readingOrder = readingOrder,
        lines = listOf(this),
    )

    private const val AI_COORDINATE_MAX = 1_000
}
