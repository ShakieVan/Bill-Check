package de.shakie.billcheck.update

import de.shakie.billcheck.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class GitHubReleaseClient {
    fun checkLatestRelease(): UpdateCheckResult = runCatching {
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
            setRequestProperty("User-Agent", "Bill-Check/${BuildConfig.VERSION_NAME}")
        }
        connection.use {
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return UpdateCheckResult(UpdateCheckStatus.NO_RELEASE)
            }
            if (responseCode !in 200..299) {
                return UpdateCheckResult(
                    UpdateCheckStatus.CHECK_FAILED,
                    message = "GitHub HTTP $responseCode",
                )
            }
            val release = parseRelease(readBody(connection))
            when {
                VersionNames.compare(release.versionName, BuildConfig.VERSION_NAME) <= 0 ->
                    UpdateCheckResult(UpdateCheckStatus.UP_TO_DATE, release)
                release.compatibleAsset == null ->
                    UpdateCheckResult(UpdateCheckStatus.NO_COMPATIBLE_ASSET, release)
                else -> UpdateCheckResult(UpdateCheckStatus.UPDATE_AVAILABLE, release)
            }
        }
    }.getOrElse { throwable ->
        UpdateCheckResult(
            UpdateCheckStatus.CHECK_FAILED,
            message = throwable.message ?: throwable.javaClass.simpleName,
        )
    }

    internal fun parseRelease(json: String): UpdateRelease {
        val root = JSONObject(json)
        val tagName = root.optString("tag_name")
        val assetsJson = root.optJSONArray("assets")
        val assets = buildList {
            if (assetsJson != null) repeat(assetsJson.length()) { index ->
                val asset = assetsJson.getJSONObject(index)
                add(
                    UpdateAsset(
                        name = asset.optString("name"),
                        downloadUrl = asset.optString("browser_download_url"),
                        sizeBytes = asset.optLong("size", 0L),
                        digest = asset.optString("digest").takeIf(String::isNotBlank),
                    ),
                )
            }
        }
        return UpdateRelease(
            tagName = tagName,
            versionName = VersionNames.normalize(tagName),
            htmlUrl = root.optString("html_url"),
            body = root.optString("body"),
            assets = assets,
        )
    }

    private fun readBody(connection: HttpURLConnection): String =
        BufferedReader(InputStreamReader(connection.inputStream)).use(BufferedReader::readText)

    private inline fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T =
        try {
            block()
        } finally {
            disconnect()
        }

    private companion object {
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/ShakieVan/Bill-Check/releases/latest"
    }
}
