package com.pagrey.receiptbox.util

import com.pagrey.receiptbox.data.Receipt
import org.json.JSONArray
import org.json.JSONObject

object ReceiptBackup {
    private const val VERSION = 1

    fun export(receipts: List<Receipt>): String {
        val root = JSONObject().put("version", VERSION)
        val items = JSONArray()
        receipts.forEach { receipt ->
            items.put(JSONObject().apply {
                put("merchant", receipt.merchant)
                put("date", receipt.date)
                put("total", receipt.total ?: JSONObject.NULL)
                put("tax", receipt.tax ?: JSONObject.NULL)
                put("receiptNumber", receipt.receiptNumber)
                put("category", receipt.category)
                put("imagePath", receipt.imagePath)
                put("rawText", receipt.rawText)
                put("createdAt", receipt.createdAt)
            })
        }
        root.put("receipts", items)
        return root.toString(2)
    }

    fun import(json: String): List<Receipt> {
        val root = JSONObject(json)
        require(root.optInt("version", -1) == VERSION) { "Versión de copia no compatible" }
        val items = root.optJSONArray("receipts") ?: JSONArray()
        return buildList(items.length()) {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                add(Receipt(
                    merchant = item.optString("merchant"),
                    date = item.optString("date"),
                    total = item.optNullableDouble("total"),
                    tax = item.optNullableDouble("tax"),
                    receiptNumber = item.optString("receiptNumber"),
                    category = item.optString("category", "Otros"),
                    imagePath = item.optString("imagePath"),
                    rawText = item.optString("rawText"),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis())
                ))
            }
        }
    }

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (!has(name) || isNull(name)) null else optDouble(name).takeUnless { it.isNaN() }
}
