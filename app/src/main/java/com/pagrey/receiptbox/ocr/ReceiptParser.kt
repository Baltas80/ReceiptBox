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
        val total = findAmount(rawText, listOf("total a pagar", "total", "importe total", "importe", "amount due", "amount", "a pagar", "pagar"))
        val tax = findAmount(rawText, listOf("iva", "vat", "tax"))
        val number = NUMBER_REGEX.find(rawText)?.groupValues?.getOrNull(1).orEmpty()
        return ParsedReceipt(merchant, date, total, tax, number)
    }

    private fun findMerchant(lines: List<String>): String {
        lines.firstNotNullOfOrNull { line ->
            WEBSITE_REGEX.find(line)?.groupValues?.getOrNull(1)?.let { domain ->
                domain.substringBefore('.').removePrefix("www").trim().takeIf { it.length >= 2 }
                    ?.replaceFirstChar { it.uppercase() }
            }
        }?.let { return it }

        val excluded = setOf(
            "total", "total a pagar", "importe", "iva", "vat", "tax", "factura", "ticket",
            "recibo", "fecha", "hora", "cliente", "comercio", "gracias por su visita",
            "ejemplar para el cliente", "forma de pago", "pago", "venta", "autorizacion",
            "autorización", "domicilio", "telefono", "teléfono", "www"
        )

        return lines.asSequence()
            .filter { it.length in 3..80 }
            .filter { line -> line.any { it.isLetter() } }
            .filter { line -> !line.first().isDigit() }
            .filter { line -> line.count { it.isDigit() } <= 1 }
            .filter { line -> line.count { it.isLetterOrDigit() }.let { alnum -> alnum == 0 || line.count { it.isDigit() }.toDouble() / alnum < 0.25 } }
            .filterNot { it.contains('%') }
            .filterNot { DATE_REGEX.containsMatchIn(it) }
            .filterNot { line -> excluded.any { key -> line.equals(key, ignoreCase = true) || line.startsWith("$key:", true) } }
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .firstOrNull()
            .orEmpty()
    }

    private fun findAmount(text: String, labels: List<String>): Double? {
        val labelPattern = labels.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
        val labelLineRegex = Regex("(?im)^.*(?<![\\p{L}\\p{N}])(?:$labelPattern)(?![\\p{L}\\p{N}]).*$")
        val matches = labelLineRegex.findAll(text).toList()

        for (match in matches) {
            val line = match.value
            val amounts = AMOUNT_REGEX.findAll(line).mapNotNull { parseNumber(it.value) }.toList()
            if (amounts.isNotEmpty()) return amounts.last()
        }

        // OCR frequently inserts spaces or punctuation inside labels (e.g. T O T A L).
        // Normalize each line and retry without weakening SUBTOTAL protection.
        val normalizedLabels = labels.map { normalizeLabel(it) }
        for (line in text.lines()) {
            val normalized = normalizeLabel(line)
            if (normalizedLabels.any { normalized.contains(it) } && !normalized.contains("SUBTOTAL")) {
                val amounts = AMOUNT_REGEX.findAll(line).mapNotNull { parseNumber(it.value) }.toList()
                if (amounts.isNotEmpty()) return amounts.last()
            }
        }
        return null
    }

    private fun normalizeLabel(value: String): String =
        value.uppercase().filter { it.isLetter() }

    /** Kept public for compatibility; amount parsing is centralized in the shared utility. */
    fun parseNumber(value: String): Double? = parseReceiptAmount(value)

    private val DATE_REGEX = Regex("\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\b")
    private val NUMBER_REGEX = Regex("(?i)(?:ticket|receipt|factura|invoice|n[ºo.]?)\\s*[:#-]?\\s*([A-Z0-9-]{3,})")
    private val WEBSITE_REGEX = Regex("(?i)(?:https?://)?(?:www\\.)?([a-z0-9][a-z0-9-]{1,30}\\.[a-z]{2,})(?:/[^\\s]*)?")
    private val AMOUNT_REGEX = Regex("(?<!\\d)\\d{1,7}(?:(?:[.,]\\d{3})*[.,]\\d{1,2})?(?!\\d)")
}
