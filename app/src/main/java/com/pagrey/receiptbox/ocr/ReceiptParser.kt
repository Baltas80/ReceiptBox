package com.pagrey.receiptbox.ocr

import java.text.Normalizer
import java.util.Locale

/** Lightweight, deterministic parser for common receipt text patterns. */
data class ParsedReceipt(
    val merchant: String = "",
    val date: String = "",
    val total: Double? = null,
    val tax: Double? = null,
    val receiptNumber: String = ""
)

object ReceiptParser {
    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val merchant = lines.firstOrNull()?.take(80).orEmpty()
        val date = Regex("\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\b")
            .find(rawText)?.value.orEmpty()
        val total = findAmount(rawText, listOf("total", "importe", "amount", "a pagar"))
        val tax = findAmount(rawText, listOf("iva", "vat", "tax"))
        val number = Regex("(?i)(?:ticket|receipt|factura|invoice|n[ºo.]?)\\s*[:#-]?\\s*([A-Z0-9-]{3,})")
            .find(rawText)?.groupValues?.getOrNull(1).orEmpty()
        return ParsedReceipt(merchant, date, total, tax, number)
    }

    private fun findAmount(text: String, labels: List<String>): Double? {
        val pattern = labels.joinToString("|") { Regex.escape(it) }
        val match = Regex("(?i)(?:$pattern)[^0-9]{0,20}([0-9]{1,6}(?:[.,][0-9]{1,2})?)").find(text)
        return match?.groupValues?.getOrNull(1)?.let { parseNumber(it) }
            ?: Regex("(?i)(?:$pattern).*?([0-9]{1,6}(?:[.,][0-9]{2}))").find(text)?.groupValues?.getOrNull(1)?.let(::parseNumber)
    }

    private fun parseNumber(value: String): Double? =
        value.replace(".", "").replace(',', '.').toDoubleOrNull()
}
