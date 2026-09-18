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
        return ParsedReceipt(
            merchant = findMerchant(lines),
            date = DATE_REGEX.find(rawText)?.value.orEmpty(),
            total = findTotal(lines),
            tax = findTax(lines),
            receiptNumber = ""
        )
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
            "autorización", "domicilio", "telefono", "teléfono", "www", "articulo", "artículo",
            "impuestos", "base", "cuota", "impuestos base cuota", "impuestos incluidos"
        )

        return lines.asSequence()
            .filter { it.length in 3..80 }
            .filter { it.any(Char::isLetter) }
            .filter { !it.first().isDigit() }
            .filter { line ->
                val alnum = line.count(Char::isLetterOrDigit)
                alnum == 0 || line.count(Char::isDigit).toDouble() / alnum < 0.25
            }
            .filterNot { it.contains('%') }
            .filterNot { DATE_REGEX.containsMatchIn(it) }
            .filterNot { line ->
                val normalized = normalizeLabel(line)
                excluded.any { key ->
                    val normalizedKey = normalizeLabel(key)
                    normalized == normalizedKey || normalized.startsWith(normalizedKey) ||
                        normalized.contains("IMPUESTOS") || normalized.contains("ARTICULO") ||
                        normalized.contains("BASECUOTA")
                }
            }
            .map { normalizeMerchant(it.replace(Regex("\\s+"), " ").trim()) }
            .firstOrNull()
            .orEmpty()
    }

    private fun normalizeMerchant(value: String): String {
        // Targeted correction for a common OCR loss observed in supermarket tickets:
        // "SUPER" is frequently reduced to "SOR" before a known ALCOOP name.
        return value.replace(Regex("(?i)^SOR(\\s+ALCOOP\\b)"), "SUPER$1")
    }

    private fun findTotal(lines: List<String>): Double? {
        val labels = listOf("TOTAL A PAGAR", "IMPORTE TOTAL", "TOTAL", "A PAGAR", "PAGAR")
            .map(::normalizeLabel)

        lines.forEachIndexed { index, line ->
            val normalized = normalizeLabel(line)
            if (normalized.contains("SUBTOTAL") || normalized.contains("DESCUENTO")) return@forEachIndexed
            val label = labels.firstOrNull { normalized.contains(it) } ?: return@forEachIndexed

            // If OCR has put TOTAL, EFECTIVO and CAMBIO on one visual line, only
            // consider amounts before the first payment marker. Never select cash tender.
            val paymentIndex = PAYMENT_STOP_LABELS
                .map { normalized.indexOf(it) }
                .filter { it >= 0 }
                .minOrNull()
            val relevant = if (paymentIndex != null && paymentIndex > normalized.indexOf(label)) {
                line.substring(0, paymentIndex)
            } else line
            val sameLineAmounts = AMOUNT_REGEX.findAll(relevant)
                .mapNotNull { parseCandidate(it.value) }
                .toList()
            if (sameLineAmounts.isNotEmpty()) return sameLineAmounts.last()

            // TOTAL and its amount may be split into adjacent OCR lines.
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
        return null
    }

    private fun findTax(lines: List<String>): Double? {
        val taxLabels = listOf("IVA", "VAT", "TAX").map(::normalizeLabel)
        val explicit = lines.flatMap { line ->
            val normalized = normalizeLabel(line)
            if (taxLabels.any { normalized.contains(it) }) {
                AMOUNT_REGEX.findAll(line).mapNotNull { parseCandidate(it.value) }.toList()
            } else emptyList()
        }
        if (explicit.isNotEmpty()) return explicit.last()

        val start = lines.indexOfFirst { normalizeLabel(it).contains("IMPUESTOS") }
        if (start >= 0) {
            var sum = 0.0
            var rows = 0
            var pendingRate = false
            for (index in start + 1 until lines.size) {
                val line = lines[index]
                val normalized = normalizeLabel(line)
                if (normalized.contains("IMPUESTOSINCLUIDOS") || normalized.contains("GRACIAS")) break

                if (line.contains('%')) {
                    val amounts = AMOUNT_REGEX.findAll(line).mapNotNull { parseCandidate(it.value) }.toList()
                    if (amounts.size >= 2) {
                        sum += amounts.last()
                        rows++
                        pendingRate = false
                    } else {
                        pendingRate = true
                    }
                    continue
                }

                // Camera OCR often separates the rate ("10,00 %") from the
                // base/cuota line ("9,81 0,98"). Pair the next numeric line with it.
                if (pendingRate) {
                    val amounts = AMOUNT_REGEX.findAll(line).mapNotNull { parseCandidate(it.value) }.toList()
                    if (amounts.size >= 2) {
                        sum += amounts.last()
                        rows++
                        pendingRate = false
                    }
                }
            }
            if (rows > 0) return sum
        }
        return null
    }

    private fun parseCandidate(value: String): Double? =
        parseReceiptAmount(value)?.takeIf { it in 0.01..100_000.0 }

    private fun normalizeLabel(value: String): String = value.uppercase().filter(Char::isLetter)

    fun parseNumber(value: String): Double? = parseReceiptAmount(value)

    private val DATE_REGEX = Regex("\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\b")
    private val WEBSITE_REGEX = Regex("(?i)(?:https?://)?(?:www\\.)?([a-z0-9][a-z0-9-]{1,30}\\.[a-z]{2,})(?:/[^\\s]*)?")
    private val AMOUNT_REGEX = Regex("(?<!\\d)\\d{1,7}(?:(?:[.,]\\d{3})*[.,]\\d{1,2})?(?!\\d)")
    private val PAYMENT_STOP_LABELS = listOf("EFECTIVO", "CAMBIO", "TARJETA", "CARD", "CREDITO", "CRÉDITO", "DEBITO", "DÉBITO", "PAGO")
}
