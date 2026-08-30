package de.shakie.billcheck.domain

import java.text.Normalizer
import java.util.Currency
import java.util.Locale

/** A currently used ISO-4217 currency and the countries or regions using it. */
data class CurrencyCatalogEntry(
    val code: String,
    val name: String,
    val regions: List<String>,
    val defaultFractionDigits: Int,
) {
    internal val searchableText: String
        get() = listOf(code, name, regions.joinToString(" ")).joinToString(" ").searchKey()
}

/**
 * Offline currency catalog backed by the ISO country and currency data shipped with Android.
 *
 * Building the catalog from currencies assigned to countries intentionally excludes accounting,
 * precious-metal and historic codes that are not useful when entering travel receipts.
 */
object CurrencyCatalog {
    fun entries(locale: Locale = Locale.getDefault()): List<CurrencyCatalogEntry> {
        val regionsByCurrency = buildMap<String, MutableSet<String>> {
            Locale.getISOCountries().forEach { countryCode ->
                val countryLocale = Locale.Builder().setRegion(countryCode).build()
                val currency = runCatching { Currency.getInstance(countryLocale) }.getOrNull()
                    ?: return@forEach
                if (currency.currencyCode == NO_CURRENCY_CODE) return@forEach

                val regionName = countryLocale.getDisplayCountry(locale).ifBlank { countryCode }
                getOrPut(currency.currencyCode) { linkedSetOf() }.add(regionName)
            }
        }

        return regionsByCurrency.mapNotNull { (code, regions) ->
            val currency = runCatching { Currency.getInstance(code) }.getOrNull()
                ?: return@mapNotNull null
            if (currency.defaultFractionDigits < 0) return@mapNotNull null
            CurrencyCatalogEntry(
                code = currency.currencyCode,
                name = currency.getDisplayName(locale).ifBlank { currency.currencyCode },
                regions = regions.sortedBy(String::searchKey),
                defaultFractionDigits = currency.defaultFractionDigits,
            )
        }.sortedWith(entryComparator(emptyMap()))
    }

    /**
     * Searches code, localized name and every localized country/region name. All whitespace-
     * separated query terms must match, while accents and case are ignored. [prioritizedCodes]
     * retain their supplied order ahead of the alphabetical remainder.
     */
    fun search(
        entries: List<CurrencyCatalogEntry>,
        query: String,
        prioritizedCodes: List<String> = emptyList(),
    ): List<CurrencyCatalogEntry> {
        val terms = query.searchKey().split(WHITESPACE).filter(String::isNotBlank)
        val priorities = prioritizedCodes
            .map { it.trim().uppercase(Locale.ROOT) }
            .distinct()
            .withIndex()
            .associate { (index, code) -> code to index }

        return entries.asSequence()
            .filter { entry -> terms.all(entry.searchableText::contains) }
            .sortedWith(entryComparator(priorities))
            .toList()
    }

    private fun entryComparator(priorities: Map<String, Int>): Comparator<CurrencyCatalogEntry> =
        compareBy<CurrencyCatalogEntry> { priorities[it.code] ?: Int.MAX_VALUE }
            .thenBy { it.name.searchKey() }
            .thenBy(CurrencyCatalogEntry::code)

    private const val NO_CURRENCY_CODE = "XXX"
    private val WHITESPACE = Regex("\\s+")
}

private fun String.searchKey(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(COMBINING_MARKS, "")
    .lowercase(Locale.ROOT)
    .trim()

private val COMBINING_MARKS = Regex("\\p{M}+")
