package de.shakie.billcheck.update

data class UpdateAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val digest: String?,
)

data class UpdateRelease(
    val tagName: String,
    val versionName: String,
    val htmlUrl: String,
    val body: String,
    val assets: List<UpdateAsset>,
) {
    val compatibleAsset: UpdateAsset?
        get() = UpdateAssetSelector.select(assets)
}

enum class UpdateCheckStatus {
    UPDATE_AVAILABLE,
    UP_TO_DATE,
    NO_RELEASE,
    NO_COMPATIBLE_ASSET,
    CHECK_FAILED,
}

data class UpdateCheckResult(
    val status: UpdateCheckStatus,
    val release: UpdateRelease? = null,
    val message: String? = null,
)

object UpdateAssetSelector {
    fun select(assets: List<UpdateAsset>): UpdateAsset? {
        val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        return apks.firstOrNull { it.name.contains("universal", ignoreCase = true) }
            ?: apks.singleOrNull()
    }
}
