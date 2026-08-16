package de.shakie.billcheck.update

object VersionNames {
    fun normalize(raw: String): String = raw
        .trim()
        .removePrefix("refs/tags/")
        .removePrefix("release/")
        .removePrefix("v")
        .substringBefore("-")

    fun compare(candidate: String, current: String): Int {
        val left = parts(candidate)
        val right = parts(current)
        repeat(maxOf(left.size, right.size)) { index ->
            val comparison = (left.getOrNull(index) ?: 0).compareTo(right.getOrNull(index) ?: 0)
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun parts(raw: String): List<Int> = normalize(raw)
        .split('.')
        .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
