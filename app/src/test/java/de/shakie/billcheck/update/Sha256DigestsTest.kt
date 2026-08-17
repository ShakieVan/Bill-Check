package de.shakie.billcheck.update

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Sha256DigestsTest {
    @Test
    fun `accepts matching GitHub digest and rejects changed file`() {
        val file = File.createTempFile("bill-check-update", ".apk")
        try {
            file.writeText("verified update")
            val digest = "sha256:${Sha256Digests.calculate(file)}"
            assertTrue(Sha256Digests.verify(file, digest))
            file.appendText(" changed")
            assertFalse(Sha256Digests.verify(file, digest))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `rejects missing and malformed digest`() {
        val file = File.createTempFile("bill-check-update", ".apk")
        try {
            assertFalse(Sha256Digests.verify(file, null))
            assertFalse(Sha256Digests.verify(file, "md5:${"a".repeat(32)}"))
        } finally {
            file.delete()
        }
    }
}
