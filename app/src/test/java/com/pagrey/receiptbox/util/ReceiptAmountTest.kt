package com.pagrey.receiptbox.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptAmountTest {
    @Test fun parsesEuropeanDecimal() {
        assertEquals(12.50, parseReceiptAmount("12,50")!!, 0.001)
    }

    @Test fun parsesEuropeanThousands() {
        assertEquals(1234.56, parseReceiptAmount("1.234,56")!!, 0.001)
    }

    @Test fun parsesUsThousands() {
        assertEquals(1234.56, parseReceiptAmount("1,234.56")!!, 0.001)
    }

    @Test fun parsesPlainDecimal() {
        assertEquals(19.99, parseReceiptAmount("19.99")!!, 0.001)
    }

    @Test fun blankReturnsNull() {
        assertNull(parseReceiptAmount("   "))
    }
}
