package de.shakie.billcheck.data

import de.shakie.billcheck.domain.VerifiedReconciliationReport
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts locally verified reconciliation facts into the representation shown to a language
 * model. Dates are deliberately formatted here instead of asking the model to interpret epochs.
 */
internal fun VerifiedReconciliationReport.toSummaryFactsJson(
    zoneId: ZoneId = ZoneId.systemDefault(),
): JSONObject = JSONObject().apply {
    val dateFormatter = if (languageCode.lowercase(Locale.ROOT) == "de") {
        DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMAN)
    } else {
        DateTimeFormatter.ISO_LOCAL_DATE
    }
    put("title", title)
    put("correctCount", correctCount)
    put("acceptedCount", acceptedCount)
    put("uncertainCount", uncertainCount)
    put("amountMismatchCount", amountMismatchCount)
    put("statementOnlyCount", statementOnlyCount)
    put("receiptOnlyCount", receiptOnlyCount)
    put("recognizedLineCount", recognizedLineCount)
    put("declaredTotalMinor", declaredTotalMinor.orEmpty())
    put("declaredTotalCurrencyCode", declaredTotalCurrencyCode.orEmpty())
    put("declaredTotalDifferenceMinor", declaredTotalDifferenceMinor.orEmpty())
    put("totalCheck", totalCheck)
    put("auditWarnings", JSONArray(auditWarnings))
    put("entries", JSONArray().apply {
        entries.forEach { entry ->
            put(JSONObject().apply {
                put("kind", entry.kind)
                put(
                    "occurredOn",
                    entry.occurredAt?.let { timestamp ->
                        Instant.ofEpochMilli(timestamp)
                            .atZone(zoneId)
                            .toLocalDate()
                            .format(dateFormatter)
                    } ?: JSONObject.NULL,
                )
                put("description", entry.description)
                put("statementCheckNumber", entry.statementCheckNumber.orEmpty())
                put("receiptCheckNumber", entry.receiptCheckNumber.orEmpty())
                put("statementAmountMinor", entry.statementAmountMinor ?: JSONObject.NULL)
                put("receiptAmountMinor", entry.receiptAmountMinor ?: JSONObject.NULL)
                put("currencyCode", entry.currencyCode)
                put("status", entry.status)
            })
        }
    })
}
