package de.shakie.billcheck.ui

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.shakie.billcheck.R
import de.shakie.billcheck.data.OcrElement
import de.shakie.billcheck.data.OcrPage
import de.shakie.billcheck.data.OcrSymbol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full-screen, image-bound text selection.
 *
 * OCR text itself stays invisible. Only the currently selected characters are
 * painted over the receipt, so the image remains the source of truth. A long
 * press selects a word and a continued drag adjusts the selection by character.
 * A tap is a convenient fallback for selecting a complete word.
 */
@Composable
fun SpatialTextSelectionDialog(
    imageUri: Uri,
    ocrPage: OcrPage,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageDescription = stringResource(R.string.spatial_text_accessibility, ocrPage.text)
    val glyphs = remember(ocrPage) { ocrPage.selectableGlyphs() }
    var image by remember(imageUri) { mutableStateOf<ImageBitmap?>(null) }
    var imageFailed by remember(imageUri) { mutableStateOf(false) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(imageUri) { mutableStateOf(1f) }
    var offset by remember(imageUri) { mutableStateOf(Offset.Zero) }
    var selection by remember(imageUri, ocrPage) { mutableStateOf<GlyphSelection?>(null) }
    var magnifierSource by remember(imageUri, ocrPage) { mutableStateOf<Offset?>(null) }

    LaunchedEffect(imageUri) {
        image = null
        imageFailed = false
        runCatching {
            withContext(Dispatchers.IO) {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val largest = maxOf(info.size.width, info.size.height)
                    if (largest > IMAGE_MAX_DIMENSION) {
                        val factor = IMAGE_MAX_DIMENSION.toDouble() / largest
                        decoder.setTargetSize(
                            (info.size.width * factor).toInt().coerceAtLeast(1),
                            (info.size.height * factor).toInt().coerceAtLeast(1),
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }.asImageBitmap()
            }
        }.onSuccess { image = it }
            .onFailure { imageFailed = true }
    }

    val geometry = remember(viewport, image, scale, offset) {
        image?.let { bitmap ->
            ImageGeometry(
                viewport = viewport,
                imageSize = IntSize(bitmap.width, bitmap.height),
                scale = scale,
                offset = offset,
            )
        }
    }

    fun constrainedOffset(candidate: Offset, atScale: Float): Offset {
        val bitmap = image ?: return Offset.Zero
        if (viewport.width == 0 || viewport.height == 0 || atScale <= 1f) return Offset.Zero
        val fit = fitSize(viewport, IntSize(bitmap.width, bitmap.height))
        val maxX = ((fit.width * atScale - viewport.width) / 2f).coerceAtLeast(0f)
        val maxY = ((fit.height * atScale - viewport.height) / 2f).coerceAtLeast(0f)
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    val transformState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        val previousScale = scale
        val nextScale = (previousScale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        val center = Offset(viewport.width / 2f, viewport.height / 2f)
        val centroidFromCenter = centroid - center
        val zoomOffset = (centroidFromCenter - offset) * (1f - nextScale / previousScale)
        scale = nextScale
        offset = constrainedOffset(offset + panChange + zoomOffset, nextScale)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(color = Color.Black, modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewport = it }
                    .transformable(transformState)
                    .magnifier(
                        sourceCenter = { magnifierSource ?: Offset.Unspecified },
                        magnifierCenter = {
                            magnifierSource?.let { source ->
                                val distance = MAGNIFIER_OFFSET_DP.dp.toPx()
                                if (source.y > distance * 1.4f) {
                                    source - Offset(0f, distance)
                                } else {
                                    source + Offset(0f, distance)
                                }
                            } ?: Offset.Unspecified
                        },
                        zoom = MAGNIFIER_ZOOM,
                    )
                    .pointerInput(imageUri, glyphs, geometry) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = if (scale > 1.01f) MIN_SCALE else DOUBLE_TAP_SCALE
                                offset = Offset.Zero
                            },
                            onTap = { point ->
                                val hit = geometry?.findGlyph(point, glyphs, HIT_SLOP_DP.dp.toPx())
                                selection = hit?.let { index ->
                                    selection?.adjustTo(index) ?: glyphs.wordSelectionAt(index)
                                }
                            },
                        )
                    }
                    .pointerInput(imageUri, glyphs, geometry) {
                        var fixedIndex: Int? = null
                        var dragPoint = Offset.Zero
                        detectDragGestures(
                            onDragStart = dragStart@{ point ->
                                fixedIndex = null
                                val active = selection?.ordered ?: return@dragStart
                                val currentGeometry = geometry ?: return@dragStart
                                val startHandle = glyphs.getOrNull(active.first)?.let { glyph ->
                                    currentGeometry.toViewport(glyph.bounds).bottomLeft
                                }
                                val endHandle = glyphs.getOrNull(active.last)?.let { glyph ->
                                    currentGeometry.toViewport(glyph.bounds).bottomRight
                                }
                                val radius = HANDLE_TOUCH_RADIUS_DP.dp.toPx()
                                val startDistance = startHandle?.let { (point - it).getDistance() }
                                val endDistance = endHandle?.let { (point - it).getDistance() }
                                fixedIndex = when {
                                    startDistance != null && startDistance <= radius &&
                                        (endDistance == null || startDistance <= endDistance) -> active.last
                                    endDistance != null && endDistance <= radius -> active.first
                                    else -> null
                                }
                                if (fixedIndex != null) {
                                    dragPoint = point
                                    magnifierSource = point
                                }
                            },
                            onDrag = handleDrag@{ change, dragAmount ->
                                val anchor = fixedIndex ?: return@handleDrag
                                change.consume()
                                dragPoint += dragAmount
                                magnifierSource = dragPoint
                                geometry?.findGlyph(dragPoint, glyphs, DRAG_SLOP_DP.dp.toPx())
                                    ?.let { hit -> selection = GlyphSelection(anchor, hit) }
                            },
                            onDragCancel = {
                                fixedIndex = null
                                magnifierSource = null
                            },
                            onDragEnd = {
                                fixedIndex = null
                                magnifierSource = null
                            },
                        )
                    }
                    .pointerInput(imageUri, glyphs, geometry) {
                        var dragPoint = Offset.Zero
                        var fixedIndex: Int? = null
                        detectDragGesturesAfterLongPress(
                            onDragStart = { point ->
                                fixedIndex = null
                                val hit = geometry?.findGlyph(point, glyphs, HIT_SLOP_DP.dp.toPx())
                                if (hit != null) {
                                    val word = glyphs.wordSelectionAt(hit)
                                    selection = word
                                    fixedIndex = hit
                                    dragPoint = point
                                    magnifierSource = point
                                }
                            },
                            onDrag = drag@{ change, dragAmount ->
                                val anchor = fixedIndex ?: return@drag
                                change.consume()
                                dragPoint += dragAmount
                                magnifierSource = dragPoint
                                geometry?.findGlyph(dragPoint, glyphs, DRAG_SLOP_DP.dp.toPx())
                                    ?.let { hit -> selection = GlyphSelection(anchor, hit) }
                            },
                            onDragCancel = {
                                fixedIndex = null
                                magnifierSource = null
                            },
                            onDragEnd = {
                                fixedIndex = null
                                magnifierSource = null
                            },
                        )
                    },
            ) {
                when {
                    image != null -> {
                        Image(
                            bitmap = requireNotNull(image),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                        )
                        val selectionColor = MaterialTheme.colorScheme.primary
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics { contentDescription = imageDescription },
                        ) {
                            val active = selection?.ordered ?: return@Canvas
                            val currentGeometry = geometry ?: return@Canvas
                            glyphs.asSequence()
                                .filter { it.index in active }
                                .forEach { glyph ->
                                    val rect = currentGeometry.toViewport(glyph.bounds)
                                    drawRoundRect(
                                        color = selectionColor.copy(alpha = 0.34f),
                                        topLeft = rect.topLeft,
                                        size = rect.size,
                                    )
                                    drawRoundRect(
                                        color = selectionColor.copy(alpha = 0.9f),
                                        topLeft = rect.topLeft,
                                        size = rect.size,
                                        style = Stroke(width = 1.dp.toPx()),
                                    )
                                }
                            glyphs.getOrNull(active.first)?.let { first ->
                                val rect = currentGeometry.toViewport(first.bounds)
                                drawCircle(selectionColor, radius = HANDLE_RADIUS_DP.dp.toPx(), center = rect.bottomLeft)
                            }
                            glyphs.getOrNull(active.last)?.let { last ->
                                val rect = currentGeometry.toViewport(last.bounds)
                                drawCircle(selectionColor, radius = HANDLE_RADIUS_DP.dp.toPx(), center = rect.bottomRight)
                            }
                        }
                    }
                    imageFailed -> Text(
                        text = stringResource(R.string.image_unavailable),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.68f),
                        shape = RoundedCornerShape(50),
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = Color.White,
                            )
                        }
                    }
                    Surface(
                        color = if (selection == null) {
                            Color.Black.copy(alpha = 0.42f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        shape = RoundedCornerShape(50),
                    ) {
                        IconButton(
                            enabled = selection != null,
                            onClick = {
                                selection?.let { onConfirm(glyphs.selectedText(it)) }
                            },
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(R.string.apply_result),
                                tint = Color.White,
                            )
                        }
                    }
                }

                selection?.let { active ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.76f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .safeDrawingPadding()
                            .padding(16.dp)
                            .fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = glyphs.selectedText(active),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                } ?: Surface(
                    color = Color.Black.copy(alpha = 0.68f),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .safeDrawingPadding()
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (glyphs.isEmpty()) R.string.local_ocr_empty
                            else R.string.spatial_text_selection_hint,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

private data class GlyphSelection(val anchor: Int, val focus: Int) {
    val start: Int get() = minOf(anchor, focus)
    val ordered: IntRange get() = start..maxOf(anchor, focus)

    /** Move the nearest selection edge to a tapped character. */
    fun adjustTo(index: Int): GlyphSelection {
        val range = ordered
        return if (index - range.first <= range.last - index) {
            GlyphSelection(range.last, index)
        } else {
            GlyphSelection(range.first, index)
        }
    }
}

private data class SelectableGlyph(
    val index: Int,
    val text: String,
    val bounds: Rect,
    val lineIndex: Int,
    val wordIndex: Int,
)

private data class ImageGeometry(
    val viewport: IntSize,
    val imageSize: IntSize,
    val scale: Float,
    val offset: Offset,
) {
    private val baseImageRect: Rect
        get() {
            val fitted = fitSize(viewport, imageSize)
            return Rect(
                left = (viewport.width - fitted.width) / 2f,
                top = (viewport.height - fitted.height) / 2f,
                right = (viewport.width + fitted.width) / 2f,
                bottom = (viewport.height + fitted.height) / 2f,
            )
        }

    fun toViewport(normalized: Rect): Rect {
        val image = baseImageRect
        val center = Offset(viewport.width / 2f, viewport.height / 2f)
        fun point(x: Float, y: Float): Offset {
            val requested = Offset(
                image.left + x.coerceIn(0f, 1f) * image.width,
                image.top + y.coerceIn(0f, 1f) * image.height,
            )
            return center + (requested - center) * scale + offset
        }
        return Rect(point(normalized.left, normalized.top), point(normalized.right, normalized.bottom))
    }

    fun findGlyph(point: Offset, glyphs: List<SelectableGlyph>, maxDistance: Float): Int? {
        if (glyphs.isEmpty()) return null
        var closestIndex: Int? = null
        var closestDistanceSquared = Float.POSITIVE_INFINITY
        glyphs.forEachIndexed { index, glyph ->
            val rect = toViewport(glyph.bounds)
            if (rect.contains(point)) return index
            val dx = when {
                point.x < rect.left -> rect.left - point.x
                point.x > rect.right -> point.x - rect.right
                else -> 0f
            }
            val dy = when {
                point.y < rect.top -> rect.top - point.y
                point.y > rect.bottom -> point.y - rect.bottom
                else -> 0f
            }
            val distanceSquared = dx * dx + dy * dy
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared
                closestIndex = index
            }
        }
        return closestIndex.takeIf { closestDistanceSquared <= maxDistance * maxDistance }
    }
}

private data class FittedSize(val width: Float, val height: Float)

private fun fitSize(viewport: IntSize, image: IntSize): FittedSize {
    if (viewport.width == 0 || viewport.height == 0 || image.width == 0 || image.height == 0) {
        return FittedSize(0f, 0f)
    }
    val factor = minOf(viewport.width.toFloat() / image.width, viewport.height.toFloat() / image.height)
    return FittedSize(image.width * factor, image.height * factor)
}

private fun OcrPage.selectableGlyphs(): List<SelectableGlyph> {
    val glyphs = mutableListOf<SelectableGlyph>()
    blocks.flatMap { it.lines }
        .sortedBy { it.readingOrder }
        .forEachIndexed { lineIndex, line ->
            line.words.sortedBy { it.readingOrder }.forEachIndexed { wordIndex, word ->
                glyphs += word.toGlyphs(
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    lineIndex = lineIndex,
                    wordIndex = wordIndex,
                    firstIndex = glyphs.size,
                )
            }
        }
    return glyphs
}

private fun OcrElement.toGlyphs(
    imageWidth: Int,
    imageHeight: Int,
    lineIndex: Int,
    wordIndex: Int,
    firstIndex: Int,
): List<SelectableGlyph> {
    val wordBounds = normalizedRect(imageWidth, imageHeight) ?: return emptyList()
    val orderedSymbols = symbols.sortedBy(OcrSymbol::readingOrder)
    if (orderedSymbols.isEmpty() || orderedSymbols.joinToString("") { it.text } != text) {
        return text.toCharacterGlyphs(wordBounds, lineIndex, wordIndex, firstIndex)
    }
    val result = mutableListOf<SelectableGlyph>()
    val symbolCharacterCount = orderedSymbols.sumOf { it.text.length.coerceAtLeast(1) }
    var fallbackCharacterOffset = 0
    orderedSymbols.forEach { symbol ->
        val symbolText = symbol.text.ifBlank { return@forEach }
        val fallbackLeft = wordBounds.left + wordBounds.width * fallbackCharacterOffset / symbolCharacterCount
        fallbackCharacterOffset += symbolText.length
        val fallbackRight = wordBounds.left + wordBounds.width * fallbackCharacterOffset / symbolCharacterCount
        val bounds = symbol.normalizedRect(imageWidth, imageHeight)
            ?: Rect(fallbackLeft, wordBounds.top, fallbackRight, wordBounds.bottom)
        result += symbolText.toCharacterGlyphs(
            bounds = bounds,
            lineIndex = lineIndex,
            wordIndex = wordIndex,
            firstIndex = firstIndex + result.size,
        )
    }
    return result.ifEmpty { text.toCharacterGlyphs(wordBounds, lineIndex, wordIndex, firstIndex) }
}

private fun String.toCharacterGlyphs(
    bounds: Rect,
    lineIndex: Int,
    wordIndex: Int,
    firstIndex: Int,
): List<SelectableGlyph> {
    if (isEmpty()) return emptyList()
    return mapIndexed { characterIndex, character ->
        val left = bounds.left + bounds.width * characterIndex / length
        val right = bounds.left + bounds.width * (characterIndex + 1) / length
        SelectableGlyph(
            index = firstIndex + characterIndex,
            text = character.toString(),
            bounds = Rect(left, bounds.top, right, bounds.bottom),
            lineIndex = lineIndex,
            wordIndex = wordIndex,
        )
    }
}

private fun OcrElement.normalizedRect(imageWidth: Int, imageHeight: Int): Rect? =
    normalizedBounds?.let { Rect(it.left, it.top, it.right, it.bottom) }
        ?: bounds?.takeIf { imageWidth > 0 && imageHeight > 0 }?.let {
            Rect(
                left = it.left.toFloat() / imageWidth,
                top = it.top.toFloat() / imageHeight,
                right = it.right.toFloat() / imageWidth,
                bottom = it.bottom.toFloat() / imageHeight,
            )
        }

private fun OcrSymbol.normalizedRect(imageWidth: Int, imageHeight: Int): Rect? =
    normalizedBounds?.let { Rect(it.left, it.top, it.right, it.bottom) }
        ?: bounds?.takeIf { imageWidth > 0 && imageHeight > 0 }?.let {
            Rect(
                left = it.left.toFloat() / imageWidth,
                top = it.top.toFloat() / imageHeight,
                right = it.right.toFloat() / imageWidth,
                bottom = it.bottom.toFloat() / imageHeight,
            )
        }

private fun List<SelectableGlyph>.wordSelectionAt(index: Int): GlyphSelection {
    val hit = get(index)
    val word = filter { it.lineIndex == hit.lineIndex && it.wordIndex == hit.wordIndex }
    return GlyphSelection(word.first().index, word.last().index)
}

private fun List<SelectableGlyph>.selectedText(selection: GlyphSelection): String {
    val glyphs = this
    return buildString {
        var previous: SelectableGlyph? = null
        selection.ordered.forEach { index ->
            val glyph = glyphs.getOrNull(index) ?: return@forEach
            previous?.let { before ->
                when {
                    before.lineIndex != glyph.lineIndex -> append('\n')
                    before.wordIndex != glyph.wordIndex -> append(' ')
                }
            }
            append(glyph.text)
            previous = glyph
        }
    }
}

private const val IMAGE_MAX_DIMENSION = 4_096
private const val MIN_SCALE = 1f
private const val MAX_SCALE = 6f
private const val DOUBLE_TAP_SCALE = 2.5f
private const val HIT_SLOP_DP = 20
private const val DRAG_SLOP_DP = 40
private const val HANDLE_RADIUS_DP = 7
private const val HANDLE_TOUCH_RADIUS_DP = 28
private const val MAGNIFIER_OFFSET_DP = 96
private const val MAGNIFIER_ZOOM = 1.8f
