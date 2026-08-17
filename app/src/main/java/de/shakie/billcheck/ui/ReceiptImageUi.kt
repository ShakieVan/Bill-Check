package de.shakie.billcheck.ui

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.shakie.billcheck.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReceiptImageReview(
    imageUri: Uri,
    modifier: Modifier = Modifier,
    onTakeAnother: () -> Unit,
    onChooseAnother: () -> Unit,
    onBrowseFolders: () -> Unit,
    onUseImage: () -> Unit,
    onClose: () -> Unit,
    onUnlink: (() -> Unit)? = null,
) {
    var showFullscreenImage by remember(imageUri) { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.review_image_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.review_image_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ContentUriImage(
            uri = imageUri,
            maxDimension = 1_600,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showFullscreenImage = true },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onTakeAnother, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.take_another_photo))
            }
            OutlinedButton(onClick = onChooseAnother, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.choose_another_image))
            }
        }
        TextButton(onClick = onBrowseFolders, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.browse_folders))
        }
        Button(onClick = onUseImage, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (onUnlink == null) R.string.use_image else R.string.save_image_link))
        }
        onUnlink?.let { unlink ->
            TextButton(onClick = unlink, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.unlink_image),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (onUnlink == null) R.string.close_image_review else R.string.close))
        }
    }
    if (showFullscreenImage) {
        FullscreenReceiptImage(
            imageUri = imageUri.toString(),
            onDismiss = { showFullscreenImage = false },
        )
    }
}

@Composable
fun ReceiptThumbnail(imageUri: String, modifier: Modifier = Modifier) {
    ContentUriImage(
        uri = Uri.parse(imageUri),
        maxDimension = 320,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
    )
}

@Composable
fun FullscreenReceiptImage(
    imageUri: String,
    onDismiss: () -> Unit,
) {
    var scale by remember(imageUri) { mutableStateOf(1f) }
    var offset by remember(imageUri) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val currentScale by rememberUpdatedState(scale)

    fun constrainedOffset(candidate: Offset, atScale: Float): Offset {
        if (atScale <= 1f) return Offset.Zero
        val maxX = viewport.width * (atScale - 1f) / 2f
        val maxY = viewport.height * (atScale - 1f) / 2f
        return Offset(
            candidate.x.coerceIn(-maxX, maxX),
            candidate.y.coerceIn(-maxY, maxY),
        )
    }

    val transformState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        val previousScale = scale
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        val centroidFromCenter = centroid - Offset(viewport.width / 2f, viewport.height / 2f)
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
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewport = it },
            ) {
                ContentUriImage(
                    uri = Uri.parse(imageUri),
                    maxDimension = 4_096,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    imageModifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                        .transformable(transformState)
                        .pointerInput(imageUri) {
                            detectTapGestures(
                                onDoubleTap = {
                                    scale = if (currentScale > 1f) 1f else 2.5f
                                    offset = Offset.Zero
                                },
                            )
                        },
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding()
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(50)),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White,
                    )
                }
                if (scale <= 1.01f) {
                    Text(
                        text = stringResource(R.string.fullscreen_image_zoom_hint),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .safeDrawingPadding()
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentUriImage(
    uri: Uri,
    maxDimension: Int,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember(uri, maxDimension) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(uri) { mutableStateOf(false) }

    LaunchedEffect(uri, maxDimension) {
        bitmap = null
        failed = false
        runCatching {
            withContext(Dispatchers.IO) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val width = info.size.width
                    val height = info.size.height
                    val largest = maxOf(width, height)
                    if (largest > maxDimension) {
                        val scale = maxDimension.toDouble() / largest
                        decoder.setTargetSize(
                            (width * scale).toInt().coerceAtLeast(1),
                            (height * scale).toInt().coerceAtLeast(1),
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }.asImageBitmap()
            }
        }.onSuccess { bitmap = it }
            .onFailure { failed = true }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(
                bitmap = requireNotNull(bitmap),
                contentDescription = stringResource(R.string.receipt_image),
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize().then(imageModifier),
            )
            failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BrokenImage, contentDescription = null, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.image_unavailable), style = MaterialTheme.typography.bodySmall)
            }
            else -> CircularProgressIndicator()
        }
    }
}
