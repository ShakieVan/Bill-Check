package de.shakie.billcheck.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Locale

data class ValidatedStatementLine(
    val description: String,
    val checkNumber: String,
    val amountMinor: Long,
    val currencyCode: String,
    val occurredOn: Long?,
    val sourceDateText: String?,
    val dateAmbiguous: Boolean,
)

data class ValidatedExtractedStatement(
    val title: String,
    val declaredTotalMinor: Long?,
    val declaredTotalCurrencyCode: String?,
    val lines: List<ValidatedStatementLine>,
)

class StatementExtractionValidationException(val problems: List<String>) :
    IllegalArgumentException(problems.joinToString(separator = "\n"))

object StatementExtractionValidator {
    private const val MAX_LINES = 1_000
    private const val MAX_TEXT_LENGTH = 500
    private val supportedCurrencies by lazy {
        CurrencyCatalog.entries(Locale.ROOT).mapTo(hashSetOf(), CurrencyCatalogEntry::code)
    }

    fun validate(
        extracted: ExtractedStatement,
        fallbackCurrencyCode: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ValidatedExtractedStatement {
        val problems = mutableListOf<String>()
        val fallbackCurrency = normalizeCurrency(fallbackCurrencyCode).also {
            if (it == null) problems += "Fallback currency is invalid"
        }
        if (extracted.lines.isEmpty()) problems += "No statement charge lines were extracted"
        if (extracted.lines.size > MAX_LINES) problems += "Statement has more than $MAX_LINES lines"

        val lines = extracted.lines.mapIndexedNotNull { index, line ->
            val prefix = "Line ${index + 1}"
            val currency = when {
                line.currencyCode.isBlank() -> fallbackCurrency
                else -> normalizeCurrency(line.currencyCode).also {
                    if (it == null) problems += "$prefix has an invalid currency: ${line.currencyCode.take(12)}"
                }
            }
            val amount = currency?.let { parsePositiveMinor(line.amountText, it) }
                ?: run {
                    if (currency != null) {
                        problems += "$prefix has an invalid positive amount: ${line.amountText.take(40)}"
                    }
                    null
                }
            val sourceDate = line.sourceDateText.trim().takeIf(String::isNotEmpty)
            val occurredOn = when {
                line.dateAmbiguous -> {
                    if (line.occurredOn.isNotBlank()) {
                        problems += "$prefix marks its date ambiguous but also normalizes it"
                    }
                    null
                }
                line.occurredOn.isBlank() -> {
                    if (sourceDate != null) problems += "$prefix has an unclassified printed date"
                    null
                }
                else -> parseIsoDate(line.occurredOn, zoneId).also {
                    if (it == null) problems += "$prefix has an invalid ISO date: ${line.occurredOn.take(40)}"
                }
            }
            if (line.description.length > MAX_TEXT_LENGTH) problems += "$prefix description is too long"
            if (line.checkNumber.length > MAX_TEXT_LENGTH) problems += "$prefix check number is too long"
            if (sourceDate != null && sourceDate.length > 80) problems += "$prefix printed date is too long"
            if (amount == null || currency == null) return@mapIndexedNotNull null
            ValidatedStatementLine(
                description = line.description.trim().take(MAX_TEXT_LENGTH),
                checkNumber = line.checkNumber.trim().take(MAX_TEXT_LENGTH),
                amountMinor = amount,
                currencyCode = currency,
                occurredOn = occurredOn,
                sourceDateText = sourceDate?.take(80),
                dateAmbiguous = line.dateAmbiguous,
            )
        }

        val hasDeclaredTotal = extracted.declaredTotalAmountText.isNotBlank()
        val declaredCurrency = when {
            !hasDeclaredTotal -> null
            extracted.declaredTotalCurrencyCode.isBlank() -> fallbackCurrency
            else -> normalizeCurrency(extracted.declaredTotalCurrencyCode).also {
                if (it == null) problems += "Declared statement currency is invalid"
            }
        }
        val declaredTotalMinor = when {
            !hasDeclaredTotal -> null
            declaredCurrency == null -> null
            else -> parsePositiveMinor(extracted.declaredTotalAmountText, declaredCurrency).also {
                if (it == null) problems += "Declared statement total is invalid"
            }
        }
        if (problems.isNotEmpty()) throw StatementExtractionValidationException(problems)
        return ValidatedExtractedStatement(
            title = extracted.title.trim().take(MAX_TEXT_LENGTH),
            declaredTotalMinor = declaredTotalMinor,
            declaredTotalCurrencyCode = declaredCurrency,
            lines = lines,
        )
    }

    internal fun parsePositiveMinor(value: String, currencyCode: String): Long? =
        CurrencyAmount.parseMajorToMinor(value, currencyCode)?.takeIf { it > 0 }

    private fun normalizeCurrency(value: String): String? = value.trim().uppercase(Locale.ROOT)
        .takeIf(supportedCurrencies::contains)

    private fun parseIsoDate(value: String, zoneId: ZoneId): Long? = try {
        LocalDate.parse(value.trim()).atStartOfDay(zoneId).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}
