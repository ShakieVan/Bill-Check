package de.shakie.billcheck.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvTransferCodec {
    private val columns = listOf(
        "recordType", "tripId", "tripName", "tripCurrency", "tripRate", "exchangeRateMode",
        "defaultTipMinor", "defaultTipCurrency", "createdAt", "receiptId", "occurredAt", "date",
        "location", "checkNumber", "amountMinor", "currency", "exactEuroCents", "tipMinor", "tipCurrency",
        "reconciliationId", "reconciliationTitle", "lineId", "description", "status", "accepted",
        "matchedReceiptId", "matchedManually",
    )

    fun encode(value: TransferPackage): String = buildString {
        append('\uFEFF')
        appendRow(listOf("Bill Check", "CSV", "1"))
        appendRow(columns)
        value.trips.forEach { trip ->
            appendRecord("TRIP", trip = trip)
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
        require(rows.firstOrNull()?.take(3) == listOf("Bill Check", "CSV", "1")) {
            "Unsupported Bill Check CSV"
        }
        val header = rows.getOrNull(1).orEmpty()
        val indices = header.withIndex().associate { it.value to it.index }
        fun List<String>.value(name: String): String = indices[name]?.let { getOrNull(it) }.orEmpty()

        val records = rows.drop(2).filter { it.isNotEmpty() }
        val trips = records.filter { it.value("recordType") == "TRIP" }.map { tripRow ->
            val tripId = tripRow.value("tripId")
            val receiptRows = records.filter {
                it.value("recordType") == "RECEIPT" && it.value("tripId") == tripId
            }
            val reconciliationRows = records.filter {
                it.value("recordType") == "RECONCILIATION" && it.value("tripId") == tripId
            }
            TransferTrip(
                id = tripId,
                name = tripRow.value("tripName"),
                foreignCurrencyCode = tripRow.value("tripCurrency"),
                defaultExchangeRate = tripRow.value("tripRate"),
                exchangeRateMode = tripRow.value("exchangeRateMode").ifBlank { "FIXED" },
                defaultTipMinor = tripRow.value("defaultTipMinor").toLongOrNull() ?: 100,
                defaultTipCurrencyCode = tripRow.value("defaultTipCurrency").ifBlank { "EUR" },
                defaultTipSelected = false,
                imageStorageMode = "ORIGINAL",
                createdAt = tripRow.value("createdAt").toLongOrNull() ?: System.currentTimeMillis(),
                receipts = receiptRows.map { row ->
                    TransferReceipt(
                        id = row.value("receiptId"),
                        occurredAt = row.value("occurredAt").toLongOrNull() ?: 0,
                        location = row.value("location"),
                        checkNumber = row.value("checkNumber"),
                        foreignAmountMinor = row.value("amountMinor").toLongOrNull() ?: 0,
                        foreignCurrencyCode = row.value("currency"),
                        exchangeRate = row.value("tripRate"),
                        exactEuroCents = row.value("exactEuroCents").toLongOrNull() ?: 0,
                        tipMinor = row.value("tipMinor").toLongOrNull() ?: 0,
                        tipCurrencyCode = row.value("tipCurrency").ifBlank {
                            row.value("defaultTipCurrency").ifBlank { "EUR" }
                        },
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
        receipt: TransferReceipt? = null,
        reconciliation: TransferReconciliation? = null,
        line: TransferStatementLine? = null,
    ) {
        val values = mapOf(
            "recordType" to type,
            "tripId" to trip.id,
            "tripName" to trip.name,
            "tripCurrency" to trip.foreignCurrencyCode,
            "tripRate" to (receipt?.exchangeRate ?: trip.defaultExchangeRate),
            "exchangeRateMode" to trip.exchangeRateMode,
            "defaultTipMinor" to trip.defaultTipMinor.toString(),
            "defaultTipCurrency" to trip.defaultTipCurrencyCode,
            "createdAt" to (receipt?.createdAt ?: reconciliation?.createdAt ?: trip.createdAt).toString(),
            "receiptId" to receipt?.id.orEmpty(),
            "occurredAt" to (receipt?.occurredAt ?: line?.occurredOn)?.toString().orEmpty(),
            "date" to formatDate(receipt?.occurredAt ?: line?.occurredOn),
            "location" to receipt?.location.orEmpty(),
            "checkNumber" to (receipt?.checkNumber ?: line?.checkNumber).orEmpty(),
            "amountMinor" to (receipt?.foreignAmountMinor ?: line?.amountMinor)?.toString().orEmpty(),
            "currency" to (receipt?.foreignCurrencyCode ?: line?.currencyCode).orEmpty(),
            "exactEuroCents" to receipt?.exactEuroCents?.toString().orEmpty(),
            "tipMinor" to receipt?.tipMinor?.toString().orEmpty(),
            "tipCurrency" to receipt?.tipCurrencyCode.orEmpty(),
            "reconciliationId" to reconciliation?.id.orEmpty(),
            "reconciliationTitle" to reconciliation?.title.orEmpty(),
            "lineId" to line?.id.orEmpty(),
            "description" to line?.description.orEmpty(),
            "status" to line?.status.orEmpty(),
            "accepted" to line?.acceptedWithoutReceipt?.toString().orEmpty(),
            "matchedReceiptId" to line?.matchedReceiptId.orEmpty(),
            "matchedManually" to line?.matchedManually?.toString().orEmpty(),
        )
        appendRow(columns.map { values[it].orEmpty() })
    }

    private fun StringBuilder.appendRow(values: List<String>) {
        append(values.joinToString(";") { value -> "\"${value.replace("\"", "\"\"")}\"" })
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
}
