package com.pagrey.receiptbox.util

import com.pagrey.receiptbox.data.Receipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptCsvExporterTest {
    @Test
    fun exportsHeaderAndEuropeanAmounts() {
        val csv = ReceiptCsvExporter.export(listOf(
            Receipt(
                id = 7L,
                merchant = "Café \"Centro\"",
                date = "11/09/2026",
                total = 1234.56,
                tax = 214.56,
                receiptNumber = "A-42",
                category = "Alimentación",
                imagePath = null,
                rawText = "texto",
                createdAt = 1L
            )
        )

        assertTrue(csv.startsWith("ID;Comercio;Fecha;Total;IVA;Número de ticket;Categoría\n"))
        assertTrue(csv.contains("\"Café \"\"Centro\"\"\""))
        assertTrue(csv.contains("\"1234,56\""))
        assertTrue(csv.contains("\"214,56\""))
        assertTrue(csv.contains("\"A-42\""))
    }

    @Test
    fun exportsEmptyOptionalAmountsAsEmptyFields() {
        val csv = ReceiptCsvExporter.export(listOf(
            Receipt(
                merchant = "Tienda",
                date = "11/09/2026",
                total = null,
                tax = null,
                receiptNumber = "",
                category = "Otros",
                imagePath = null,
                rawText = "",
                createdAt = 1L
            )
        )

        assertEquals(2, csv.lines().size)
        assertTrue(csv.lines()[1].contains("\"Tienda\""))
        assertTrue(csv.lines()[1].contains(";\"\";\"\";\"\";\"Otros\""))
    }
}
