package de.shakie.billcheck.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateAssetSelectorTest {
    @Test
    fun `prefers explicitly universal apk`() {
        val selected = UpdateAssetSelector.select(
            listOf(asset("checksums.txt"), asset("other.apk"), asset("Bill-Check-universal.apk")),
        )
        assertEquals("Bill-Check-universal.apk", selected?.name)
    }

    @Test
    fun `accepts sole apk and rejects ambiguous apk set`() {
        assertEquals("Bill-Check.apk", UpdateAssetSelector.select(listOf(asset("Bill-Check.apk")))?.name)
        assertNull(UpdateAssetSelector.select(listOf(asset("a.apk"), asset("b.apk"))))
    }

    private fun asset(name: String) = UpdateAsset(name, "https://example.invalid/$name", 1, null)
}
