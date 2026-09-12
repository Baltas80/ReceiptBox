package com.pagrey.receiptbox.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val receiptDateFormatters = listOf(
    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("yyyy/MM/dd")
)

fun parseReceiptDate(value: String): LocalDate? {
    val clean = value.trim()
    if (clean.isEmpty()) return null
    return receiptDateFormatters.firstNotNullOfOrNull { formatter ->
        try {
            LocalDate.parse(clean, formatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
