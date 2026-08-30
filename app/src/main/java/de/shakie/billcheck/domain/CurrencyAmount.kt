package de.shakie.billcheck.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyAmount {
    /** ISO 4217 fraction digits (JPY=0, EUR=2, KWD=3). */
    fun fractionDigits(currencyCode: String): Int = runCatching {
        Currency.getInstance(normalizeCode(currencyCode)).defaultFractionDigits
    }.getOrDefault(2).takeIf { it in 0..3 } ?: 2

    fun normalizeCode(currencyCode: String): String =
        currencyCode.trim().uppercase(Locale.ROOT).also {
            require(it.length == 3 && it.all(Char::isLetter)) { "Invalid currency code: $currencyCode" }
        }

    fun minorFactor(currencyCode: String): BigDecimal =
        BigDecimal.TEN.pow(fractionDigits(currencyCode))

    fun parseMajorToMinor(text: String, currencyCode: String): Long? {
        val compact = text.trim()
        if (compact.isEmpty()) return null
        if (compact.any { it.isWhitespace() || it == '\u00a0' }) return null
        val separators = compact.withIndex().filter { it.value == ',' || it.value == '.' }
        if (separators.size > 1) return null // Deliberately no ambiguous grouping separators.
        val separatorIndex = separators.singleOrNull()?.index
        val fractionDigits = fractionDigits(currencyCode)
        if (separatorIndex != null && compact.length - separatorIndex - 1 > fractionDigits) return null
        if (separatorIndex != null && fractionDigits == 0) return null
        val normalized = compact.replace(',', '.')
        if (!normalized.withIndex().all { (index, character) ->
                character.isDigit() || character == '.' || (character == '-' && index == 0)
            }
        ) return null
        return normalized.toBigDecimalOrNull()
            ?.movePointRight(fractionDigits)
            ?.setScale(0, RoundingMode.UNNECESSARY)
            ?.runCatching { longValueExact() }
            ?.getOrNull()
    }

    /** Normalizes a selected OCR fragment without assuming two decimal places. */
    fun normalizeOcrMajorText(text: String, currencyCode: String): String {
        val candidate = text.filter { it.isDigit() || it == ',' || it == '.' }
        if (candidate.isEmpty()) return text
        val digits = fractionDigits(currencyCode)
        if (digits == 0) return candidate.filter(Char::isDigit)
        val decimalIndex = maxOf(candidate.lastIndexOf(','), candidate.lastIndexOf('.'))
        if (decimalIndex < 0) return candidate
        val decimals = candidate.substring(decimalIndex + 1).filter(Char::isDigit)
        if (decimals.length !in 1..digits) return candidate.filter(Char::isDigit)
        val whole = candidate.substring(0, decimalIndex).filter(Char::isDigit).ifBlank { "0" }
        return "$whole.$decimals"
    }

    fun formatMinor(minor: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val code = normalizeCode(currencyCode)
        return NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(code)
            minimumFractionDigits = fractionDigits(code)
            maximumFractionDigits = fractionDigits(code)
        }.format(BigDecimal.valueOf(minor).movePointLeft(fractionDigits(code)))
    }
}
