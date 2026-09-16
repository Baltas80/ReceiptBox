package com.pagrey.receiptbox.ocr

import com.pagrey.receiptbox.util.parseReceiptAmount

data class ParsedReceipt(
    val merchant: String = "",
    val date: String = "",
    val total: Double? = null,
    val tax: Double? = null,
    // App-assigned sequence number. Printed receipt numbers are deliberately ignored.
    val receiptNumber: String = ""
)

object ReceiptParser {
    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val merchant = findMerchant(lines)
        val date = DATE_REGEX.find(rawText)?.value.orEmpty()
        val total = findAmount(rawText, listOf("total a pagar", "total", "importe total", "importe", "amount due", "amount", "a pagar", "pagar"), preferFinalTotal = true)
        val tax = findAmount(rawText, listOf("iva", "vat", "tax"))
        // Do not trust an OCR-extracted ticket number. ReceiptBox assigns its own
        // stable sequence number when the receipt is saved.
        return ParsedReceipt(merchant, date, total, tax, "")
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

    private fun findAmount(text: String, labels: List<String>, preferFinalTotal: Boolean = false): Double? {
        val lines = text.lines().map { it.trim() }
        val normalizedLabels = labels.map { normalizeLabel(it) }

        // First pass: explicit label and amount on the same line. For TOTAL, keep
        // scanning instead of returning the first match because receipts often contain
        // several totals/subtotals and the final total is the authoritative value.
        val sameLineCandidates = mutableListOf<Pair<Int, Double>>()
        lines.forEachIndexed { index, line ->
            val normalized = normalizeLabel(line)
            if (isExcludedAmountLine(normalized)) return@forEachIndexed
            if (normalizedLabels.any { normalized.contains(it) }) {
                val amounts = AMOUNT_REGEX.findAll(line).mapNotNull { parseCandidate(it.value) }.toList()
                if (amounts.isNotEmpty()) sameLineCandidates += index to amounts.last()
            }
        }
        if (sameLineCandidates.isNotEmpty()) {
            return if (preferFinalTotal) sameLineCandidates.last().second else sameLineCandidates.first().second
        }

        // Second pass: OCR frequently puts the label on one line and its amount on the
        // following line. Search a short window after an explicit label.
        val nearbyCandidates = mutableListOf<Pair<Int, Double>>()
        lines.forEachIndexed { index, line ->
            val normalized = normalizeLabel(line)
            if (isExcludedAmountLine(normalized)) return@forEachIndexed
            if (normalizedLabels.any { normalized.contains(it) }) {
                for (offset in 1..2) {
                    val next = lines.getOrNull(index + offset) ?: break
                    if (isExcludedAmountLine(normalizeLabel(next))) continue
                    val amounts = AMOUNT_REGEX.findAll(next).mapNotNull { parseCandidate(it.value) }.toList()
                    if (amounts.isNotEmpty()) {
                        nearbyCandidates += index to amounts.last()
                        break
                    }
                }
            }
        }
        if (nearbyCandidates.isNotEmpty()) {
            return if (preferFinalTotal) nearbyCandidates.last().second else nearbyCandidates.first().second
        }

        // Last fallback: tolerate OCR splitting/corrupting the label while still requiring
        // a plausible total label and never accepting subtotal/discount lines.
        for (index in lines.indices) {
            val normalized = normalizeLabel(lines[index])
            if (isExcludedAmountLine(normalized)) continue
            if (normalizedLabels.any { normalized.contains(it) }) {
                val amounts = AMOUNT_REGEX.findAll(lines[index]).mapNotNull { parseCandidate(it.value) }.toList()
                if (amounts.isNotEmpty()) return amounts.last()
            }
        }
        return null
    }

    private fun isExcludedAmountLine(normalized: String): Boolean =
        normalized.contains("SUBTOTAL") || normalized.contains("DESCUENTO") || normalized.contains("DISCOUNT")

    private fun parseCandidate(value: String): Double? =
        parseReceiptAmount(value)?.takeIf { it in 0.01..100_000.0 }

    private fun normalizeLabel(value: String): String =
        value.uppercase().filter { it.isLetter() }

    fun parseNumber(value: String): Double? = parseReceiptAmount(value)

    private val DATE_REGEX = Regex("\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\b")
    private val WEBSITE_REGEX = Regex("(?i)(?:https?://)?(?:www\\.)?([a-z0-9][a-z0-9-]{1,30}\\.[a-z]{2,})(?:/[^\\s]*)?")
    private val AMOUNT_REGEX = Regex("(?<!\\d)\\d{1,7}(?:(?:[.,]\\d{3})*[.,]\\d{1,2})?(?!\\d)")
}
