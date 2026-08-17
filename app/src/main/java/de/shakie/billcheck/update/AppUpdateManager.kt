package de.shakie.billcheck.update

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class AppUpdateManager(context: Context) {
    private val applicationContext = context.applicationContext
    private val client = GitHubReleaseClient()
    private val preferences = UpdatePreferences(applicationContext)

    suspend fun check(force: Boolean): UpdateCheckResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!force && !preferences.shouldCheck(now)) {
            return@withContext UpdateCheckResult(UpdateCheckStatus.UP_TO_DATE)
        }
        client.checkLatestRelease().also {
            if (it.status != UpdateCheckStatus.CHECK_FAILED) preferences.lastCheckAtMillis = now
        }
    }

    fun downloadedApkFor(release: UpdateRelease): File? = preferences.downloadedApkFor(release)

    suspend fun download(
        release: UpdateRelease,
        asset: UpdateAsset,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        if (Sha256Digests.parse(asset.digest) == null) {
            throw IllegalStateException("GitHub liefert für diese APK keine SHA-256-Prüfsumme.")
        }
        val directory = File(applicationContext.filesDir, "updates/${safe(release.tagName)}")
        val target = File(directory, safe(asset.name))
        val partial = File(directory, "${safe(asset.name)}.part")
        directory.mkdirs()
        partial.delete()
        try {
            val connection = (URL(asset.downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Bill-Check")
            }
            connection.use {
                if (responseCode !in 200..299) throw IllegalStateException("Download HTTP $responseCode")
                val total = contentLengthLong.takeIf { it > 0 } ?: asset.sizeBytes
                var downloaded = 0L
                inputStream.use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, total)
                        }
                    }
                }
                if (asset.sizeBytes > 0 && partial.length() != asset.sizeBytes) {
                    throw IllegalStateException("Die heruntergeladene Dateigröße stimmt nicht.")
                }
            }
            if (!Sha256Digests.verify(partial, asset.digest)) {
                throw IllegalStateException("Die SHA-256-Prüfsumme der APK stimmt nicht.")
            }
            target.delete()
            if (!partial.renameTo(target)) throw IllegalStateException("Die APK konnte nicht übernommen werden.")
            preferences.saveDownloadedApk(release, target)
            target
        } catch (throwable: Throwable) {
            partial.delete()
            throw throwable
        }
    }

    fun deleteDownloadedApk(release: UpdateRelease) {
        downloadedApkFor(release)?.delete()
        preferences.clearDownloadedApk()
    }

    private fun safe(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private inline fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T =
        try {
            block()
        } finally {
            disconnect()
        }
}
