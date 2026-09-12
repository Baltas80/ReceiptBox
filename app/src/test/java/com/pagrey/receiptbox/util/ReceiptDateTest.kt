package com.pagrey.receiptbox.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReceiptDateTest {
    @Test
    fun parsesSupportedReceiptDateFormats() {
        assertEquals(LocalDate.of(2026, 9, 11), parseReceiptDate("11/09/2026"))
        assertEquals(LocalDate.of(2026, 9, 11), parseReceiptDate("11-09-2026"))
        assertEquals(LocalDate.of(2026, 9, 11), parseReceiptDate("2026-09-11"))
        assertEquals(LocalDate.of(2026, 9, 11), parseReceiptDate("2026/09/11"))
    }

    @Test
    fun rejectsInvalidReceiptDate() {
        assertNull(parseReceiptDate("31/02/2026"))
        assertNull(parseReceiptDate(""))
    }
}
