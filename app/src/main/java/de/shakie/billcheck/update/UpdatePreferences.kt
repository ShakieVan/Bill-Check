package de.shakie.billcheck.update

import android.content.Context
import java.io.File

class UpdatePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var lastCheckAtMillis: Long
        get() = preferences.getLong(KEY_LAST_CHECK, 0L)
        set(value) = preferences.edit().putLong(KEY_LAST_CHECK, value).apply()

    fun shouldCheck(nowMillis: Long): Boolean =
        nowMillis - lastCheckAtMillis >= AUTO_CHECK_INTERVAL_MILLIS

    fun saveDownloadedApk(release: UpdateRelease, file: File) {
        preferences.edit()
            .putString(KEY_DOWNLOADED_TAG, release.tagName)
            .putString(KEY_DOWNLOADED_PATH, file.absolutePath)
            .apply()
    }

    fun downloadedApkFor(release: UpdateRelease): File? {
        if (preferences.getString(KEY_DOWNLOADED_TAG, null) != release.tagName) return null
        return preferences.getString(KEY_DOWNLOADED_PATH, null)
            ?.let(::File)
            ?.takeIf(File::isFile)
    }

    fun clearDownloadedApk() {
        preferences.edit().remove(KEY_DOWNLOADED_TAG).remove(KEY_DOWNLOADED_PATH).apply()
    }

    private companion object {
        const val NAME = "bill_check_update_preferences"
        const val KEY_LAST_CHECK = "last_check_at_millis"
        const val KEY_DOWNLOADED_TAG = "downloaded_tag"
        const val KEY_DOWNLOADED_PATH = "downloaded_path"
        const val AUTO_CHECK_INTERVAL_MILLIS = 24 * 60 * 60 * 1_000L
    }
}
