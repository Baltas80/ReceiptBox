package com.pagrey.receiptbox.util

import com.pagrey.receiptbox.data.Receipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptBackupTest {
    @Test
    fun roundTripsReceiptData() {
        val source = Receipt(
            id = 42L,
            merchant = "Café Centro",
            date = "11/09/2026",
            total = 1234.56,
            tax = 214.56,
            receiptNumber = "A-42",
            category = "Alimentación",
            imagePath = "/local/image.jpg",
            rawText = "TOTAL 1.234,56",
            createdAt = 123L
        )

        val restored = ReceiptBackup.import(ReceiptBackup.export(listOf(source))).single()
        assertEquals(source.merchant, restored.merchant)
        assertEquals(source.date, restored.date)
        assertEquals(source.total, restored.total)
        assertEquals(source.tax, restored.tax)
        assertEquals(source.receiptNumber, restored.receiptNumber)
        assertEquals(source.category, restored.category)
        assertEquals(source.imagePath, restored.imagePath)
        assertEquals(source.rawText, restored.rawText)
        assertEquals(source.createdAt, restored.createdAt)
        assertEquals(0L, restored.id)
    }

    @Test
    fun preservesMissingAmounts() {
        val restored = ReceiptBackup.import(ReceiptBackup.export(listOf(Receipt(imagePath = "")))).single()
        assertNull(restored.total)
        assertNull(restored.tax)
        assertTrue(restored.merchant.isEmpty())
    }
}
