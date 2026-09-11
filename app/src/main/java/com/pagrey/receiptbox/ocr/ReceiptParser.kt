package com.pagrey.receiptbox.ocr

import com.pagrey.receiptbox.util.parseReceiptAmount

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
        val merchant = findMerchant(lines)
        val date = DATE_REGEX.find(rawText)?.value.orEmpty()
        val total = findAmount(rawText, listOf("total a pagar", "total", "importe", "amount", "a pagar"))
        val tax = findAmount(rawText, listOf("iva", "vat", "tax"))
        val number = NUMBER_REGEX.find(rawText)?.groupValues?.getOrNull(1).orEmpty()
        return ParsedReceipt(merchant, date, total, tax, number)
    }

    private fun findMerchant(lines: List<String>): String {
        // A receipt website is often more reliable than the first OCR line.
        lines.firstNotNullOfOrNull { line ->
            WEBSITE_REGEX.find(line)?.groupValues?.getOrNull(1)?.let { domain ->
                domain.substringBefore('.').removePrefix("www").trim().takeIf { it.length >= 2 }
                    ?.replaceFirstChar { it.uppercase() }
            }
        }?.let { return it }

        val excluded = setOf(
            "total", "total a pagar", "importe", "iva", "vat", "tax", "factura", "ticket",
            "recibo", "fecha", "hora", "cliente", "comercio", "gracias por su visita",
            "ejemplar para el cliente", "forma de pago", "pago", "venta", "autorizacion"
        )

        return lines.asSequence()
            .filter { it.length in 3..80 }
            .filter { line -> line.any { it.isLetter() } }
            .filter { line -> line.count { it.isDigit() } <= 1 }
            .filterNot { it.contains('%') }
            .filterNot { DATE_REGEX.containsMatchIn(it) }
            .filterNot { line -> excluded.any { key -> line.equals(key, ignoreCase = true) || line.startsWith("$key:", true) } }
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .firstOrNull()
            .orEmpty()
    }

    private fun findAmount(text: String, labels: List<String>): Double? {
        val labelPattern = labels.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
        val labelLineRegex = Regex("(?im)^.*(?:$labelPattern).*$")
        val matches = labelLineRegex.findAll(text).toList()

        // Prefer an explicit labelled line. This avoids taking an unrelated number
        // from the following receipt text (a common source of false 0.0/other totals).
        for (match in matches) {
            val line = match.value
            val amounts = AMOUNT_REGEX.findAll(line).mapNotNull { parseNumber(it.value) }.toList()
            if (amounts.isNotEmpty()) return amounts.last()
        }
        return null
    }

    /** Kept public for compatibility; amount parsing is centralized in the shared utility. */
    fun parseNumber(value: String): Double? = parseReceiptAmount(value)

    private val DATE_REGEX = Regex("\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\b")
    private val NUMBER_REGEX = Regex("(?i)(?:ticket|receipt|factura|invoice|n[ºo.]?)\\s*[:#-]?\\s*([A-Z0-9-]{3,})")
    private val WEBSITE_REGEX = Regex("(?i)(?:https?://)?(?:www\\.)?([a-z0-9][a-z0-9-]{1,30}\\.[a-z]{2,})(?:/[^\\s]*)?")
    private val AMOUNT_REGEX = Regex("(?<!\\d)\\d{1,7}(?:[.,]\\d{1,3})?(?:[.,]\\d{3})?(?!\\d)")
}
