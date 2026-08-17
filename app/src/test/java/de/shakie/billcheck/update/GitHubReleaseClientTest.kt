package de.shakie.billcheck.update

import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubReleaseClientTest {
    @Test
    fun `parses current GitHub asset digest`() {
        val release = GitHubReleaseClient().parseRelease(
            """{"tag_name":"v0.2.0","html_url":"https://example.invalid/release","body":"Neu","assets":[{"name":"Bill-Check-v0.2.0-universal.apk","browser_download_url":"https://example.invalid/app.apk","size":42,"digest":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]}""",
        )
        assertEquals("0.2.0", release.versionName)
        assertEquals("sha256:" + "a".repeat(64), release.compatibleAsset?.digest)
    }
}
