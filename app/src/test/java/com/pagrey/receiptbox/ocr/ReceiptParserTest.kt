package com.pagrey.receiptbox.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptParserTest {
    @Test fun parsesSpanishDecimal() {
        assertEquals(12.99, ReceiptParser.parseNumber("12,99")!!, 0.001)
    }

    @Test fun parsesDotDecimal() {
        assertEquals(12.99, ReceiptParser.parseNumber("12.99")!!, 0.001)
    }

    @Test fun parsesSpanishThousands() {
        assertEquals(1234.56, ReceiptParser.parseNumber("1.234,56")!!, 0.001)
    }

    @Test fun parsesEnglishThousands() {
        assertEquals(1234.56, ReceiptParser.parseNumber("1,234.56")!!, 0.001)
    }

    @Test fun parsesTotalLabel() {
        val result = ReceiptParser.parse("SUPERMERCADO\\nTOTAL A PAGAR: 24,90")
        assertEquals(24.90, result.total!!, 0.001)
    }
}
