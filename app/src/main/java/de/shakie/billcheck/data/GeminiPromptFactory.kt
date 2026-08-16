package de.shakie.billcheck.data

import de.shakie.billcheck.domain.AiDocumentType

internal object GeminiPromptFactory {
    fun create(documentType: AiDocumentType, expectedCurrencyCode: String): String =
        when (documentType) {
            AiDocumentType.RECEIPT -> """
                Extract this hotel bar or restaurant receipt exactly. The expected currency is
                $expectedCurrencyCode. Do not include room number, signature, or handwritten tip.
                For location return only the specific restaurant, bar, lounge, pool, or beach venue
                where the charge occurred. Exclude the hotel or resort name, city, region, country,
                street, and other address text. For example, prefer "Sunset Lobby" over
                "Utopia Beach Club, Marsa Alam, Sunset Lobby".
                Keep decimal amounts as plain strings without currency symbols. Use YYYY-MM-DD for
                dates and an empty string when a value is not visible. Item amounts must be the
                printed line totals. Never invent missing values.
            """.trimIndent()

            AiDocumentType.STATEMENT -> """
                Extract every charge line from this hotel interim or final statement. The expected
                currency is $expectedCurrencyCode. Ignore headings, subtotals, payments and final
                totals. Preserve check numbers exactly as printed. Keep amounts as positive decimal
                strings without symbols, dates as YYYY-MM-DD, and empty strings for missing values.
                Never invent a line or value.
            """.trimIndent()
        }
}
