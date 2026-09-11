package com.pagrey.receiptbox.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptParserTest {
    @Test
    fun parsesTypicalSpanishReceipt() {
        val raw = """
            SUPERMERCADO EJEMPLO
            11/09/2026
            Factura: ABC123
            IVA: 2,50
            Total: 12,50
        """.trimIndent()

        val result = ReceiptParser.parse(raw)

        assertEquals("SUPERMERCADO EJEMPLO", result.merchant)
        assertEquals("11/09/2026", result.date)
        assertEquals(12.50, result.total!!, 0.001)
        assertEquals(2.50, result.tax!!, 0.001)
        assertEquals("ABC123", result.receiptNumber)
    }
}
