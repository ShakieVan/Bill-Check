package de.shakie.billcheck.data

import org.json.JSONArray
import org.json.JSONObject

object BackupJsonCodec {
    fun encode(value: TransferPackage): String = JSONObject().apply {
        put("format", "bill-check")
        put("version", value.formatVersion)
        put("exportedAt", value.exportedAt)
        put("trips", JSONArray().apply { value.trips.forEach { put(encodeTrip(it)) } })
    }.toString(2)

    fun decode(text: String): TransferPackage {
        val root = JSONObject(text)
        require(root.optString("format") == "bill-check") { "Unsupported backup format" }
        val version = root.getInt("version")
        require(version == 2) { "Unsupported backup version: $version" }
        return TransferPackage(
            formatVersion = version,
            exportedAt = root.getLong("exportedAt"),
            trips = root.getJSONArray("trips").objects(::decodeTrip),
        )
    }

    private fun encodeTrip(value: TransferTrip) = JSONObject().apply {
        put("id", value.id)
        put("name", value.name)
        put("homeCurrencyCode", value.homeCurrencyCode)
        put("defaultTipMinor", value.defaultTipMinor)
        put("defaultTipCurrencyCode", value.defaultTipCurrencyCode)
        put("defaultTipSelected", value.defaultTipSelected)
        put("imageStorageMode", value.imageStorageMode)
        put("createdAt", value.createdAt)
        put("currencies", JSONArray().apply { value.currencies.forEach { put(encodeTripCurrency(it)) } })
        put("receipts", JSONArray().apply { value.receipts.forEach { put(encodeReceipt(it)) } })
        put(
            "reconciliations",
            JSONArray().apply { value.reconciliations.forEach { put(encodeReconciliation(it)) } },
        )
    }

    private fun decodeTrip(value: JSONObject) = TransferTrip(
        id = value.getString("id"),
        name = value.getString("name"),
        homeCurrencyCode = value.getString("homeCurrencyCode"),
        defaultTipMinor = value.getLong("defaultTipMinor"),
        defaultTipCurrencyCode = value.getString("defaultTipCurrencyCode"),
        defaultTipSelected = value.optBoolean("defaultTipSelected"),
        imageStorageMode = value.optString("imageStorageMode", "ORIGINAL"),
        createdAt = value.getLong("createdAt"),
        currencies = value.getJSONArray("currencies").objects(::decodeTripCurrency),
        receipts = value.getJSONArray("receipts").objects(::decodeReceipt),
        reconciliations = value.getJSONArray("reconciliations").objects(::decodeReconciliation),
    )

    private fun encodeTripCurrency(value: TransferTripCurrency) = JSONObject().apply {
        put("currencyCode", value.currencyCode)
        put("homeToCurrencyRate", value.homeToCurrencyRate)
        put("exchangeRateMode", value.exchangeRateMode)
        put("isDefault", value.isDefault)
    }

    private fun decodeTripCurrency(value: JSONObject) = TransferTripCurrency(
        currencyCode = value.getString("currencyCode"),
        homeToCurrencyRate = value.getString("homeToCurrencyRate"),
        exchangeRateMode = value.getString("exchangeRateMode"),
        isDefault = value.getBoolean("isDefault"),
    )

    private fun encodeReceipt(value: TransferReceipt) = JSONObject().apply {
        put("id", value.id)
        put("occurredAt", value.occurredAt)
        put("location", value.location)
        put("checkNumber", value.checkNumber)
        put("amountMinor", value.amountMinor)
        put("currencyCode", value.currencyCode)
        put("exchangeRateSnapshot", value.exchangeRateSnapshot)
        put("exactHomeMinor", value.exactHomeMinor)
        put("tipMinor", value.tipMinor)
        put("tipCurrencyCode", value.tipCurrencyCode)
        put("tipExchangeRateSnapshot", value.tipExchangeRateSnapshot)
        putNullable("imageEntry", value.imageEntry)
        putNullable("imageMimeType", value.imageMimeType)
        put("reviewState", value.reviewState)
        put("createdAt", value.createdAt)
        put("items", JSONArray().apply { value.items.forEach { put(encodeItem(it)) } })
    }

    private fun decodeReceipt(value: JSONObject) = TransferReceipt(
        id = value.getString("id"),
        occurredAt = value.getLong("occurredAt"),
        location = value.getString("location"),
        checkNumber = value.getString("checkNumber"),
        amountMinor = value.getLong("amountMinor"),
        currencyCode = value.getString("currencyCode"),
        exchangeRateSnapshot = value.getString("exchangeRateSnapshot"),
        exactHomeMinor = value.getLong("exactHomeMinor"),
        tipMinor = value.getLong("tipMinor"),
        tipCurrencyCode = value.getString("tipCurrencyCode"),
        tipExchangeRateSnapshot = value.getString("tipExchangeRateSnapshot"),
        imageEntry = value.nullableString("imageEntry"),
        imageMimeType = value.nullableString("imageMimeType"),
        reviewState = value.optString("reviewState", "CONFIRMED"),
        createdAt = value.getLong("createdAt"),
        items = value.getJSONArray("items").objects(::decodeItem),
    )

    private fun encodeItem(value: TransferReceiptItem) = JSONObject().apply {
        put("id", value.id)
        put("sortPosition", value.sortPosition)
        put("name", value.name)
        put("amountMinor", value.amountMinor)
        put("currencyCode", value.currencyCode)
    }

    private fun decodeItem(value: JSONObject) = TransferReceiptItem(
        id = value.getString("id"),
        sortPosition = value.getInt("sortPosition"),
        name = value.getString("name"),
        amountMinor = value.getLong("amountMinor"),
        currencyCode = value.getString("currencyCode"),
    )

    private fun encodeReconciliation(value: TransferReconciliation) = JSONObject().apply {
        put("id", value.id)
        put("title", value.title)
        putNullable("statementImageEntry", value.statementImageEntry)
        putNullable("statementImageMimeType", value.statementImageMimeType)
        put("createdAt", value.createdAt)
        putNullable("analysisSummary", value.analysisSummary)
        if (value.analysisUpdatedAt == null) put("analysisUpdatedAt", JSONObject.NULL)
        else put("analysisUpdatedAt", value.analysisUpdatedAt)
        if (value.declaredTotalMinor == null) put("declaredTotalMinor", JSONObject.NULL)
        else put("declaredTotalMinor", value.declaredTotalMinor)
        putNullable("declaredTotalCurrencyCode", value.declaredTotalCurrencyCode)
        put("lines", JSONArray().apply { value.lines.forEach { put(encodeLine(it)) } })
    }

    private fun decodeReconciliation(value: JSONObject) = TransferReconciliation(
        id = value.getString("id"),
        title = value.getString("title"),
        statementImageEntry = value.nullableString("statementImageEntry"),
        statementImageMimeType = value.nullableString("statementImageMimeType"),
        createdAt = value.getLong("createdAt"),
        lines = value.getJSONArray("lines").objects(::decodeLine),
        analysisSummary = value.nullableString("analysisSummary"),
        analysisUpdatedAt = if (!value.has("analysisUpdatedAt") || value.isNull("analysisUpdatedAt")) {
            null
        } else {
            value.getLong("analysisUpdatedAt")
        },
        declaredTotalMinor = if (!value.has("declaredTotalMinor") || value.isNull("declaredTotalMinor")) null
            else value.getLong("declaredTotalMinor"),
        declaredTotalCurrencyCode = value.nullableString("declaredTotalCurrencyCode"),
    )

    private fun encodeLine(value: TransferStatementLine) = JSONObject().apply {
        put("id", value.id)
        if (value.occurredOn == null) put("occurredOn", JSONObject.NULL) else put("occurredOn", value.occurredOn)
        put("description", value.description)
        put("checkNumber", value.checkNumber)
        put("amountMinor", value.amountMinor)
        put("currencyCode", value.currencyCode)
        put("status", value.status)
        put("acceptedWithoutReceipt", value.acceptedWithoutReceipt)
        putNullable("matchedReceiptId", value.matchedReceiptId)
        put("matchedManually", value.matchedManually)
        putNullable("aiSuggestedReceiptId", value.aiSuggestedReceiptId)
        if (value.aiConfidence == null) put("aiConfidence", JSONObject.NULL) else put("aiConfidence", value.aiConfidence)
        putNullable("aiReason", value.aiReason)
        putNullable("sourceDateText", value.sourceDateText)
        put("dateAmbiguous", value.dateAmbiguous)
    }

    private fun decodeLine(value: JSONObject) = TransferStatementLine(
        id = value.getString("id"),
        occurredOn = if (value.isNull("occurredOn")) null else value.getLong("occurredOn"),
        description = value.getString("description"),
        checkNumber = value.getString("checkNumber"),
        amountMinor = value.getLong("amountMinor"),
        currencyCode = value.getString("currencyCode"),
        status = value.getString("status"),
        acceptedWithoutReceipt = value.optBoolean("acceptedWithoutReceipt"),
        matchedReceiptId = value.nullableString("matchedReceiptId"),
        matchedManually = value.optBoolean("matchedManually"),
        aiSuggestedReceiptId = value.nullableString("aiSuggestedReceiptId"),
        aiConfidence = if (!value.has("aiConfidence") || value.isNull("aiConfidence")) null
            else value.getInt("aiConfidence"),
        aiReason = value.nullableString("aiReason"),
        sourceDateText = value.nullableString("sourceDateText"),
        dateAmbiguous = value.optBoolean("dateAmbiguous"),
    )
}

private inline fun <T> JSONArray.objects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }

private fun JSONObject.putNullable(name: String, value: String?) {
    put(name, value ?: JSONObject.NULL)
}

private fun JSONObject.nullableString(name: String): String? =
    if (isNull(name)) null else getString(name)
