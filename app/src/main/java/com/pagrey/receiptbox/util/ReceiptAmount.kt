package com.pagrey.receiptbox.util

/** Parses common Spanish/European and US receipt amount formats. */
fun parseReceiptAmount(value: String): Double? {
    val cleaned = value.trim().replace(" ", "")
    if (cleaned.isEmpty()) return null

    val commas = cleaned.count { it == ',' }
    val dots = cleaned.count { it == '.' }

    return when {
        commas > 0 && dots > 0 -> {
            if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                cleaned.replace(".", "").replace(',', '.').toDoubleOrNull()
            } else {
                cleaned.replace(",", "").toDoubleOrNull()
            }
        }
        commas == 1 -> {
            val decimals = cleaned.substringAfter(',')
            if (decimals.length == 3 && cleaned.substringBefore(',').length <= 3) {
                cleaned.replace(",", "").toDoubleOrNull()
            } else {
                cleaned.replace(',', '.').toDoubleOrNull()
            }
        }
        dots == 1 -> {
            val decimals = cleaned.substringAfter('.')
            if (decimals.length == 3 && cleaned.substringBefore('.').length <= 3) {
                cleaned.replace(".", "").toDoubleOrNull()
            } else {
                cleaned.toDoubleOrNull()
            }
        }
        dots > 0 -> cleaned.replace(".", "").toDoubleOrNull()
        commas > 0 -> cleaned.replace(",", "").toDoubleOrNull()
        else -> cleaned.toDoubleOrNull()
    }
}
