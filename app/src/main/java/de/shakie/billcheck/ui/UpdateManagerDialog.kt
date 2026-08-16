package de.shakie.billcheck.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.shakie.billcheck.BuildConfig
import de.shakie.billcheck.R
import de.shakie.billcheck.update.UpdateInstallHelper
import java.io.File
import kotlin.math.roundToInt

@Composable
fun UpdateManagerDialog(
    state: AppUpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val release = state.release
    val progress = if (state.totalBytes > 0L) {
        (state.downloadedBytes.toFloat() / state.totalBytes).coerceIn(0f, 1f)
    } else {
        0f
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 16.dp, end = 10.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.update_manager_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onCheck,
                        enabled = state.status != AppUpdateStatus.CHECKING &&
                            state.status != AppUpdateStatus.DOWNLOADING,
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.update_check))
                    }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.update_current_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (release != null) {
                        Text(stringResource(R.string.update_latest_version, release.versionName))
                    }

                    when (state.status) {
                        AppUpdateStatus.IDLE -> Text(stringResource(R.string.update_privacy_hint))
                        AppUpdateStatus.CHECKING -> Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator()
                            Text(stringResource(R.string.update_checking))
                        }
                        AppUpdateStatus.NO_RELEASE -> Text(stringResource(R.string.update_no_release))
                        AppUpdateStatus.UP_TO_DATE -> Text(stringResource(R.string.update_up_to_date))
                        AppUpdateStatus.NO_COMPATIBLE_ASSET ->
                            Text(stringResource(R.string.update_no_compatible_asset), color = MaterialTheme.colorScheme.error)
                        AppUpdateStatus.AVAILABLE -> {
                            Text(stringResource(R.string.update_available))
                            Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.update_download))
                            }
                        }
                        AppUpdateStatus.DOWNLOADING -> {
                            Text(stringResource(R.string.update_downloading, (progress * 100).roundToInt()))
                            if (state.totalBytes > 0L) {
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            OutlinedButton(onClick = onCancelDownload, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.update_cancel_download))
                            }
                        }
                        AppUpdateStatus.READY_TO_INSTALL -> {
                            Text(stringResource(R.string.update_verified), color = MaterialTheme.colorScheme.primary)
                            Button(
                                onClick = {
                                    val path = state.downloadedFilePath ?: return@Button
                                    try {
                                        if (UpdateInstallHelper.canRequestPackageInstalls(context)) {
                                            context.startActivity(UpdateInstallHelper.installIntent(context, File(path)))
                                        } else {
                                            context.startActivity(UpdateInstallHelper.installSettingsIntent(context))
                                        }
                                    } catch (error: ActivityNotFoundException) {
                                        Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    if (UpdateInstallHelper.canRequestPackageInstalls(context)) {
                                        stringResource(R.string.update_install)
                                    } else {
                                        stringResource(R.string.update_open_install_settings)
                                    },
                                )
                            }
                            if (!UpdateInstallHelper.canRequestPackageInstalls(context)) {
                                Text(
                                    stringResource(R.string.update_install_permission),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = onDeleteDownload, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.update_delete_file), color = MaterialTheme.colorScheme.error)
                            }
                        }
                        AppUpdateStatus.ERROR -> Text(
                            stringResource(
                                if (release == null) R.string.update_check_failed else R.string.update_download_failed,
                                state.message.orEmpty(),
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    if (release != null) {
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Text(stringResource(R.string.update_release_notes), style = MaterialTheme.typography.titleMedium)
                        Text(release.body.ifBlank { stringResource(R.string.update_release_notes_empty) })
                        if (release.htmlUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = { uriHandler.openUri(release.htmlUrl) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.update_open_release_notes))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.update_privacy_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                }
            }
        }
    }
}
