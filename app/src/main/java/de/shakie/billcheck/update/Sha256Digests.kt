package de.shakie.billcheck.update

import java.io.File
import java.security.MessageDigest

object Sha256Digests {
    fun verify(file: File, githubDigest: String?): Boolean {
        val expected = parse(githubDigest) ?: return false
        return calculate(file).equals(expected, ignoreCase = true)
    }

    internal fun parse(githubDigest: String?): String? = githubDigest
        ?.takeIf { it.startsWith("sha256:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) }

    internal fun calculate(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
