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
        val total = findTotal(lines)
        val tax = findTax(lines)
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
            .filter { it.any { c -> c.isLetter() } }
            .filter { !it.first().isDigit() }
            .filter { line -> line.count { it.isDigit() } <= 1 }
            .filter { line ->
                val alnum = line.count { it.isLetterOrDigit() }
                alnum == 0 || line.count { it.isDigit() }.toDouble() / alnum < 0.25
            }
            .filterNot { it.contains('%') }
            .filterNot { DATE_REGEX.containsMatchIn(it) }
            .filterNot { line -> excluded.any { key -> line.equals(key, true) || line.startsWith("$key:", true) } }
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .firstOrNull()
            .orEmpty()
    }

    private fun findTotal(lines: List<String>): Double? {
        val labels = listOf("TOTAL A PAGAR", "TOTAL", "IMPORTE TOTAL", "A PAGAR", "PAGAR")
            .map(::normalizeLabel)

        // Highest-confidence case: the amount is on the same OCR line as TOTAL.
        val sameLine = lines.flatMap { line ->
            val normalized = normalizeLabel(line)
            if (normalized.contains("SUBTOTAL") || normalized.contains("DESCUENTO")) emptyList()
            else if (labels.any { normalized.contains(it) }) {
                AMOUNT_REGEX.findAll(line).mapNotNull { parseCandidate(it.value) }.toList()
            } else emptyList()
        }
        if (sameLine.isNotEmpty()) return sameLine.last()

        // OCR can split TOTAL and its amount. Search forward, but stop at payment/tax
        // sections so cash/change values cannot become the receipt total.
        lines.forEachIndexed { index, line ->
            val normalized = normalizeLabel(line)
            if (normalized.contains("SUBTOTAL") || normalized.contains("DESCUENTO")) return@forEachIndexed
            if (labels.any { normalized.contains(it) }) {
                val candidates = mutableListOf<Double>()
                for (offset in 1..8) {
                    val next = lines.getOrNull(index + offset) ?: break
                    val nextNormalized = normalizeLabel(next)
                    if (PAYMENT_STOP_LABELS.any { nextNormalized.contains(it) } || nextNormalized.contains("IMPUESTOS")) break
                    val amounts = AMOUNT_REGEX.findAll(next).mapNotNull { parseCandidate(it.value) }.toList()
                    if (amounts.isNotEmpty()) candidates += amounts.last()
                }
                if (candidates.isNotEmpty()) return candidates.maxOrNull()
            }
        }
        return null
    }

    private fun findTax(lines: List<String>): Double? {
        val taxLabels = listOf("IVA", "VAT", "TAX").map(::normalizeLabel)

        // Explicit IVA/VAT/TAX amount.
        val explicit = lines.flatMap { line ->
            val normalized = normalizeLabel(line)
            if (taxLabels.any { normalized.contains(it) }) {
                AMOUNT_REGEX.findAll(line).mapNotNull { parseCandidate(it.value) }.toList()
            } else emptyList()
        }
        if (explicit.isNotEmpty()) return explicit.last()

        // Spanish receipts may call the tax section IMPUESTOS and present rows as
        // rate / taxable base / quota. Sum the final amount of each percentage row.
        val start = lines.indexOfFirst { normalizeLabel(it).contains("IMPUESTOS") }
        if (start >= 0) {
            var sum = 0.0
            var rows = 0
            for (index in start + 1 until lines.size) {
                val normalized = normalizeLabel(lines[index])
                if (normalized.contains("IMPUESTOSINCLUIDOS") || normalized.contains("GRACIAS")) break
                if (!lines[index].contains('%')) continue
                val amounts = AMOUNT_REGEX.findAll(lines[index]).mapNotNull { parseCandidate(it.value) }.toList()
                if (amounts.size >= 2) {
                    sum += amounts.last()
                    rows++
                }
            }
            if (rows > 0) return sum
        }
        return null
    }

    private fun parseCandidate(value: String): Double? =
        parseReceiptAmount(value)?.takeIf { it in 0.01..100_000.0 }

    private fun normalizeLabel(value: String): String =
        value.uppercase().filter { it.isLetter() }

    fun parseNumber(value: String): Double? = parseReceiptAmount(value)

    private val DATE_REGEX = Regex("\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\b")
    private val WEBSITE_REGEX = Regex("(?i)(?:https?://)?(?:www\\.)?([a-z0-9][a-z0-9-]{1,30}\\.[a-z]{2,})(?:/[^\\s]*)?")
    private val AMOUNT_REGEX = Regex("(?<!\\d)\\d{1,7}(?:(?:[.,]\\d{3})*[.,]\\d{1,2})?(?!\\d)")
    private val PAYMENT_STOP_LABELS = listOf("EFECTIVO", "CAMBIO", "TARJETA", "CARD", "CREDITO", "CRÉDITO", "DEBITO", "DÉBITO", "PAGO")
}
