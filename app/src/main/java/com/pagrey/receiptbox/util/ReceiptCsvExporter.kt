package com.pagrey.receiptbox.util

import com.pagrey.receiptbox.data.Receipt

object ReceiptCsvExporter {
    private const val HEADER = "ID;Comercio;Fecha;Total;IVA;Número de ticket;Categoría"

    fun export(receipts: List<Receipt>): String = buildString {
        appendLine(HEADER)
        receipts.forEach { receipt ->
            appendLine(listOf(
                receipt.id.toString(),
                receipt.merchant,
                receipt.date,
                receipt.total?.let(::formatAmount).orEmpty(),
                receipt.tax?.let(::formatAmount).orEmpty(),
                receipt.receiptNumber,
                receipt.category
            ).joinToString(";") { escape(it) })
        }
    }

    private fun formatAmount(value: Double): String =
        java.math.BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',')

    private fun escape(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
