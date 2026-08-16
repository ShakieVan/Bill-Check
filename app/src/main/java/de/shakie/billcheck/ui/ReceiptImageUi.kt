package de.shakie.billcheck.ui

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
private fun ContentUriImage(
    uri: Uri,
    maxDimension: Int,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
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
                modifier = Modifier.fillMaxSize(),
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
