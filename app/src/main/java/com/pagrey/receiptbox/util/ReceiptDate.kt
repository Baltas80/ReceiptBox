package com.pagrey.receiptbox.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

private val receiptDateFormatters = listOf(
    DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
    DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT),
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("uuuu/MM/dd").withResolverStyle(ResolverStyle.STRICT)
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
