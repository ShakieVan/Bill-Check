package de.shakie.billcheck.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object UpdateInstallHelper {
    fun canRequestPackageInstalls(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun installSettingsIntent(context: Context): Intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}"),
    )

    fun installIntent(context: Context, apkFile: File): Intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            ),
            "application/vnd.android.package-archive",
        )
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
