package de.shakie.billcheck.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionNamesTest {
    @Test
    fun `normalizes tags and build suffixes`() {
        assertEquals("0.2.0", VersionNames.normalize(" refs/tags/v0.2.0 "))
        assertEquals("1.0.0", VersionNames.normalize("release/v1.0.0-debug"))
    }

    @Test
    fun `compares numeric version parts`() {
        assertTrue(VersionNames.compare("v0.2.0", "0.1.9") > 0)
        assertTrue(VersionNames.compare("0.1.0", "0.2.0-debug") < 0)
        assertEquals(0, VersionNames.compare("0.1", "0.1.0"))
    }
}
