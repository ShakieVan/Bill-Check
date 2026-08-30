package de.shakie.billcheck.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvTransferCodec {
    private val columns = listOf(
        "recordType", "tripId", "tripName", "homeCurrency", "currencyRate", "exchangeRateMode", "isDefaultCurrency",
        "defaultTipMinor", "defaultTipCurrency", "defaultTipSelected", "createdAt", "receiptId", "occurredAt", "date",
        "location", "checkNumber", "amountMinor", "currency", "receiptRate", "exactHomeMinor", "tipMinor", "tipCurrency", "tipRate",
        "reconciliationId", "reconciliationTitle", "lineId", "description", "status", "accepted",
        "matchedReceiptId", "matchedManually", "analysisSummary", "analysisUpdatedAt",
        "aiSuggestedReceiptId", "aiConfidence", "aiReason",
        "declaredTotalMinor", "declaredTotalCurrency", "sourceDateText", "dateAmbiguous",
    )

    fun encode(value: TransferPackage): String = buildString {
        append('\uFEFF')
        appendRow(listOf("Bill Check", "CSV", "2"))
        appendRow(columns)
        value.trips.forEach { trip ->
            appendRecord("TRIP", trip = trip)
            trip.currencies.forEach { currency -> appendRecord("CURRENCY", trip, tripCurrency = currency) }
            trip.receipts.forEach { receipt -> appendRecord("RECEIPT", trip, receipt = receipt) }
            trip.reconciliations.forEach { reconciliation ->
                appendRecord("RECONCILIATION", trip, reconciliation = reconciliation)
                reconciliation.lines.forEach { line ->
                    appendRecord("LINE", trip, reconciliation = reconciliation, line = line)
                }
            }
        }
    }

    fun decode(text: String): TransferPackage {
        val rows = parseRows(text.removePrefix("\uFEFF"))
        require(rows.firstOrNull()?.take(3) == listOf("Bill Check", "CSV", "2")) {
            "Unsupported Bill Check CSV"
        }
        val header = rows.getOrNull(1).orEmpty()
        val indices = header.withIndex().associate { it.value to it.index }
        fun List<String>.value(name: String): String = indices[name]?.let { getOrNull(it) }
            .orEmpty().removeSpreadsheetGuard()

        val records = rows.drop(2).filter { it.isNotEmpty() }
        val trips = records.filter { it.value("recordType") == "TRIP" }.map { tripRow ->
            val tripId = tripRow.value("tripId")
            val receiptRows = records.filter {
                it.value("recordType") == "RECEIPT" && it.value("tripId") == tripId
            }
            val reconciliationRows = records.filter {
                it.value("recordType") == "RECONCILIATION" && it.value("tripId") == tripId
            }
            val currencyRows = records.filter {
                it.value("recordType") == "CURRENCY" && it.value("tripId") == tripId
            }
            TransferTrip(
                id = tripId,
                name = tripRow.value("tripName"),
                homeCurrencyCode = tripRow.value("homeCurrency"),
                defaultTipMinor = tripRow.value("defaultTipMinor").toLongOrNull() ?: 100,
                defaultTipCurrencyCode = tripRow.value("defaultTipCurrency").ifBlank {
                    tripRow.value("homeCurrency")
                },
                defaultTipSelected = tripRow.value("defaultTipSelected").toBooleanStrictOrNull() ?: false,
                imageStorageMode = "ORIGINAL",
                createdAt = tripRow.value("createdAt").toLongOrNull() ?: System.currentTimeMillis(),
                currencies = currencyRows.map { row ->
                    TransferTripCurrency(
                        currencyCode = row.value("currency"),
                        homeToCurrencyRate = row.value("currencyRate"),
                        exchangeRateMode = row.value("exchangeRateMode").ifBlank { "FIXED" },
                        isDefault = row.value("isDefaultCurrency").toBooleanStrictOrNull() ?: false,
                    )
                },
                receipts = receiptRows.map { row ->
                    TransferReceipt(
                        id = row.value("receiptId"),
                        occurredAt = row.value("occurredAt").toLongOrNull() ?: 0,
                        location = row.value("location"),
                        checkNumber = row.value("checkNumber"),
                        amountMinor = row.value("amountMinor").toLongOrNull() ?: 0,
                        currencyCode = row.value("currency"),
                        exchangeRateSnapshot = row.value("receiptRate"),
                        exactHomeMinor = row.value("exactHomeMinor").toLongOrNull() ?: 0,
                        tipMinor = row.value("tipMinor").toLongOrNull() ?: 0,
                        tipCurrencyCode = row.value("tipCurrency").ifBlank {
                            row.value("defaultTipCurrency").ifBlank { tripRow.value("homeCurrency") }
                        },
                        tipExchangeRateSnapshot = row.value("tipRate").ifBlank { "1" },
                        imageEntry = null,
                        imageMimeType = null,
                        reviewState = "CONFIRMED",
                        createdAt = row.value("createdAt").toLongOrNull() ?: 0,
                        items = emptyList(),
                    )
                },
                reconciliations = reconciliationRows.map { row ->
                    val reconciliationId = row.value("reconciliationId")
                    val lineRows = records.filter {
                        it.value("recordType") == "LINE" &&
                            it.value("reconciliationId") == reconciliationId
                    }
                    TransferReconciliation(
                        id = reconciliationId,
                        title = row.value("reconciliationTitle"),
                        statementImageEntry = null,
                        statementImageMimeType = null,
                        createdAt = row.value("createdAt").toLongOrNull() ?: 0,
                        analysisSummary = row.value("analysisSummary").ifBlank { null },
                        analysisUpdatedAt = row.value("analysisUpdatedAt").toLongOrNull(),
                        declaredTotalMinor = row.value("declaredTotalMinor").toLongOrNull(),
                        declaredTotalCurrencyCode = row.value("declaredTotalCurrency").ifBlank { null },
                        lines = lineRows.map { line ->
                            TransferStatementLine(
                                id = line.value("lineId"),
                                occurredOn = line.value("occurredAt").toLongOrNull(),
                                description = line.value("description"),
                                checkNumber = line.value("checkNumber"),
                                amountMinor = line.value("amountMinor").toLongOrNull() ?: 0,
                                currencyCode = line.value("currency"),
                                status = line.value("status").ifBlank { "NOT_FOUND" },
                                acceptedWithoutReceipt = line.value("accepted").toBooleanStrictOrNull() ?: false,
                                matchedReceiptId = line.value("matchedReceiptId").ifBlank { null },
                                matchedManually = line.value("matchedManually").toBooleanStrictOrNull() ?: false,
                                aiSuggestedReceiptId = line.value("aiSuggestedReceiptId").ifBlank { null },
                                aiConfidence = line.value("aiConfidence").toIntOrNull(),
                                aiReason = line.value("aiReason").ifBlank { null },
                                sourceDateText = line.value("sourceDateText").ifBlank { null },
                                dateAmbiguous = line.value("dateAmbiguous").toBooleanStrictOrNull() ?: false,
                            )
                        },
                    )
                },
            )
        }
        require(trips.isNotEmpty()) { "CSV contains no trips" }
        return TransferPackage(exportedAt = System.currentTimeMillis(), trips = trips)
    }

    private fun StringBuilder.appendRecord(
        type: String,
        trip: TransferTrip,
        tripCurrency: TransferTripCurrency? = null,
        receipt: TransferReceipt? = null,
        reconciliation: TransferReconciliation? = null,
        line: TransferStatementLine? = null,
    ) {
        val values = mapOf(
            "recordType" to type,
            "tripId" to trip.id,
            "tripName" to trip.name,
            "homeCurrency" to trip.homeCurrencyCode,
            "currencyRate" to tripCurrency?.homeToCurrencyRate.orEmpty(),
            "exchangeRateMode" to tripCurrency?.exchangeRateMode.orEmpty(),
            "isDefaultCurrency" to tripCurrency?.isDefault?.toString().orEmpty(),
            "defaultTipMinor" to trip.defaultTipMinor.toString(),
            "defaultTipCurrency" to trip.defaultTipCurrencyCode,
            "defaultTipSelected" to trip.defaultTipSelected.toString(),
            "createdAt" to (receipt?.createdAt ?: reconciliation?.createdAt ?: trip.createdAt).toString(),
            "receiptId" to receipt?.id.orEmpty(),
            "occurredAt" to (receipt?.occurredAt ?: line?.occurredOn)?.toString().orEmpty(),
            "date" to formatDate(receipt?.occurredAt ?: line?.occurredOn),
            "location" to receipt?.location.orEmpty(),
            "checkNumber" to (receipt?.checkNumber ?: line?.checkNumber).orEmpty(),
            "amountMinor" to (receipt?.amountMinor ?: line?.amountMinor)?.toString().orEmpty(),
            "currency" to (tripCurrency?.currencyCode ?: receipt?.currencyCode ?: line?.currencyCode).orEmpty(),
            "receiptRate" to receipt?.exchangeRateSnapshot.orEmpty(),
            "exactHomeMinor" to receipt?.exactHomeMinor?.toString().orEmpty(),
            "tipMinor" to receipt?.tipMinor?.toString().orEmpty(),
            "tipCurrency" to receipt?.tipCurrencyCode.orEmpty(),
            "tipRate" to receipt?.tipExchangeRateSnapshot.orEmpty(),
            "reconciliationId" to reconciliation?.id.orEmpty(),
            "reconciliationTitle" to reconciliation?.title.orEmpty(),
            "lineId" to line?.id.orEmpty(),
            "description" to line?.description.orEmpty(),
            "status" to line?.status.orEmpty(),
            "accepted" to line?.acceptedWithoutReceipt?.toString().orEmpty(),
            "matchedReceiptId" to line?.matchedReceiptId.orEmpty(),
            "matchedManually" to line?.matchedManually?.toString().orEmpty(),
            "analysisSummary" to reconciliation?.analysisSummary.orEmpty(),
            "analysisUpdatedAt" to reconciliation?.analysisUpdatedAt?.toString().orEmpty(),
            "aiSuggestedReceiptId" to line?.aiSuggestedReceiptId.orEmpty(),
            "aiConfidence" to line?.aiConfidence?.toString().orEmpty(),
            "aiReason" to line?.aiReason.orEmpty(),
            "declaredTotalMinor" to reconciliation?.declaredTotalMinor?.toString().orEmpty(),
            "declaredTotalCurrency" to reconciliation?.declaredTotalCurrencyCode.orEmpty(),
            "sourceDateText" to line?.sourceDateText.orEmpty(),
            "dateAmbiguous" to line?.dateAmbiguous?.toString().orEmpty(),
        )
        appendRow(columns.map { values[it].orEmpty() })
    }

    private fun StringBuilder.appendRow(values: List<String>) {
        append(values.joinToString(";") { value ->
            val safeValue = value.addSpreadsheetGuard()
            "\"${safeValue.replace("\"", "\"\"")}\""
        })
        append("\r\n")
    }

    private fun formatDate(value: Long?): String = value?.let {
        DateTimeFormatter.ISO_LOCAL_DATE.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
    }.orEmpty()

    private fun parseRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val character = text[index]
            when {
                character == '"' && quoted && text.getOrNull(index + 1) == '"' -> {
                    field.append('"')
                    index++
                }
                character == '"' -> quoted = !quoted
                character == ';' && !quoted -> {
                    row.add(field.toString())
                    field.clear()
                }
                (character == '\n' || character == '\r') && !quoted -> {
                    if (character == '\r' && text.getOrNull(index + 1) == '\n') index++
                    row.add(field.toString())
                    field.clear()
                    if (row.any(String::isNotEmpty)) rows.add(row)
                    row = mutableListOf()
                }
                else -> field.append(character)
            }
            index++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            if (row.any(String::isNotEmpty)) rows.add(row)
        }
        return rows
    }

    private fun String.addSpreadsheetGuard(): String =
        if (firstOrNull() in setOf('=', '+', '-', '@')) "'$this" else this

    private fun String.removeSpreadsheetGuard(): String =
        if (length >= 2 && first() == '\'' && this[1] in setOf('=', '+', '-', '@')) drop(1) else this
}
