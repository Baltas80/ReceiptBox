package com.pagrey.receiptbox.ocr

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
        val date = DATE_REGEX.find(rawText)?.value.orEmpty()
        val total = findAmount(rawText, listOf("total a pagar", "total", "importe", "amount", "a pagar"))
        val tax = findAmount(rawText, listOf("iva", "vat", "tax"))
        val number = NUMBER_REGEX.find(rawText)?.groupValues?.getOrNull(1).orEmpty()
        return ParsedReceipt(merchant, date, total, tax, number)
    }

    private fun findAmount(text: String, labels: List<String>): Double? {
        val pattern = labels.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
        val match = Regex("(?i)(?:$pattern)\\s*[:#-]?\\s*[^0-9]{0,20}([0-9]{1,7}(?:[.,][0-9]{1,2})?)").find(text)
        return match?.groupValues?.getOrNull(1)?.let(::parseNumber)
    }

    private fun parseNumber(value: String): Double? {
        val cleaned = value.trim()
        val commas = cleaned.count { it == ',' }
        val dots = cleaned.count { it == '.' }
        return when {
            commas > 0 && dots > 0 -> if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                cleaned.replace(".", "").replace(',', '.').toDoubleOrNull()
            } else {
                cleaned.replace(",", "").toDoubleOrNull()
            }
            commas == 1 -> cleaned.replace(',', '.').toDoubleOrNull()
            dots == 1 && cleaned.substringAfter('.').length <= 2 -> cleaned.toDoubleOrNull()
            dots > 0 -> cleaned.replace(".", "").toDoubleOrNull()
            else -> cleaned.toDoubleOrNull()
        }
    }

    private val DATE_REGEX = Regex("\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\b")
    private val NUMBER_REGEX = Regex("(?i)(?:ticket|receipt|factura|invoice|n[ºo.]?)\\s*[:#-]?\\s*([A-Z0-9-]{3,})")
}
