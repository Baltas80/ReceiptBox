package com.pagrey.receiptbox.util

import com.pagrey.receiptbox.data.Receipt
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ReceiptFullBackupTest {
    @Test
    fun roundTripsReceiptAndImage() {
        val temp = Files.createTempDirectory("receiptbox-full-backup").toFile()
        try {
            val sourceImage = byteArrayOf(1, 2, 3, 4)
            val source = Receipt(
                merchant = "Café Centro",
                date = "11/09/2026",
                total = 12.34,
                tax = 2.14,
                receiptNumber = "A-42",
                category = "Alimentación",
                imagePath = "/old/device/receipt.jpg",
                rawText = "TOTAL 12,34",
                createdAt = 123L
            )
            val zip = ReceiptFullBackup.export(listOf(source)) { sourceImage }
            val result = ReceiptFullBackup.import(zip, temp)
            val restored = result.receipts.single()

            assertEquals(source.merchant, restored.merchant)
            assertEquals(source.total, restored.total)
            assertEquals(source.tax, restored.tax)
            assertEquals(source.rawText, restored.rawText)
            assertEquals(1, result.restoredImages)
            assertEquals(0, result.missingImages)
            assertTrue(restored.imagePath.isNotBlank())
            assertArrayEquals(sourceImage, File(restored.imagePath).readBytes())
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun missingSourceImageRemainsWithoutImage() {
        val temp = Files.createTempDirectory("receiptbox-full-backup-missing").toFile()
        try {
            val zip = ReceiptFullBackup.export(listOf(Receipt(imagePath = "/missing.jpg"))) { null }
            val result = ReceiptFullBackup.import(zip, temp)
            assertEquals(1, result.receipts.size)
            assertEquals(0, result.restoredImages)
            assertEquals(0, result.missingImages)
            assertTrue(result.receipts.single().imagePath.isEmpty())
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun reportsMissingImageEntry() {
        val temp = Files.createTempDirectory("receiptbox-full-backup-missing-entry").toFile()
        try {
            val zip = zipOf(
                "backup.json" to """{"version":1,"receipts":[{"merchant":"Test","imageEntry":"images/0.jpg"}]}""".toByteArray()
            )
            val result = ReceiptFullBackup.import(zip, temp)
            assertEquals(0, result.restoredImages)
            assertEquals(1, result.missingImages)
            assertTrue(result.receipts.single().imagePath.isEmpty())
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun cleanupImagesRemovesOnlyRestoredFiles() {
        val temp = Files.createTempDirectory("receiptbox-full-backup-cleanup").toFile()
        try {
            val zip = ReceiptFullBackup.export(listOf(Receipt(imagePath = "/source.jpg"))) { byteArrayOf(8, 9) }
            val result = ReceiptFullBackup.import(zip, temp)
            val restoredPath = result.receipts.single().imagePath
            assertTrue(File(restoredPath).isFile)

            result.cleanupImages()

            assertFalse(File(restoredPath).exists())
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun rejectsPathTraversalImageEntry() {
        val unsafeName = "images/" + ".." + "/escape.jpg"
        val zip = zipOf(
            "backup.json" to """{"version":1,"receipts":[{"imageEntry":"$unsafeName"}]}""".toByteArray(),
            unsafeName to byteArrayOf(9)
        )

        runCatching { ReceiptFullBackup.import(zip, Files.createTempDirectory("receiptbox-full-backup-safe").toFile()) }
            .onSuccess { error("Unsafe ZIP entry should be rejected") }
            .onFailure { assertEquals("Entrada de copia no válida", it.message) }
    }

    @Test
    fun exportedImageEntryDoesNotContainOriginalPath() {
        val zip = ReceiptFullBackup.export(listOf(Receipt(imagePath = "/private/device/secret.jpg"))) { byteArrayOf(7) }
        val names = zip.toZipNames()
        assertTrue(names.contains("backup.json"))
        assertTrue(names.contains("images/0.jpg"))
        assertFalse(names.any { it.contains("secret.jpg") || it.contains("private") })
    }

    @Test
    fun rejectsOversizedZipEntry() {
        val oversized = ByteArray(10 * 1024 * 1024 + 1) { 1 }
        val zip = zipOf("backup.json" to oversized)
        runCatching { ReceiptFullBackup.import(zip, Files.createTempDirectory("receiptbox-full-backup-limit").toFile()) }
            .onSuccess { error("Oversized ZIP entry should be rejected") }
            .onFailure { assertEquals("Una entrada de la copia supera el tamaño máximo permitido", it.message) }
    }

    @Test
    fun rejectsOversizedExpandedBackup() {
        val image = ByteArray(9 * 1024 * 1024) { 0 }
        val zip = zipOf(
            "backup.json" to "{\"version\":1,\"receipts\":[]}".toByteArray(),
            "images/0.jpg" to image,
            "images/1.jpg" to image,
            "images/2.jpg" to image,
            "images/3.jpg" to image
        )
        runCatching { ReceiptFullBackup.import(zip, Files.createTempDirectory("receiptbox-full-backup-total-limit").toFile()) }
            .onSuccess { error("Expanded ZIP should be rejected") }
            .onFailure { assertEquals("La copia descomprimida supera el tamaño máximo permitido", it.message) }
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().use { output ->
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        output.toByteArray()
    }

    private fun ByteArray.toZipNames(): List<String> {
        val names = mutableListOf<String>()
        java.util.zip.ZipInputStream(inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                names += entry.name
                zip.closeEntry()
            }
        }
        return names
    }
}